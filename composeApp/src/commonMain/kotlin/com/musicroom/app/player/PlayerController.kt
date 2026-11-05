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
            val sentId = upNext.firstOrNull { it.key == sentKey }?.track?.id ?: return spotifyQueue
            val index = spotifyQueue.indexOfFirst { it.id == sentId }
            return if (index < 0) spotifyQueue else spotifyQueue.toMutableList().apply { removeAt(index) }
        }

    /** Position in the track, moving forward between two syncs while it plays. */
    fun positionMs(): Long {
        val current = state ?: return 0
        val elapsed = if (current.isPlaying) syncedAt.elapsedNow().inWholeMilliseconds else 0
        val position = current.progressMs + elapsed
        return current.track?.durationMs?.let { position.coerceAtMost(it) } ?: position
    }

    /** Plays [tracks] from [index]; the rest of the list follows as the queue. */
    fun play(tracks: List<TrackResponse>, index: Int) {
        if (!requireSpotify()) return
        val track = tracks.getOrNull(index) ?: return
        update(PlaybackStateResponse(active = true, isPlaying = true, track = track, deviceName = state?.deviceName))
        waitFor(track.id)
        command { api.play(tracks.map { it.uri }, index) }
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
        track?.let {
            update(PlaybackStateResponse(active = true, isPlaying = true, track = it, deviceName = state?.deviceName))
        }
        waitFor(track?.id)
        command { api.play(index = index, contextUri = uri) }
    }

    /** Plays [following] from its track at [index]; the tracks after it follow. */
    fun playFollowing(index: Int) = play(following, index)

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
        if (songs.isEmpty() || !requireSpotify()) return
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
                play(listOf(entry.track), 0)
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
        if (device.isActive) return
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
        update(current.copy(isPlaying = !current.isPlaying, progressMs = positionMs()))
        command { if (current.isPlaying) api.pause() else api.play() }
    }

    /** Skips to the first [upNext] song, handing it to Spotify first, or to Spotify's next track. */
    fun next() {
        if (!allowed()) return
        val first = upNext.firstOrNull()
        if (first != null && sentKey == null) send(first) { api.skipNext() } else command { api.skipNext() }
    }

    fun previous() {
        if (allowed()) command { api.skipPrevious() }
    }

    fun seek(positionMs: Long) {
        if (!allowed()) return
        state?.let { update(it.copy(progressMs = positionMs)) }
        command { api.seek(positionMs) }
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
                if (!isStillStarting(fetched)) update(fetched)
                sendIfEnding()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Keep the last known state; a failed poll is not worth an error message.
            }
            val interval = when {
                pendingUntil != null -> 1.seconds
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
        // The event chooses what plays next.
        if (lockedByEvent) return
        val first = upNext.firstOrNull() ?: return
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

        /** Opens the Spotify app on phones; on computers the web player, which also counts as a device. */
        const val SPOTIFY_WEB_URL = "https://open.spotify.com"
    }
}
