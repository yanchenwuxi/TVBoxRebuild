package com.rebuild.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rebuild.spider.Filter
import com.rebuild.spider.Spider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * M2 · 详情页 ViewModel：详情 + 分集 + 选中集 → 播放。
 * 数据走 Spider.detail(id)（QuickJS 源 / 内置解析），分集由 mac_url 拆出。
 */
sealed class DetailUiState {
    data object Loading : DetailUiState()
    data class Ready(val name: String, val content: String?, val actor: String?,
                     val score: String?, val episodes: List<com.rebuild.parse.EpisodeLite>,
                     val selectedEp: Int = 0) : DetailUiState()
    data class Error(val msg: String) : DetailUiState()
}

/** 轻量分集模型（详情屏用；完整 Episode 在 core-spider） */
data class EpisodeLite(val name: String, val url: String)

class DetailViewModel(
    private val spider: Spider,
    private val vodId: String
) : ViewModel() {

    private val _state = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val state: StateFlow<DetailUiState> = _state

    fun load() {
        viewModelScope.launch {
            try {
                val raw = spider.detail(vodId)
                val vod = com.rebuild.parse.VodJsonLite.parseDetail(raw)
                _state.value = DetailUiState.Ready(
                    name = vod.name, content = vod.content, actor = vod.actor,
                    score = vod.score, episodes = vod.episodes
                )
            } catch (e: Exception) {
                _state.value = DetailUiState.Error("详情加载失败: ${e.message}")
            }
        }
    }

    fun selectEpisode(idx: Int) {
        (_state.value as? DetailUiState.Ready)?.let { s ->
            _state.value = s.copy(selectedEp = idx)
        }
    }

    /** 取选中集的可播 URL（走 Spider.play()，M3 下带时效 token） */
    fun playUrl(): String? {
        val s = _state.value as? DetailUiState.Ready ?: return null
        return s.episodes.getOrNull(s.selectedEp)?.url
    }
}
