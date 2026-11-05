package com.musicroom.app.player

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.musicroom.app.common.UiText
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.player_locked_by_event
import com.musicroom.shared.ApiErrors
import com.musicroom.shared.ApiException
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.AlbumResponse
import com.musicroom.shared.dto.DeviceResponse
import com.musicroom.shared.dto.PlaybackStateResponse
import com.musicroom.shared.dto.PlaylistSummaryResponse
import com.musicroom.shared.dto.TrackResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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
 * ([playingHosted]). A list holding some of them is played by the app song after song ([appList]): each Spotify song
 * is started on its own, and the next one starts when it ends. An uploaded song queued during a Spotify album or
 * playlist pauses it, which goes on afterwards ([resumeSpotify]).
 */
@Stable
class PlayerController(private val api: MusicRoomApi, private val scope: CoroutineScope) {
    var state by mutableStateOf<PlaybackStateResponse?>(null)
        private set

    var error by mutableStateOf<UiText?>(null)
        private set

    /** The last error can be fixed by opening Spotify on a device. */
    var needsDevice by mutableStateOf(false)
        private set

    /** Songs added in the app, in the order they will play. */
    var upNext by mutableStateOf<List<QueueEntry>>(emptyList())
        private set

    /** The [upNext] entry already handed to Spotify: it plays next and can no longer move. Always the first one. */
    var sentKey by mutableStateOf<Long?>(null)
        private set

    /** Spotify's own queue (songs added elsewhere, then the rest of the album or playlist), from [followQueue]. */
    private var spotifyQueue by mutableStateOf<List<TrackResponse>>(emptyList())

    /** The user's devices with Spotify open, from [loadDevices]; null until loaded. */
    var devices by mutableStateOf<List<DeviceResponse>?>(null)
        private set

    /** Who adds songs from this app; set by the screen once the user is known. */
    var me: Proposer? = null

    /** Whether the user linked Spotify; set by the screen. Without it, playing asks to link it ([needsSpotify]). */
    var spotifyConnected = true

    /** The user tried to play or queue music without a linked Spotify account: the screen asks them to link it. */
    var needsSpotify by mutableStateOf(false)
        private set

    /** Confirmation of the last songs added to the queue, cleared after a few seconds. */
    var queueNotice by mutableStateOf<QueueNotice?>(null)
        private set

    /**
     * An event is focused: only the event's music plays ([forEvent]); the user's own commands (play, pause, skip,
     * queue...) are refused with an [error] until they stop listening to it.
     */
    var lockedByEvent by mutableStateOf(false)
        private set

    /** Proposes songs to the focused event; while [lockedByEvent], songs added to the queue go there. Set by the focus. */
    var proposeToEvent: ((List<TrackResponse>) -> Unit)? = null

    /** A command sent by the focused event, allowed while [lockedByEvent]. */
    private var eventCommand = false

    /** The song playing was uploaded to Music Room: the app plays it, Spotify waits paused. */
    var playingHosted by mutableStateOf(false)
        private set

    /** A list the app plays song after song, as it holds uploaded songs; null while Spotify plays on its own. */
    private var appList by mutableStateOf<List<TrackResponse>?>(null)
    private var appIndex by mutableStateOf(0)

    /** Spotify was paused in an album or playlist for uploaded songs: it goes on after them. */
    private var resumeSpotify = false

    private val hosted = HostedPlayback(
        api,
        scope,
        onEnded = { advance() },
        onError = { e ->
            error = UiText.of(e)
            state?.let { update(it.copy(isPlaying = false)) }
        },
    )

    private var queueChanges by mutableStateOf(0)
    private var noticeJob: Job? = null
    private var nextKey = 0L
    private var nextNoticeId = 0L

    private var syncedAt by mutableStateOf(TimeSource.Monotonic.markNow())
    private val refreshNow = Channel<Unit>(Channel.CONFLATED)

    /**
     * After a play command, Spotify can take a few seconds to start on a device that was idle (a phone in
     * the background): until then it reports nothing playing. Polls that do not show the requested track
     * yet are ignored until [pendingUntil], so the track does not disappear from the player.
     */
    private var pendingTrackId: String? = null
    private var pendingUntil: TimeSource.Monotonic.ValueTimeMark? = null

