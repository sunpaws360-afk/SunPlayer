package com.rebecca.sunplayer.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.rebecca.sunplayer.model.AudioTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlaybackState(
    val currentTrack: AudioTrack? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L
)

@OptIn(UnstableApi::class)
class AudioPlayerManager(private val context: Context) {

    private val player: ExoPlayer = ExoPlayer.Builder(context).build()
    private val playlist = mutableListOf<AudioTrack>()
    private var currentIndex: Int = -1

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateState()
            }

            override fun onPlaybackStateChanged(state: Int) {
                updateState()
                if (state == Player.STATE_ENDED) {
                    playNext()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateState()
            }
        })
    }

    fun setPlaylist(tracks: List<AudioTrack>, startIndex: Int = 0) {
        playlist.clear()
        playlist.addAll(tracks)
        if (tracks.isNotEmpty() && startIndex in tracks.indices) {
            playTrackAtIndex(startIndex)
        }
    }

    fun playTrack(track: AudioTrack) {
        val index = playlist.indexOfFirst { it.id == track.id }
        if (index != -1) {
            playTrackAtIndex(index)
        } else {
            playlist.add(track)
            playTrackAtIndex(playlist.size - 1)
        }
    }

    private fun playTrackAtIndex(index: Int) {
        if (index !in playlist.indices) return
        currentIndex = index
        val track = playlist[index]

        val mediaItem = MediaItem.fromUri(track.uri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
        updateState()
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_IDLE && currentIndex in playlist.indices) {
                playTrackAtIndex(currentIndex)
            } else {
                player.play()
            }
        }
        updateState()
    }

    fun playNext() {
        if (playlist.isEmpty()) return
        val nextIndex = (currentIndex + 1) % playlist.size
        playTrackAtIndex(nextIndex)
    }

    fun playPrevious() {
        if (playlist.isEmpty()) return
        val prevIndex = if (currentIndex - 1 < 0) playlist.size - 1 else currentIndex - 1
        playTrackAtIndex(prevIndex)
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        updateState()
    }

    fun updatePosition() {
        if (player.isPlaying) {
            _playbackState.value = _playbackState.value.copy(
                currentPositionMs = player.currentPosition,
                durationMs = player.duration.coerceAtLeast(0L)
            )
        }
    }

    private fun updateState() {
        val track = if (currentIndex in playlist.indices) playlist[currentIndex] else null
        _playbackState.value = PlaybackState(
            currentTrack = track,
            isPlaying = player.isPlaying,
            currentPositionMs = player.currentPosition,
            durationMs = player.duration.coerceAtLeast(0L)
        )
    }

    fun release() {
        player.release()
    }
}
