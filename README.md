# TVBoxRebuild · 纯自研 TVBox 类手机影音 App

> 基于某商业闭源影音App 3.3.8 反编译产物做**架构参考**，**纯自研**重建一个可上架的 TVBox 类手机影音 App。
> 合规红线：**0 处复用某商业闭源影音App so/资源/端点**（闭源 + 私有 `<competitor-endpoint>` + Dex2C 加固），
> 不 fork 无 license 开源壳，内容源走**自有/授权**，个人信息最小化。

本目录 = 可直接 `git init` 起步的完整工程 + 10 份设计文档。

---

## 从这里开始（阅读动线）

```
你在这里：README（总入口）
  │
  ├─ 想先懂"为什么/能不能做"  →  TVBox重建方案白皮书.md
  ├─ 想选"用什么开源件"        →  1-模块对照选型表.md
  ├─ 想写爬虫源                →  2-TVBox源协议规范.md
  ├─ 想看工时/排期             →  4-MVP工程拆解与工时.md
  └─ 要动手（代码在这里）       →  下面的工程目录 + 9-M2完整UI设计与状态流.md
```

## 工程结构（多模块）

```
TVBoxRebuild/
├─ app/                    Compose 壳（MainActivity + 5 占位页）
├─ core-player/            播放器封装（Media3Player，ijk 可切）
├─ core-spider/            爬虫引擎（13 接口 Spider + QuickJS 桥 + 内置解析）
├─ core-source/            源管理（导入导出/多仓/健康检测）
├─ core-network/           OkHttp/Retrofit + DoH + 服务端加密拦截器
├─ core-security/          安全（ECDH 握手/源白名单/时效 token/Keystore）
├─ core-local/             本地（SAF 片单 + Room/SQLCipher）
├─ feature-home/detail/    首页/搜索、详情/分集（Compose + ViewModel）
├─ feature-live/           播放屏（接 core-player）
├─ feature-settings/       设置（源管理/播放/加密 3 屏）
└─ libs/quickjs/           真实 QuickJS（Bellard quickjs-ng v0.17.0，MIT）
```

**模块依赖单向无环**：feature-* → app；feature-* → core-*；core-spider → libs:quickjs + core-network。

## 里程碑（M0→M3 已落盘）

| 里程碑 | 内容 | 状态 |
|---|---|---|
| M0 | 脚手架 + 5 占位页 + Media3 播放 + DoH | ✅ 代码就绪 |
| M1 | 13 接口 Spider + QuickJS 真实 C 源码 + 内置解析 + 源管理 | ✅ 代码就绪（QuickJS C 源码已实机跑通 JS） |
| M2 | 手机 UI 全量（首页/搜索/详情/播放/设置）+ 源管理 | ✅ 代码就绪 |
| M3 | 服务端加密 + 源白名单 + 商用加固 | ✅ 设计 + 伪代码就绪 |

## 快速起步（本机 3 步）

```sh
# 1. 初始化 git（生成 wrapper + 首提交）
./init-repo.sh

# 2. 拉 QuickJS C 源码（已含 .gitignore 例外，不会误删）
./libs/quickjs/fetch-and-build.sh --src-only

# 3. 编 QuickJS so（需装 NDK + CMake）
./libs/quickjs/build-native.sh --ndk="$ANDROID_NDK_HOME" --abi=arm64-v8a
# 然后
./gradlew :app:assembleDebug
```

## 设计文档索引（10 份）

| 文档 | 作用 |
|---|---|
| `TVBox重建方案白皮书.md` | 总纲：可行性 + 红线 + 路径 |
| `1-模块对照选型表.md` | 49 个 so → 开源等价选型 |
| `2-TVBox源协议规范.md` | 13 接口 Spider + VOD 字段 + mac_url 格式 |
| `4-MVP工程拆解与工时.md` | 工时 + 里程碑 + 合规清单 |
| `5-纯自研方案.md` | 纯自研全方案（模块/UI/爬虫/安全） |
| `6-服务端加密设计.md` | ECDH + 源白名单 + 时效 token |
| `7-QuickJS集成完成度核对表.md` | 哪些已实机验证 / 哪些要 NDK 真机 |
| `8-M3服务端可落地伪代码.md` | /resolve + 签名 + SQLCipher 参考实现 |
| `9-M2完整UI设计与状态流.md` | 4 屏 Compose + 单向数据流 |
| `10-上线合规Checklist.md` | 内容/个人信息/广告/渠道 逐项打勾 |

## 合规红线（随时自检）

```sh
# 任何时候应为 0（无某商业闭源影音App残留）
grep -rn "<competitor-endpoint>" .          # 0
grep -rln "libconceal\|libnc\.so\|native salt 函数\|dex2c" .  # 0
```
- 内容源：自有/授权，**不接盗版**（某商业闭源影音App `pan.<competitor-endpoint>/direct/azg/*.txt` 远程下发盗版源列表的模式禁止）
- 密钥：服务端 ECDH 下发，**不硬编码 salt**
- 端点：服务端按签名下发 + AES-GCM + 时效 token，**不下发明文源 URL**

## 实机已验证（QuickJS C 侧）

```
./qjs_smoke "1+2*3"                         → 7
./qjs_smoke 'JSON.stringify({a:1,b:[2,3]})' → {"a":1,"b":[2,3]}
```
（Bellard quickjs-ng v0.17.0，`-DNDEBUG` 关 host GC 断言；NDK Release 自动生效。详见 7-核对表。）

## 风险
1. **内容源合规**：最大雷区，务必自有/授权，不聚合盗版
2. **NDK 真机**：`libquickjs_rebuild.so` + nativeCall 字节写回 + 4 内置接 OkHttp，需装 NDK 后跑一次（核对表 §5 有回归清单）
3. **第三方 SDK**：广告/统计逐家合规申请，不照搬某商业闭源影音App配置