    /** After moving playback: polls are ignored until they show the music on [pendingDevice] (playing if [pendingPlaying]). */
    private var pendingDevice: String? = null
    private var pendingPlaying = false

    val track: TrackResponse? get() = state?.track
    val isPlaying: Boolean get() = state?.isPlaying == true

    /** What Spotify plays after [upNext] on its own; the song already handed to it is left out. */
    val following: List<TrackResponse>
        get() {
            appList?.let { return it.drop(appIndex + 1) }
            if (playingHosted && !resumeSpotify) return emptyList()
            val sentId = upNext.firstOrNull { it.key == sentKey }?.track?.id ?: return spotifyQueue
            val index = spotifyQueue.indexOfFirst { it.id == sentId }
            return if (index < 0) spotifyQueue else spotifyQueue.toMutableList().apply { removeAt(index) }
        }

    /** Position in the track, moving forward between two syncs while it plays. */
    fun positionMs(): Long {
        val current = state ?: return 0
        if (playingHosted) return current.track?.durationMs?.let { hosted.positionMs().coerceAtMost(it) } ?: hosted.positionMs()
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
        if (tracks.none { it.hosted }) {
            if (!requireSpotify()) return
            appList = null
            playOnSpotify(track) { api.play(tracks.map { it.uri }, index) }
            return
        }
        if (!allowed()) return
        appList = tracks
        appIndex = index
        resumeSpotify = false
        playSingle(track)
    }

    /** Plays [playlist] as a whole from [index]; [tracks] (when Spotify listed them) shows the track right away. */
    fun playPlaylist(playlist: PlaylistSummaryResponse, tracks: List<TrackResponse>, index: Int) {
        val uri = playlist.uri ?: return play(tracks, index)
        playContext(uri, tracks, index)
    }

    /** Plays [album] as a whole from its track at [index], shown right away from [tracks]. */
    fun playAlbum(album: AlbumResponse, tracks: List<TrackResponse>, index: Int) = playContext(album.uri, tracks, index)

