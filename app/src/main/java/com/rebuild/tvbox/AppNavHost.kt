package com.rebuild.tvbox

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** M0 占位：5 页面 NavHost 骨架，M2 再填 UI */
@Composable
fun AppNavHost(padding: androidx.compose.foundation.layout.PaddingValues) {
    // MVP 先放单页占位，M2 接入 Navigation-Compose 的 5 目的地
    Column(modifier = Modifier.padding(padding)) {
        Text("TVBoxRebuild · M0 脚手架")
        Text("Home / Detail / Play / Search / Setting 占位，等待 feature-* 模块")
    }
}
