package com.rebuild.setting

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rebuild.source.SourceManager
import com.rebuild.source.VideoSource

/**
 * M2 · 设置屏（feature-settings 的 3 个子页入口 + 源管理设置）。
 * 源管理：SourceManager 的导入/导出/启用/健康检测。
 * 播放设置：画质/倍速/手势（接 core-player）。
 * 安全设置：加密开关（接 core-security/EncryptedApiInterceptor）。
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("设置", style = MaterialTheme.typography.headlineSmall)
        Spacer(12.dp)
        SettingsItem("源管理") { SourceSettings(onBack) }
        SettingsItem("播放设置") { PlaySettings() }
        SettingsItem("安全与加密") { SecuritySettings() }
        SettingsItem("关于") { AboutSettings() }
    }
}

@Composable
private fun SettingsItem(title: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = { Text("•") },
        trailingContent = { Text("›") },
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    )
}

// ---- 源管理设置（接 core-source SourceManager）----
@Composable
fun SourceSettings(onBack: () -> Unit) {
    val sm = remember { SourceManager() }
    var sources by remember { mutableStateOf<List<VideoSource>>(emptyList()) }

    Column(Modifier.fillMaxSize().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("源管理（${sources.size} 个）", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { /* 导入源 JSON：sm.importSource(text) */ }) { Text("导入") }
            Button(onClick = { /* 导出：sm.exportSources(sources) */ }) { Text("导出") }
            OutlinedButton(onClick = {
                // 健康检测（stub runner 演示）
                sources.forEach { sm.probeHealth(it) { it.content } }
            }) { Text("健康检测") }
        }
        Text("提示：导入自有/授权源（CONFIG/JS 型）。合规红线：不接盗版源。",
            style = MaterialTheme.typography.bodySmall)
    }
}

// ---- 播放设置（接 core-player）----
@Composable
fun PlaySettings() {
    var quality by remember { mutableStateOf("自动") }
    var gesture by remember { mutableStateOf(true) }
    Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("画质：$quality", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("自动", "1080P", "720P", "原画").forEach {
                FilterChip(selected = quality == it, onClick = { quality = it },
                    label = { Text(it) })
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Switch(checked = gesture, onCheckedChange = { gesture = it },
                label = { Text("手势控制（音量/亮度/倍速）") })
        }
    }
}

// ---- 安全与加密（接 core-security）----
@Composable
fun SecuritySettings() {
    var enc by remember { mutableStateOf(true) }
    Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Switch(checked = enc, onCheckedChange = { enc = it },
                label = { Text("请求加密（M3：签名 + AES-GCM）") })
        }
        Text("密钥由服务端 ECDH 握手下发，客户端不持有明文主密钥。",
            style = MaterialTheme.typography.bodySmall)
    }
}

// ---- 关于 ----
@Composable
fun AboutSettings() {
    Column(Modifier.padding(8.dp)) {
        Text("TVBoxRebuild · 纯自研", style = MaterialTheme.typography.titleMedium)
        Text("版本 0.1.0\n合规：自有/授权内容源，0 复用某商业闭源影音App so/资源/端点",
            style = MaterialTheme.typography.bodySmall)
    }
}