    /** Plays a Spotify playlist or album ([uri]) from [index]. */
    private fun playContext(uri: String, tracks: List<TrackResponse>, index: Int) {
        if (!requireSpotify()) return
        val track = tracks.getOrNull(index)
        val device = spotifyDevice()
        appList = null
        resumeSpotify = false
        leaveHosted()
        track?.let {
            update(PlaybackStateResponse(active = true, isPlaying = true, track = it, deviceName = device))
        }
        waitFor(track?.id)
        command { api.play(index = index, contextUri = uri) }
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
    fun addProposed(songs: List<Pair<TrackResponse, Proposer?>>) {
        val toEvent = proposeToEvent
        if (lockedByEvent && toEvent != null) return toEvent(songs.map { it.first })
        if (songs.isEmpty()) return
        // Uploaded songs play without Spotify.
        if (songs.all { it.first.hosted }) {
            if (!allowed()) return
        } else if (!requireSpotify()) {
            return
        }
        var added = songs.map { (track, proposer) -> QueueEntry(nextKey++, track, proposer) }
        upNext = upNext + added
        if (track == null) {
            // Nothing plays: the first song starts now (the player shows it, no need to confirm it).
            val first = upNext.first()
            upNext = upNext.drop(1)
            added = added - first
            play(listOf(first.track), 0)
        } else {
            sendIfEnding()
        }
        if (added.isNotEmpty()) showNotice(QueueNotice(nextNoticeId++, added))
    }

    /** The songs of [notice] can still be taken back: some wait in [upNext] and were not handed to Spotify. */
    fun canUndo(notice: QueueNotice): Boolean = notice.entries.any { entry -> entry.key != sentKey && upNext.any { it.key == entry.key } }

    /** Takes back the songs of [notice] that were not handed to Spotify yet. */
    fun undo(notice: QueueNotice) {
        val keys = notice.entries.map { it.key }.toSet() - setOfNotNull(sentKey)
        upNext = upNext.filterNot { it.key in keys }
        dismissNotice()
    }

    fun dismissNotice() {
        noticeJob?.cancel()
        queueNotice = null
    }

    /** Moves an [upNext] entry [offset] places (negative: earlier). The song handed to Spotify stays first. */
    fun move(key: Long, offset: Int) {
        val from = upNext.indexOfFirst { it.key == key }
        if (from < 0 || key == sentKey) return
        val to = (from + offset).coerceIn(if (sentKey != null) 1 else 0, upNext.lastIndex)
        if (to != from) upNext = upNext.toMutableList().apply { add(to, removeAt(from)) }
    }

    /** Removes an [upNext] entry, unless it was already handed to Spotify. */
    fun removeFromQueue(key: Long) {
        if (key != sentKey) upNext = upNext.filterNot { it.key == key }
    }

    /** Plays an [upNext] entry now; the current album or playlist goes on after it. */
    fun playNow(entry: QueueEntry) {
        if (!allowed()) return
        when (sentKey) {
            entry.key -> next()
            null -> {
                upNext = listOf(entry) + upNext.filterNot { it.key == entry.key }
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
                spotifyQueue = api.queue().tracks
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Keep the last known queue.
            }
        }
    }

    /** Reloads [devices]; throws when Spotify cannot be reached. */
    suspend fun loadDevices() {
        devices = api.devices().devices
    }

    /** Moves playback to [device], playing there unless it was paused. */
    fun transferTo(device: DeviceResponse) {
        // An uploaded song plays in the app, not on a Spotify device.
        if (device.isActive || playingHosted) return
        val play = state?.track == null || isPlaying
        devices = devices?.map { it.copy(isActive = it.id == device.id) }
        state?.let { update(it.copy(deviceName = device.name, progressMs = positionMs())) }
        // While Spotify moves the music it can briefly report it paused or playing nowhere: keep what is shown.
        waitFor(track?.id, device = device.name, playing = play)
        command { api.transferPlayback(device.id, play) }
    }

    fun togglePlay() {
        if (!allowed()) return
        val current = state ?: return
        if (playingHosted) {
            if (current.isPlaying) hosted.pause() else hosted.resume()
            update(current.copy(isPlaying = !current.isPlaying, progressMs = positionMs()))
            return
        }
        update(current.copy(isPlaying = !current.isPlaying, progressMs = positionMs()))
        command { if (current.isPlaying) api.pause() else api.play() }
    }

    /** Skips to the first [upNext] song, or to the next song of the list. */
    fun next() {
        if (allowed()) advance()
    }

    /** Back to the previous song of a list the app plays (at its start), else to the start of the song. */
    fun previous() {
        if (!allowed()) return
        val list = appList
        when {
            list != null && appIndex > 0 && positionMs() < RESTART_THRESHOLD.inWholeMilliseconds -> {
                appIndex--
                playSingle(list[appIndex])
            }
            playingHosted || list != null -> seek(0)
            else -> command { api.skipPrevious() }
        }
    }

    fun seek(positionMs: Long) {
        if (!allowed()) return
        state?.let { update(it.copy(progressMs = positionMs)) }
        if (playingHosted) hosted.seek(positionMs) else command { api.seek(positionMs) }
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
            first != null && playingHosted && resumeSpotify && !first.track.hosted -> {
                leaveHosted()
                resumeSpotify = false
                send(first) { api.skipNext() }
            }
            first != null && appDriven() -> {
                if (list == null && !playingHosted && first.track.hosted) resumeSpotify = true
                upNext = upNext - first
                playSingle(first.track)
            }
            first != null -> send(first) { api.skipNext() }
            list != null && appIndex < list.lastIndex -> {
                appIndex++
                playSingle(list[appIndex])
            }
            playingHosted && resumeSpotify -> {
                leaveHosted()
                resumeSpotify = false
                command { api.skipNext() }
            }
            playingHosted || list != null -> stopAtEnd()
            else -> command { api.skipNext() }
        }
    }

    /** The app chooses each song (a list with uploaded songs, or an uploaded song playing or next). */
    private fun appDriven() = appList != null || playingHosted || upNext.firstOrNull()?.track?.hosted == true

