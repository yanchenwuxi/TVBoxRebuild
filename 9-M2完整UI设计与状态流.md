# M2 · 完整 UI 设计 + 状态流（接 core-spider 真数据）

> 4 屏（Home / Detail / Play / Setting）的 Compose 布局 + 单向数据流（ViewModel → StateFlow → Compose），
> 数据全部走 `core-spider` 的 13 接口（QuickJSSpider + RuleBasedParser），不碰某商业闭源影音App端点。

---

## 1. 架构：单向数据流

```
UI(Compose) ──intent──▶ ViewModel ──▶ Repository(Spider) ──▶ Spider(13接口)
   ▲                        │ StateFlow
   └──observe──────────────┘
```

- **Intent**：`onSearch / onDetail / onPlay / onSelectSource / onNextPage`
- **State**：`UiState = sealed`（`Loading / Success(data) / Error(msg)`），`StateFlow<UiState>` 驱动重组
- **Repository**：把 Spider 返回的 JSON 解析成 data class（`VodSummary` / `VodDetail` / `Episode`），缓存页
- 网络在 Spider 的 `get()`（OkHttp，挂 `EncryptedApiInterceptor` + DoH）

## 2. 数据模型（与 Spider 13 接口 VOD 字段对齐）

```kotlin
// feature 层 UI 模型（从 Spider JSON 映射）
data class VodSummary(
    val vodId: String, val name: String, val year: String?,
    val area: String?, val cls: String?, val remarks: String?, val pic: String?
)
data class VodDetail(
    val summary: VodSummary,
    val content: String?, val actor: String?, val director: String?, val score: String?,
    val episodes: List<Episode>          // 由 parseMacUrl(vod_play_url) 拆出
)
```

## 3. 四屏布局

### 3.1 Home（首页）
```
┌ Scaffold ┐
│ TopBar: [搜索框 ______] [🔍] [源▾]      │  ← 搜索框(Enter→onSearch) + 源切换
│ TabRow: [电影|连续剧|综艺|动漫]          │  ← 分类 Tab（Spider.home().class）
│ LazyColumn:                              │
│   VodCard(封面│名称/地区/年份/更新)       │  ← 网格/列表可切（Spider.homeVod()）
│   ...分页(底部触底加载更多)               │
└──────────────────────────────────────────┘
```
- 状态：`categories: List<SourceCategory>`、`vods: List<VodSummary>`、`page/pages`
- 交互：切 Tab → `onCategory(type)`；触底 → `onNextPage()`；搜索 → 切 SearchFragment

### 3.2 Search（搜索，合并进 Home 或独立屏）
```
[搜索框(自动聚焦)]
[历史搜索 chips]
LazyColumn 搜索结果（复用 VodCard）
```
- `onSearch(kw)` → `Spider.search(kw,page,filter)`

### 3.3 Detail（详情）
```
[Banner 封面 + 名称/年份/地区/评分/简介]
[分集横向 LazyRow: [第01集][第02集]...  ← 选中态]
[播放按钮(选中的集) → Play 屏]
```
- 状态：`detail: VodDetail`（含 `episodes`）
- `onPlay(ep)` → 进 Play 屏

### 3.4 Play（播放）
```
[PlayerView(Media3/ijk) 全屏/竖屏]
[底部控件: ⏮ 进度 ⏭ │ 手势区]
[字幕/画质/倍速(长按) — 见 core-player 手势]
[下一集(自动连播) ]
```
- 状态：`episode`、`position/duration`、`subtitles`
- `Player.load(url, headers)`；进度回写 `core-local` 的 PlayProgress

## 4. ViewModel 骨架（feature-home）

```kotlin
class HomeViewModel(private val spider: Spider, private val parser: RuleBasedParser) : ViewModel() {
    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state

    fun onCategory(type: Int) {
        viewModelScope.launch {
            val raw = spider.category(type, 1, Filter(), emptyMap())
            val vods = parser.parseList(raw)            // JSON → List<VodSummary>
            _state.value = UiState.Success(vods)
        }
    }
    fun onSearch(kw: String) {
        viewModelScope.launch {
            val raw = spider.search(kw, 1, Filter())
            _state.value = UiState.Success(parser.parseList(raw))
        }
    }
}

sealed class UiState {
    data object Loading : UiState()
    data class Success(val vods: List<VodSummary>) : UiState()
    data class Error(val msg: String) : UiState()
}
```

## 5. Compose 接真数据（feature-home/HomeScreen.kt 改造要点）
- `collectAsStateWithLifecycle(state)` 读 `UiState`
- `Loading` → `CircularProgressIndicator`
- `Success` → `ScrollableTabRow` + `LazyColumn(items(vods))`
- 封面：`Coil AsyncImage(pic)`（MVP 占位 🎬）
- 触底：`LazyListState` 监听末项 → `onNextPage()`
- 源切换：`SourceManager.allEnabled(repos)` 下拉

## 6. 各模块落点
| 屏 | 模块 | 文件 |
|---|---|---|
| Home/Search | feature-home | `HomeScreen.kt`（已有）+ 本文 §4 VM |
| Detail | feature-detail | `DetailScreen.kt`（已有）+ VM |
| Play | feature-live / app | `Player`（core-player）+ PlayScreen |
| Setting | feature-settings | 源管理/播放设置（接 SourceManager + DoH + 加密开关） |

## 7. 合规再确认
- 数据源走 Spider（自有/授权源，QuickJS 联网）；**不接某商业闭源影音App <competitor-endpoint> 端点**
- 请求经 `EncryptedApiInterceptor`（签名 + 可选 AES-GCM），隐私最小化
- 本地进度/收藏存 `core-local`（Room/SQLCipher）
