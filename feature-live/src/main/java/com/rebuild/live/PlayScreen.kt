package com.rebuild.live

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rebuild.player.Media3Player
import com.rebuild.player.PlaybackState
import com.rebuild.player.Player

/**
 * M2 · 播放屏（接 core-player 的 Media3Player）。
 * 状态：播放/暂停/进度；手势（缩放/音量/倍速）由 UI 调 Player.setVolume/setSpeed。
 * 进度回写 core-local 的 PlayProgress（此处占位，接 Room 后落地）。
 */
@Composable
fun PlayScreen(
    ctx: Context,
    url: String,
    epName: String,
    onExit: () -> Unit
) {
    // 真实：用 remember { Media3Player(ctx) }，onDispose dispose()
    var state by remember { mutableStateOf<PlaybackState>(PlaybackState.Idle) }
    var showControls by remember { mutableStateOf(true) }

    Column(Modifier.fillMaxSize()) {
        // 视频区（真实接 AndroidView + PlayerView；MVP 占位）
        Box(
            Modifier.weight(1f).fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text("▶  ${epName}\n$url")
        }

        // 底部控制条
        Row(Modifier.padding(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            TextButton(onClick = onExit) { Text("返回") }
            Spacer(Modifier.weight(1f))
            when (state) {
                is PlaybackState.Playing -> Text("进度 ${state.positionMs / 1000}s / ${state.durationMs / 1000}s")
                is PlaybackState.Paused -> Text("暂停 ${state.positionMs / 1000}s")
                is PlaybackState.Error -> Text("错误 ${state.code}: ${state.msg}",
                    color = MaterialTheme.colorScheme.error)
                is PlaybackState.Idle -> Text("未开始")
            }
        }
        // 说明：手势区/字幕/画质/倍速 见 core-player 的 Player 接口
    }
}