    /** Plays one song: in the app if it was uploaded to Music Room, else on Spotify. */
    private fun playSingle(track: TrackResponse) {
        if (track.hosted) {
            startHosted(track)
        } else if (requireSpotify()) {
            playOnSpotify(track) { api.play(listOf(track.uri), 0) }
        } else {
            // No Spotify to play it: the list stops there.
            stopAtEnd()
        }
    }

    private fun playOnSpotify(track: TrackResponse, start: suspend () -> Unit) {
        val device = spotifyDevice()
        leaveHosted()
        update(PlaybackStateResponse(active = true, isPlaying = true, track = track, deviceName = device))
        waitFor(track.id)
        command(action = start)
    }

    private fun startHosted(track: TrackResponse) {
        // Spotify goes quiet while the app plays.
        if (!playingHosted && isPlaying) command { api.pause() }
        playingHosted = true
        stopWaiting()
        update(PlaybackStateResponse(active = true, isPlaying = true, track = track, deviceName = HOSTED_DEVICE))
        hosted.play(track)
    }

    private fun leaveHosted() {
        if (!playingHosted) return
        hosted.stop()
        playingHosted = false
    }

    /** The list is over: its last song stays shown, paused at its start. */
    private fun stopAtEnd() {
        if (playingHosted) {
            hosted.pause()
            hosted.seek(0)
        }
        state?.let { update(it.copy(isPlaying = false, progressMs = 0)) }
    }

    /** The Spotify device last known (not the app, while it plays an uploaded song). */
    private fun spotifyDevice(): String? = if (playingHosted) null else state?.deviceName

    /** The app plays the list, or an uploaded song comes next: near the end of a Spotify song, it starts the next one. */
    private fun advanceIfSpotifyEnding() {
        if (!spotifyEndsSoon(END_MARGIN.inWholeMilliseconds)) return
        advance()
    }

    private fun spotifyEndsSoon(withinMs: Long): Boolean {
        if (lockedByEvent || playingHosted || !isPlaying || sentKey != null) return false
        if (appList == null && upNext.firstOrNull()?.track?.hosted != true) return false
        val duration = track?.durationMs ?: return false
        return duration - positionMs() <= withinMs
    }

    /** Stops an uploaded song playing in the app (leaving the signed-in screens). Spotify plays on its own. */
    fun release() {
        leaveHosted()
    }

    /** Asks the user to link Spotify (e.g. to host an event's music). */
    fun askToLinkSpotify() {
        needsSpotify = true
    }

    fun dismissNeedsSpotify() {
        needsSpotify = false
    }

