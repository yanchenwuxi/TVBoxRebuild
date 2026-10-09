package com.rebuild.home

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * M2 · 详情页（分集列表 + 播放入口）。数据来自 Spider.detail(id)。
 */
@Composable
fun DetailScreen(
    vodName: String, vodContent: String?, vodActor: String?,
    episodes: List<com.rebuild.spider.Episode>,
    onPlay: (com.rebuild.spider.Episode) -> Unit
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item {
            Text(vodName, style = MaterialTheme.typography.headlineSmall)
            Spacer(4.dp)
            vodContent?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            vodActor?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Spacer(12.dp)
            Text("分集（${episodes.size}）", style = MaterialTheme.typography.titleMedium)
        }
        items(episodes) { ep ->
            ListItem(
                headlineContent = { Text(ep.name) },
                supportingContent = { Text(ep.url, style = MaterialTheme.typography.bodySmall) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(MaterialTheme.shapes.small)
            )
            // 简化：列表项可点播放（真实用 Button onClick = onPlay(ep)）
            Spacer(4.dp)
        }
    }
}
