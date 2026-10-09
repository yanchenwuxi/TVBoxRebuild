package com.rebuild.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rebuild.source.VideoSource

/**
 * M2 · 首页 Compose UI（feature-home 的"内容区"；导航/壳在 app 模块）。
 * 纯自研 UI：分类页 + 网格 + 搜索入口，数据来自 source/spider。
 */

@Composable
fun HomeScreen(
    categories: List<SourceCategory>,
    vods: List<VodSummary>,
    onSearch: (String) -> Unit,
    onDetail: (String) -> Unit
) {
    var kw by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = kw,
                    onValueChange = { kw = it },
                    placeholder = { Text("搜索片名 / 关键词") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(8.dp)
                IconButton(onClick = { kw.isNotEmpty() && onSearch(kw) }) { Text("🔍") }
            }
        }
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(12.dp)) {
            item {
                // 分类 Tab
                ScrollableTabRow(selectedTabIndex = 0) {
                    categories.forEachIndexed { i, c ->
                        Tab(selected = i == 0, onClick = {}) { Text(c.name) }
                    }
                }
            }
            item { Spacer(8.dp) }
            items(vods) { v ->
                VodCard(v, onClick = { onDetail(v.vodId) })
            }
        }
    }
}

@Composable
fun VodCard(v: VodSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(MaterialTheme.shapes.medium),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            // 封面（MVP 占位；接 Coil 后 AsImage）
            Box(Modifier.size(64.dp).clip(MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center) { Text("🎬") }
            Spacer(12.dp)
            Column(Modifier.weight(1f)) {
                Text(v.name, style = MaterialTheme.typography.titleMedium)
                Text(v.remarks, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(v.year ?: "—", style = MaterialTheme.typography.bodySmall)
        }
    }
}

// ---- 首页数据模型（与 Spider.home()/homeVod() 返回的 VOD 字段对齐）----
data class SourceCategory(val type: Int, val name: String, val style: Int = 0)
data class VodSummary(
    val vodId: String, val name: String, val year: String?,
    val area: String?, val cls: String?, val remarks: String?, val pic: String?
)