    /** False (and asks to link Spotify) when the user has no Spotify account linked. */
    private fun requireSpotify(): Boolean {
        if (!allowed()) return false
        if (!spotifyConnected) needsSpotify = true
        return spotifyConnected
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

    /** False, with an explanation, for the user's own commands while an event is focused. */
    private fun allowed(): Boolean {
        if (!lockedByEvent || eventCommand) return true
        needsDevice = false
        error = LOCKED_ERROR
        return false
    }

    fun dismissError() {
        error = null
        needsDevice = false
    }

    /** Keeps the state in sync with Spotify until cancelled: often while playing, rarely otherwise. */
    suspend fun sync() {
        while (true) {
            try {
                val fetched = api.playbackState().let { fetched ->
                    // Spotify can report the music playing without saying what (just after it moved to another
                    // device): the last known track stays shown rather than the player disappearing.
                    val shown = state?.track
                    if (fetched.active && fetched.isPlaying && fetched.track == null && shown != null) fetched.copy(track = shown) else fetched
                }
                // While the app plays an uploaded song, Spotify (paused) does not say what is shown.
                if (!playingHosted) {
                    if (!isStillStarting(fetched)) update(fetched)
                    sendIfEnding()
                    advanceIfSpotifyEnding()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Keep the last known state; a failed poll is not worth an error message.
            }
            val interval = when {
                pendingUntil != null -> 1.seconds
                // The app starts the next song itself: it watches the end closely.
                spotifyEndsSoon(WATCH_END.inWholeMilliseconds) -> 500.milliseconds
                isPlaying -> 5.seconds
                else -> 15.seconds
            }
            withTimeoutOrNull(interval) { refreshNow.receive() }
        }
    }

    private fun showNotice(notice: QueueNotice) {
        noticeJob?.cancel()
        queueNotice = notice
        noticeJob = scope.launch {
            delay(NOTICE_DURATION)
            queueNotice = null
        }
    }

    private fun waitFor(trackId: String?, device: String? = null, playing: Boolean = false) {
        pendingTrackId = trackId
        pendingDevice = device
        pendingPlaying = playing
        pendingUntil = TimeSource.Monotonic.markNow() + START_TIMEOUT
    }

    private fun stopWaiting() {
        pendingTrackId = null
        pendingDevice = null
        pendingPlaying = false
        pendingUntil = null
    }

    /** [fetched] does not show the requested track (or device) yet, while Spotify may still be starting it. */
    private fun isStillStarting(fetched: PlaybackStateResponse): Boolean {
        val until = pendingUntil ?: return false
        val expected = pendingTrackId
        val started = fetched.track != null && (expected == null || fetched.track?.id == expected) &&
            (pendingDevice == null || fetched.deviceName == pendingDevice) && (!pendingPlaying || fetched.isPlaying)
        if (started || until.hasPassedNow()) {
            stopWaiting()
            return false
        }
        return true
    }

    private fun update(newState: PlaybackStateResponse) {
        state = newState
        syncedAt = TimeSource.Monotonic.markNow()
        // The song handed to Spotify started: it leaves the queue.
        val sent = upNext.firstOrNull { it.key == sentKey }
        if (sent != null && newState.track?.id == sent.track.id) {
            upNext = upNext - sent
            sentKey = null
        }
    }

    /** Hands the first [upNext] song to Spotify when the current track is about to end. */
    private fun sendIfEnding() {
        // The event chooses what plays next; the app starts the songs of its own lists and the uploaded ones.
        if (lockedByEvent || playingHosted || appList != null) return
        val first = upNext.firstOrNull()?.takeUnless { it.track.hosted } ?: return
        val duration = track?.durationMs ?: return
        if (sentKey != null || duration - positionMs() > SEND_BEFORE_END.inWholeMilliseconds) return
        send(first)
    }

    /** Adds [entry] to Spotify's queue, then runs [then]; on failure it can move again. */
    private fun send(entry: QueueEntry, then: suspend () -> Unit = {}) {
        sentKey = entry.key
        command(onError = { if (sentKey == entry.key) sentKey = null }) {
            api.addToQueue(listOf(entry.track.uri))
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
                stopWaiting()
                onError()
                // Unlinked meanwhile (from another device): ask to link it again rather than show an error.
                if ((e as? ApiException)?.error == ApiErrors.SPOTIFY_NOT_CONNECTED) {
                    state = null
                    needsSpotify = true
                    refreshNow.trySend(Unit)
                    return@launch
                }
                error = UiText.of(e)
                needsDevice = (e as? ApiException)?.error == ApiErrors.SPOTIFY_NO_DEVICE
            }
            refreshNow.trySend(Unit)
        }
    }

    companion object {
        /** Shown when the user plays music themselves while an event is focused. */
        private val LOCKED_ERROR = UiText.of(Res.string.player_locked_by_event)

        /** How long a play command may take to show up in Spotify's state. */
        private val START_TIMEOUT = 8.seconds

        /** How long before the end of a track the next queued song is handed to Spotify (polls are 5 s apart). */
        private val SEND_BEFORE_END = 20.seconds

        /** How long the confirmation of songs added to the queue stays. */
        private val NOTICE_DURATION = 4.seconds

        /** Shown as the device while the app plays an uploaded song. */
        const val HOSTED_DEVICE = "Music Room"

        /** How long before the end of a Spotify song the app starts the next one of its list. */
        private val END_MARGIN = 1.seconds

        /** From how long before the end of a Spotify song the app checks it often. */
        private val WATCH_END = 8.seconds

        /** "Previous" restarts the song after this, and goes to the previous one before. */
        private val RESTART_THRESHOLD = 3.seconds

        /** Opens the Spotify app on phones; on computers the web player, which also counts as a device. */
        const val SPOTIFY_WEB_URL = "https://open.spotify.com"
    }
}
