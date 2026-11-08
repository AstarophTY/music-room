package com.musicroom.app.player

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.musicroom.app.common.UiText
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.player_locked_by_event
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.AlbumResponse
import com.musicroom.shared.dto.DeviceResponse
import com.musicroom.shared.dto.PlaybackStateResponse
import com.musicroom.shared.dto.PlaylistSummaryResponse
import com.musicroom.shared.dto.TrackResponse
import com.musicroom.shared.dto.TrackSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Remote for Spotify playback on the user's devices (Spotify Connect): the music plays in the Spotify app
 * (phone, computer, speaker) and this controls it. Commands update the state right away, then [sync]
 * confirms it with Spotify.
 *
 * Spotify cannot reorder or remove songs from its queue, so songs added in the app wait in [upNext], in the
 * order the user chooses. The first one is handed to Spotify shortly before the current track ends (or on
 * "next"); from then on it can no longer move ([sentKey]).
 *
 * Songs uploaded to Music Room are played by the app itself ([HostedPlayback]), Spotify waiting paused meanwhile
 * ([playingHosted]). Apple Music songs play through the Apple Music SDK on Android ([playingAppleMusic]), Spotify
 * waiting paused too. A list holding some of them is played by the app song after song ([appList]): each Spotify
 * song is started on its own, and the next one starts when it ends. An app-local song queued during a Spotify album
 * or playlist pauses it, which goes on afterwards ([resumeRemote]).
 */
