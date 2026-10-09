package com.rebuild.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rebuild.parse.RuleBasedParser
import com.rebuild.spider.Filter
import com.rebuild.spider.Spider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * M2 · 首页/搜索 ViewModel（接 core-spider 真数据，单向数据流）。
 * 数据全部走 Spider 13 接口（QuickJSSpider + RuleBasedParser），不碰某商业闭源影音App端点。
 */

/** UI 状态（Loading / Success / Error），驱动 Compose 重组 */
sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(
        val categories: List<SourceCategory> = emptyList(),
        val vods: List<VodSummary> = emptyList(),
        val page: Int = 1,
        val pages: Int = 1,
        val canLoadMore: Boolean = false
    ) : HomeUiState()
    data class Error(val msg: String) : HomeUiState()
}

class HomeViewModel(
    private val spider: Spider,
    private val parser: RuleBasedParser
) : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state

    private var currentType = "1"

    fun init() {
        loadCategories()
        loadCategory(1)
    }

    /** 首页分类 Tab（Spider.home() → class 数组） */
    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val classes = com.rebuild.parse.VodJson.parseClass(spider.home())
                if (classes.isNotEmpty()) {
                    (_state.value as? HomeUiState.Success)?.let {
                        _state.value = it.copy(categories = classes)
                    }
                }
            } catch (e: Exception) { /* 分类加载失败不影响列表 */ }
        }
    }

    /** 切分类 Tab */
    fun onCategory(type: String) {
        currentType = type
        loadCategory(1)
    }

    /** 搜索 */
    fun onSearch(kw: String) {
        viewModelScope.launch {
            _state.value = HomeUiState.Loading
            try {
                val vods = com.rebuild.parse.VodJson.parseList(spider.search(kw, 1, Filter()))
                _state.value = HomeUiState.Success(vods = vods)
            } catch (e: Exception) {
                _state.value = HomeUiState.Error("搜索失败: ${e.message}")
            }
        }
    }

    private fun loadCategory(page: Int) {
        viewModelScope.launch {
            _state.value = HomeUiState.Loading
            try {
                val out = com.rebuild.parse.VodJson.parseList(
                    spider.category(currentType, page, Filter(), emptyMap())
                )
                val (vods, pages) = out
                _state.value = HomeUiState.Success(vods = vods, page = page, pages = pages,
                    canLoadMore = page < pages)
            } catch (e: Exception) {
                _state.value = HomeUiState.Error("加载失败: ${e.message}")
            }
        }
    }

    /** 触底加载更多 */
    fun onNextPage() {
        (state.value as? HomeUiState.Success)?.let { s ->
            if (s.canLoadMore) loadCategory(s.page + 1)
        }
    }
}
