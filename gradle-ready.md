## 多模块 Gradle 编译就绪 · 交付清单

本目录已补全全部 11 个模块的 `build.gradle.kts` + 根/全局配置，`git init` 后可直接
`./gradlew :app:assembleDebug` 起步。版本目录 `gradle/libs.versions.toml` 与所有模块的
`libs.*` 别名已对齐（Kotlin DSL 的 `-`→`.` 规则）。

### 全量 gradle 文件（15 个）
| 文件 | 作用 |
|---|---|
| `settings.gradle.kts` | 多模块 include + 仓库 + KSP 插件版本解析 |
| `build.gradle.kts` | 根插件声明（AGP/Kotlin/Compose/Hilt/KSP，均 apply false） |
| `gradle/libs.versions.toml` | 单一版本目录（Compose BOM/Media3/OkHttp/Room/Gson/Sentry…） |
| `gradle.properties` | 全局 JVM/AndroidX/ABI 配置 |
| `app/build.gradle.kts` | 壳：Compose + 全 feature/core 依赖 + Coil + Sentry |
| `core-player/build.gradle.kts` | 播放：Media3（exo/ui/datasource） |
| `core-spider/build.gradle.kts` | 爬虫：QuickJS + core-network + Gson + OkHttp |
| `core-source/build.gradle.kts` | 源管理：Gson + core-network（健康检测） |
| `core-network/build.gradle.kts` | 网络：OkHttp/DoH/Retrofit/Gson |
| `core-local/build.gradle.kts` | 本地：Room(KSP) + MediaStore |
| `feature-home/build.gradle.kts` | 首页 UI：Compose + core-spider/core-local |
| `feature-detail/build.gradle.kts` | 详情 UI：Compose + core-spider/core-player |
| `feature-live/build.gradle.kts` | 直播 UI（占位）：Compose + core-player |
| `feature-settings/build.gradle.kts` | 设置 UI（占位）：Compose + core-source |
| `libs/quickjs/build.gradle.kts` | QuickJS：NDK/CMake 编 Bellard QuickJS + JNI 包装 |

### 已修的正确性要点
- `core-local` 的 Room 注解处理器：`annotationProcessor` → **KSP**（AGP8 下避免 room-compiler 兼容坑），
  根 + settings 声明 `com.google.devtools.ksp 2.0.20-1.0.25`
- 版本目录别名统一 Kotlin DSL 规则：`androidx-ui`→`libs.androidx.ui`、
  `material3`→`libs.material3`、`media3-datasource`→`libs.media3.datasource`（逐一核对过）
- 模块依赖方向单向无环：feature-* → app；feature-* → core-*；core-spider → libs:quickjs + core-network

### 仍需外部动作（非代码缺陷，集成时做）
1. **Bellard QuickJS C 源码**：把 `quickjs.c/quickjs-libc.c/quickjs.h`（MIT）放进
   `libs/quickjs/src/main/jni/quickjs/`，CMakeLists 才会编出 `libquickjs_rebuild.so`
2. **NDK + CMake 3.22**：Android Studio 装 NDK/LLVM + CMake
3. **Room/KSP 首次构建** 需联网拉依赖（google() + mavenCentral()）
4. **内容源**：接自有/授权源，不接某商业闭源影音App `<competitor-endpoint>` 端点

### 起步命令
```
cd 5-纯自研工程方案
git init
./gradlew :app:assembleDebug   # 首次拉依赖 + NDK 编 QuickJS
```