@Stable
class PlayerController(
    private val api: MusicRoomApi,
    private val scope: CoroutineScope,
    environment: PlaybackEnvironment,
) {
    private val remote = environment.remote

    var state by mutableStateOf<PlaybackStateResponse?>(null)
        private set

    var error by mutableStateOf<UiText?>(null)
        private set

    /** The last error can be fixed by opening Spotify on a device. */
    var needsDevice by mutableStateOf(false)
        private set

    private val queue = PlayerQueue(scope)
    val upNext: List<QueueEntry> get() = queue.entries
    val sentKey: Long? get() = queue.sentKey

    /** Spotify's own queue (songs added elsewhere, then the rest of the album or playlist), from [followQueue]. */
    private var remoteQueue by mutableStateOf<List<TrackResponse>>(emptyList())

    /** The user's devices with Spotify open, from [loadDevices]; null until loaded. */
    var devices by mutableStateOf<List<DeviceResponse>?>(null)
        private set

    /** Who adds songs from this app; set by the screen once the user is known. */
    var me: Proposer? = null

    /** Whether the user linked Spotify; set by the screen. */
    var remoteConnected = true

    val queueNotice: QueueNotice? get() = queue.notice

    /**
     * An event is focused: only the event's music plays ([forEvent]), and the buttons drive it ([eventRemote]).
     * Playing a song of their own makes the user leave the event ([leaveEvent]); songs added to the queue are
     * proposed to it ([proposeToEvent]).
     */
    var lockedByEvent by mutableStateOf(false)
        private set

    /** Proposes songs to the focused event; while [lockedByEvent], songs added to the queue go there. Set by the focus. */
    var proposeToEvent: ((List<TrackResponse>) -> Unit)? = null

    /**
     * The focused event's remote, set by the focus: the buttons then drive the event's music (its owner only), and the
     * ones the event cannot do are greyed out, see [canTogglePlay], [canSkipNext], [canSkipPrevious] and [canSeek].
     */
    var eventRemote by mutableStateOf<EventRemote?>(null)

    /** Stops listening to the focused event; set by the focus. Called when the user plays a song of their own. */
    var leaveEvent: (() -> Unit)? = null

    /** A command sent by the focused event, allowed while [lockedByEvent]. */
    private var eventCommand = false

    private var activeDeviceSource by mutableStateOf<TrackSource?>(null)
    val playingHosted: Boolean get() = activeDeviceSource == TrackSource.MUSIC_ROOM
    val playingAppleMusic: Boolean get() = activeDeviceSource == TrackSource.APPLE_MUSIC

    /** A list the app plays song after song, as it holds uploaded songs; null while Spotify plays on its own. */
    private var appList by mutableStateOf<List<TrackResponse>?>(null)
    private var appIndex by mutableStateOf(0)

    /** Spotify was paused in an album or playlist for uploaded songs: it goes on after them. */
    private var resumeRemote = false

    private val deviceProviders = environment.devices(
        onEnded = { advance() },
        onError = { failure ->
            error = UiText.of(failure)
            state?.let { update(it.copy(isPlaying = false)) }
        },
    )

    private val playbackRouter = TrackPlaybackRouter(buildList {
        deviceProviders.all.forEach { session ->
            add(object : TrackPlaybackRouter.Provider {
                override val source = session.source
                override val managesOwnQueue = false
                override fun availabilityError() = session.availabilityError()
                override fun connectionError() = session.connectionError()
                override fun play(track: TrackResponse) {
                    val problem = session.availabilityError() ?: session.connectionError()
                    if (problem != null) error = problem else startDevicePlayback(session, track)
                }
            })
        }
        add(object : TrackPlaybackRouter.Provider {
            override val source = remote.source
            override val managesOwnQueue = true
            override fun play(track: TrackResponse) {
                if (requireRemote()) playOnRemote(track) { remote.play(listOf(track.uri), 0) }
                else stopAtEnd()
            }
        })
    })

    private var queueChanges by mutableStateOf(0)

    private var syncedAt by mutableStateOf(TimeSource.Monotonic.markNow())
    private val synchronizer = RemotePlaybackSynchronizer()

    val track: TrackResponse? get() = state?.track
    val isPlaying: Boolean get() = state?.isPlaying == true
    val canTransferPlayback: Boolean get() = deviceSession == null

    /** What the play / pause button shows: the focused event's status, else what the player does. */
    val showsPlaying: Boolean get() = eventRemote?.playing ?: isPlaying

    /** With an event focused, only its owner starts, pauses and skips; the event has no "previous" and no seek. */
    val canTogglePlay: Boolean get() = eventRemote?.canControl ?: true
    val canSkipNext: Boolean get() = eventRemote?.let { it.canControl && it.canSkip } ?: true
    val canSkipPrevious: Boolean get() = eventRemote == null
    val canSeek: Boolean get() = eventRemote == null

    /** What Spotify plays after [upNext] on its own; the song already handed to it is left out. */
    val following: List<TrackResponse>
        get() {
            appList?.let { return it.drop(appIndex + 1) }
            if ((deviceSession != null) && !resumeRemote) return emptyList()
            val sentId = upNext.firstOrNull { it.key == sentKey }?.track?.id ?: return remoteQueue
            val index = remoteQueue.indexOfFirst { it.id == sentId }
            return if (index < 0) remoteQueue else remoteQueue.toMutableList().apply { removeAt(index) }
        }

    private val deviceSession: DevicePlaybackSession?
        get() = activeDeviceSource?.let(deviceProviders::find)

    /** Position in the track, moving forward between two syncs while it plays. */
    fun positionMs(): Long {
        val current = state ?: return 0
        deviceSession?.let { return it.positionMs().coerceAtMost(current.track?.durationMs ?: Long.MAX_VALUE) }
        val elapsed = if (current.isPlaying) syncedAt.elapsedNow().inWholeMilliseconds else 0
        val position = current.progressMs + elapsed
        return current.track?.durationMs?.let { position.coerceAtMost(it) } ?: position
    }

    /**
     * Plays [tracks] from [index]; the rest of the list follows as the queue. Spotify plays a list of Spotify songs
     * on its own; with uploaded songs in it, the app plays it song after song.
     */
    fun play(tracks: List<TrackResponse>, index: Int) {
        val track = tracks.getOrNull(index) ?: return
        playbackRouter.availabilityError(track)?.let {
            error = it
            return
        }
        if (tracks.none(::isAppTrack)) {
            if (!requireRemote()) return
            appList = null
            resumeRemote = false
            playOnRemote(track) { remote.play(tracks.map { it.uri }, index) }
            return
        }
        if (!allowed()) return
        appList = tracks
        appIndex = index
        resumeRemote = false
        playSingle(track)
    }

    /** Plays [playlist] as a whole from [index]; [tracks] (when Spotify listed them) shows the track right away. */
    fun playPlaylist(playlist: PlaylistSummaryResponse, tracks: List<TrackResponse>, index: Int) {
        val uri = playlist.uri ?: return play(tracks, index)
        playContext(uri, tracks, index)
    }

    /** Plays [album] as a whole from its track at [index], shown right away from [tracks]. */
    fun playAlbum(album: AlbumResponse, tracks: List<TrackResponse>, index: Int) {
        if (tracks.getOrNull(index)?.let(::isAppTrack) == true) play(tracks, index)
        else playContext(album.uri, tracks, index)
    }

    /** Plays a Spotify playlist or album ([uri]) from [index]. */
    private fun playContext(uri: String, tracks: List<TrackResponse>, index: Int) {
        if (!requireRemote()) return
        val track = tracks.getOrNull(index)
        val device = remoteDevice()
        appList = null
        resumeRemote = false
        leaveDevicePlayback()
        track?.let {
            update(PlaybackStateResponse(active = true, isPlaying = true, track = it, deviceName = device))
        }
        synchronizer.waitFor(track?.id)
        command { remote.play(index = index, contextUri = uri) }
    }

    /** Plays [following] from its track at [index]; the tracks after it follow. */
    fun playFollowing(index: Int) {
        val list = appList ?: return play(following, index)
        if (!allowed()) return
        appIndex += 1 + index
        list.getOrNull(appIndex)?.let { playSingle(it) }
    }

    /** Adds [tracks] at the end of [upNext], proposed by [proposer] (the user by default). */
    fun addToQueue(tracks: List<TrackResponse>, proposer: Proposer? = null) =
        addProposed(tracks.map { it to (proposer ?: me) })

    /**
     * Adds songs at the end of [upNext], each with who proposed it, and confirms it in [queueNotice]. While an event
     * is focused, the event's queue is the queue: the songs are proposed to it instead.
     */
    fun addProposed(songs: List<Pair<TrackResponse, Proposer?>>): Boolean {
        // Proposing to a focused event plays nothing here: no need for the local Apple Music playback.
        val toEvent = proposeToEvent
        if (lockedByEvent && toEvent != null) {
            toEvent(songs.map { it.first })
            return true
        }
        songs.firstNotNullOfOrNull { playbackRouter.playbackError(it.first) }?.let {
            error = it
            return false
        }
        if (songs.isEmpty()) return false
        // Uploaded songs play without Spotify.
        if (songs.all { isAppTrack(it.first) }) {
            if (!allowed()) return false
        } else if (!requireRemote()) {
            return false
        }
        var added = queue.append(songs)
        if (track == null) {
            // Nothing plays: the first song starts now (the player shows it, no need to confirm it).
            val first = requireNotNull(queue.takeFirst())
            added = added - first
            play(listOf(first.track), 0)
        } else {
            sendIfEnding()
        }
        queue.announce(added)
        return true
    }

    fun canUndo(notice: QueueNotice): Boolean = queue.canUndo(notice)
    fun undo(notice: QueueNotice) = queue.undo(notice)
    fun dismissNotice() = queue.dismissNotice()
    fun move(key: Long, offset: Int) = queue.move(key, offset)
    fun removeFromQueue(key: Long) = queue.remove(key)

    /** Plays an [upNext] entry now; the current album or playlist goes on after it. */
    fun playNow(entry: QueueEntry) {
        if (!allowed()) return
        when (sentKey) {
            entry.key -> next()
            null -> {
                queue.bringToFront(entry)
                next()
            }
            else -> {
                removeFromQueue(entry.key)
                if (appDriven()) playSingle(entry.track) else play(listOf(entry.track), 0)
            }
        }
    }

    /** Reloads Spotify's queue when the track changes or a song is handed to it, until cancelled (while shown). */
    suspend fun followQueue() {
        snapshotFlow { track?.id to queueChanges }.collectLatest {
            // Spotify takes a moment to show a skip or an added song in its queue.
            delay(800.milliseconds)
            try {
                remoteQueue = remote.queue().tracks
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Keep the last known queue.
            }
        }
    }

    /** Reloads [devices]; throws when Spotify cannot be reached. */
    suspend fun loadDevices() {
        devices = remote.devices().devices
    }

    /** Moves playback to [device], playing there unless it was paused. */
    fun transferTo(device: DeviceResponse) {
        // App-local tracks do not play on Spotify devices.
        if (device.isActive || deviceSession != null) return
        val play = state?.track == null || isPlaying
        devices = devices?.map { it.copy(isActive = it.id == device.id) }
        state?.let { update(it.copy(deviceName = device.name, progressMs = positionMs())) }
        // While Spotify moves the music it can briefly report it paused or playing nowhere: keep what is shown.
        synchronizer.waitFor(track?.id, device = device.name, playing = play)
        command { remote.transferPlayback(device.id, play) }
    }

    fun togglePlay() {
        if (viaEvent { it.togglePlay() }) return
        if (!allowed()) return
        val current = state ?: return
        val local = deviceSession
        if (local != null) {
            if (current.isPlaying) {
                local.pause()
                update(current.copy(isPlaying = false, progressMs = local.positionMs()))
            } else if (local.canResume) {
                local.resume()
                update(current.copy(isPlaying = true, progressMs = local.positionMs()))
            } else {
                current.track?.let(::playSingle)
            }
            return
        }
        update(current.copy(isPlaying = !current.isPlaying, progressMs = positionMs()))
        command { if (current.isPlaying) remote.pause() else remote.play() }
    }

    /** Skips to the first [upNext] song, or to the next song of the list. */
    fun next() {
        if (viaEvent { if (it.canSkip) it.skip() }) return
        if (allowed()) advance()
    }

    /** Back to the previous song of a list the app plays (at its start), else to the start of the song. */
    fun previous() {
        if (eventRemote != null && !eventCommand) return
        if (!allowed()) return
        val list = appList
        when {
            list != null && appIndex > 0 && positionMs() < RESTART_THRESHOLD.inWholeMilliseconds -> {
                appIndex--
                playSingle(list[appIndex])
            }
            deviceSession != null || list != null -> seek(0)
            else -> command { remote.skipPrevious() }
        }
    }

    fun seek(positionMs: Long) {
        if (eventRemote != null && !eventCommand) return
        if (!allowed()) return
        state?.let { update(it.copy(progressMs = positionMs)) }
        val local = deviceSession
        if (local != null) local.seek(positionMs) else command { remote.seek(positionMs) }
    }

    /**
     * Goes to what comes next: the first [upNext] song (handed to Spotify when Spotify plays on its own), else the next
     * song of [appList], else Spotify's next track. At the end of the list, the last song stays, paused.
     */
    private fun advance() {
        val first = upNext.firstOrNull()?.takeIf { sentKey == null }
        val list = appList
        when {
            // Back to the paused Spotify album or playlist: the Spotify song goes before what it plays next.
            first != null && (deviceSession != null) && resumeRemote && !isAppTrack(first.track) -> {
                leaveDevicePlayback()
                resumeRemote = false
                send(first) { remote.skipNext() }
            }
            first != null && appDriven() -> {
                if (list == null && deviceSession == null && isAppTrack(first.track)) resumeRemote = true
                queue.remove(first)
                playSingle(first.track)
            }
            first != null -> send(first) { remote.skipNext() }
            list != null && appIndex < list.lastIndex -> {
                appIndex++
                playSingle(list[appIndex])
            }
            (deviceSession != null) && resumeRemote -> {
                leaveDevicePlayback()
                resumeRemote = false
                command { remote.skipNext() }
            }
            deviceSession != null || list != null -> stopAtEnd()
            else -> command { remote.skipNext() }
        }
    }

    /** The app chooses each song (a list with app-local songs, or an app-local song playing or next). */
    private fun appDriven() = appList != null || deviceSession != null || upNext.firstOrNull()?.track?.let(::isAppTrack) == true

    private fun isAppTrack(track: TrackResponse) = playbackRouter.appManaged(track)

    private fun playSingle(track: TrackResponse) = playbackRouter.play(track)

    private fun playOnRemote(track: TrackResponse, start: suspend () -> Unit) {
        val device = remoteDevice()
        leaveDevicePlayback()
        update(PlaybackStateResponse(active = true, isPlaying = true, track = track, deviceName = device))
        synchronizer.waitFor(track.id)
        command(action = start)
    }

    private fun startDevicePlayback(session: DevicePlaybackSession, track: TrackResponse) {
        val wasLocal = deviceSession != null
        if (!wasLocal && isPlaying) command { remote.pause() }
        leaveDevicePlayback()
        activeDeviceSource = session.source
        synchronizer.stopWaiting()
        update(PlaybackStateResponse(active = true, isPlaying = true, track = track, deviceName = HOSTED_DEVICE))
        session.start(track)
    }

    private fun leaveDevicePlayback() {
        val session = deviceSession ?: return
        activeDeviceSource = null
        session.stop()
    }

    /** The list is over: its last song stays shown, paused at its start. */
    private fun stopAtEnd() {
        deviceSession?.pauseAtEnd()
        state?.let { update(it.copy(isPlaying = false, progressMs = 0)) }
    }

    /** The Spotify device last known (not the app, while it plays an app-local song). */
    private fun remoteDevice(): String? = if (deviceSession != null) null else state?.deviceName

    /** The app plays the list, or an uploaded song comes next: near the end of a Spotify song, it starts the next one. */
    private fun advanceIfRemoteEnding() {
        if (!remoteEndsSoon(END_MARGIN.inWholeMilliseconds)) return
        advance()
    }

    private fun remoteEndsSoon(withinMs: Long): Boolean {
        if (lockedByEvent || deviceSession != null || !isPlaying || sentKey != null) return false
        if (appList == null && upNext.firstOrNull()?.track?.let(::isAppTrack) != true) return false
        val duration = track?.durationMs ?: return false
        return duration - positionMs() <= withinMs
    }

    /** Stops an uploaded song playing in the app (leaving the signed-in screens). Spotify plays on its own. */
    fun release() {
        leaveDevicePlayback()
    }

    /** Stops device-local event audio before the host's Spotify takes over. */
    fun stopEventLocalPlayback() {
        if (deviceSession == null) return
        leaveDevicePlayback()
        state = null
        synchronizer.requestRefresh()
    }

    /** Returns false when Spotify playback is unavailable. */
    private fun requireRemote(): Boolean {
        if (!allowed()) return false
        if (!remoteConnected) error = remote.notConnectedError()
        return remoteConnected
    }

    /** Locks the player for a focused event ([lockedByEvent]), or gives it back to the user. */
    fun lockForEvent(locked: Boolean) {
        lockedByEvent = locked
        if (!locked && error == LOCKED_ERROR) dismissError()
    }

    /** Runs [commands] for the focused event: they pass the lock. */
    fun forEvent(commands: () -> Unit) {
        eventCommand = true
        try {
            commands()
        } finally {
            eventCommand = false
        }
    }

    /**
     * A button pressed while an event is focused drives the event's music (its owner only), not the user's own: runs
     * [command] on the remote when the user may, and returns true. False when no event is focused, and for the
     * focus's own commands ([forEvent]).
     */
    private fun viaEvent(command: (EventRemote) -> Unit): Boolean {
        val remote = eventRemote ?: return false
        if (eventCommand) return false
        if (remote.canControl) command(remote)
        return true
    }

    /**
     * Always true: when the user plays a song of their own while an event is focused, they simply stop listening to
     * it ([leaveEvent]) and the command goes on. The focus's own commands ([forEvent]) never leave it.
     */
    private fun allowed(): Boolean {
        if (!lockedByEvent || eventCommand) return true
        val leave = leaveEvent
        if (leave != null) {
            leave()
        } else {
            eventRemote = null
            lockForEvent(false)
        }
        return true
    }

    fun dismissError() {
        error = null
        needsDevice = false
    }

    fun reportPlaybackError(problem: UiText) {
        error = problem
    }

    /** Keeps the state in sync with Spotify until cancelled: often while playing, rarely otherwise. */
    suspend fun sync() = synchronizer.run(
        fetch = { remote.playbackState() },
        onState = { remote ->
            val shown = state?.track
            val fetched = if (remote.active && remote.isPlaying && remote.track == null && shown != null) remote.copy(track = shown) else remote
            if (deviceSession == null) {
                if (!synchronizer.isStillStarting(fetched)) update(fetched)
                sendIfEnding()
                advanceIfRemoteEnding()
            }
        },
        interval = {
            when {
                synchronizer.waiting -> 1.seconds
                remoteEndsSoon(WATCH_END.inWholeMilliseconds) -> 500.milliseconds
                isPlaying -> 5.seconds
                else -> 15.seconds
            }
        },
    )

    private fun update(newState: PlaybackStateResponse) {
        state = newState
        syncedAt = TimeSource.Monotonic.markNow()
        queue.onTrackChanged(newState.track?.id)
    }

    /** Hands the first [upNext] song to Spotify when the current track is about to end. */
    private fun sendIfEnding() {
        // The event chooses what plays next; the app starts the songs of its own lists and the uploaded ones.
        if (lockedByEvent || deviceSession != null || appList != null) return
        val first = upNext.firstOrNull()?.takeUnless { isAppTrack(it.track) } ?: return
        val duration = track?.durationMs ?: return
        if (sentKey != null || duration - positionMs() > SEND_BEFORE_END.inWholeMilliseconds) return
        send(first)
    }

    /** Adds [entry] to Spotify's queue, then runs [then]; on failure it can move again. */
    private fun send(entry: QueueEntry, then: suspend () -> Unit = {}) {
        queue.markSent(entry)
        command(onError = { queue.cancelSent(entry.key) }) {
            remote.addToQueue(listOf(entry.track.uri))
            queueChanges++
            then()
        }
    }

    private fun command(onError: () -> Unit = {}, action: suspend () -> Unit) {
        scope.launch {
            try {
                action()
                dismissError()
                // Spotify takes a moment to apply a command before its state shows it.
                delay(700.milliseconds)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                synchronizer.stopWaiting()
                onError()
                // Unlinked meanwhile (from another device): ask to link it again rather than show an error.
                if (remote.isDisconnected(e)) {
                    state = null
                    error = UiText.of(e)
                    synchronizer.requestRefresh()
                    return@launch
                }
                error = UiText.of(e)
                needsDevice = remote.isMissingDevice(e)
            }
            synchronizer.requestRefresh()
        }
    }

    companion object {
        /** Shown when the user plays music themselves while an event is focused. */
        private val LOCKED_ERROR = UiText.of(Res.string.player_locked_by_event)

        /** How long before the end of a track the next queued song is handed to Spotify (polls are 5 s apart). */
        private val SEND_BEFORE_END = 20.seconds

        /** Shown as the device while the app plays an uploaded song. */
        const val HOSTED_DEVICE = "Music Room"

        /** How long before the end of a Spotify song the app starts the next one of its list. */
        private val END_MARGIN = 1.seconds

        /** From how long before the end of a Spotify song the app checks it often. */
        private val WATCH_END = 8.seconds

        /** "Previous" restarts the song after this, and goes to the previous one before. */
        private val RESTART_THRESHOLD = 3.seconds

        /** Opens the Spotify app on phones; on computers the web player, which also counts as a device. */
        const val SPOTIFY_WEB_URL = "https://open.remote.com"
    }
}
