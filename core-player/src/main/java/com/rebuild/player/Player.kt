package com.rebuild.player

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player as Media3Player
import androidx.media3.session.MediaController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 纯自研播放器封装：Media3/ExoPlayer 后端。
 * 后续可加 ijkplayer 后端（同一 Player 接口两种实现），按需切换。
 * 手势（缩放/音量/倍速）由 UI 层调用 seekTo/setVolume 触发，
 * 参考 doikki 手势设计，但代码全部重写。
 */
interface Player {
    val state: StateFlow<PlaybackState>

    fun load(url: String, headers: Map<String, String> = emptyMap())
    fun start()
    fun pause()
    fun seekTo(ms: Long)
    fun setVolume(v: Float)            // 0f..1f（手势竖向拖动映射）
    fun setSpeed(multiple: Float)       // 长按倍速
    fun setSubtitle(uriString: String?)
    fun onStateChanged(cb: (PlaybackState) -> Unit): DisposableHandle
    fun dispose()
}

sealed class PlaybackState {
    data object Idle : PlaybackState()
    data class Playing(val positionMs: Long, val durationMs: Long) : PlaybackState()
    data class Paused(val positionMs: Long) : PlaybackState()
    data class Error(val code: Int, val msg: String) : PlaybackState()
}

class DisposableHandle(private val onDispose: () -> Unit) { fun dispose() = onDispose() }

class Media3Player(private val context: Context) : Player {
    private val exo = ExoPlayer.Builder(context).build()
    private val _state = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    override val state: StateFlow<PlaybackState> = _state

    private val listener = object : Media3Player.Listener {
        override fun onPlaybackStateChanged(s: Int) {
            when (s) {
                Media3Player.STATE_READY -> _state.value =
                    PlaybackState.Playing(exo.currentPosition, exo.duration.coerceAtLeast(0))
                Media3Player.STATE_PAUSED -> _state.value =
                    PlaybackState.Paused(exo.currentPosition)
                Media3Player.STATE_ENDED -> _state.value = PlaybackState.Idle
                else -> {}
            }
        }
        override fun onPlayerError(e: androidx.media3.common.PlaybackException) {
            _state.value = PlaybackState.Error(e.errorCode, e.message ?: "")
        }
    }
    init { exo.addListener(listener) }

    override fun load(url: String, headers: Map<String, String>) {
        val item = MediaItem.Builder()
            .setUri(url)
            .setHeaders(HashMap(headers))
            .build()
        exo.setMediaItem(item)
        exo.prepare()
    }
    override fun start() = exo.play()
    override fun pause() = exo.pause()
    override fun seekTo(ms: Long) = exo.seekTo(ms)
    override fun setVolume(v: Float) { exo.volume = v }
    override fun setSpeed(m: Float) { exo.setPlaybackSpeed(m.coerceIn(0.5f, 3.0f)) }
    override fun setSubtitle(uriString: String?) { /* Media3 TextOverlay 接入 */ }
    override fun onStateChanged(cb: (PlaybackState) -> Unit): DisposableHandle {
        val obs = _state.collect { cb(it) }.let { /* 订阅 */ }
        return DisposableHandle { }
    }
    override fun dispose() { exo.release() }
}
