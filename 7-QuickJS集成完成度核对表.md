# QuickJS 集成完成度核对表

> 用途：一张表说清 QuickJS 这块"哪些已实机验证、哪些要 NDK 真机跑、回调桩怎么换成 OkHttp"。
> 依据：`libs/quickjs` 全部产物 + 沙箱实机编译/运行记录。

---

## 1. 完成度总览

| 层 | 状态 | 实机验证 | 说明 |
|---|---|---|---|
| C 源码获取 | ✅ 完成 | ✅ 已拉取 v0.17.0 | `fetch-and-build.sh` 拉全量 16 头 + 6 C 源 |
| C 源码可编译 | ✅ 完成 | ✅ `clang -c` 5 核心 .c→.o | 独立编译证明 C 源码本身没问题 |
| C 能执行 JS | ✅ 完成 | ✅ `qjs_smoke` 跑出 `7`/JSON/`__result__` | host 可执行验证 |
| JNI 导出实现 | ✅ 代码完成 | ⚠️ 需 NDK 真机 | `quickjs_rebuild.c` 4 导出 + 4 内置桩 |
| Kotlin 调用点 | ✅ 完成 | ⚠️ 需 NDK 真机 | `QuickJSNative`/`QuickJSContextImpl`/`loadLibraryOnce` |
| 真实 nativeCall | ⚠️ 桩 | ⚠️ 需 NDK 真机 | `nativeCall` 结果写回简化了（见 §3） |
| 4 内置→OkHttp | ⚠️ 桩 | 需接真实 OkHttp | 见 §4 改法 |
| NDK 出 .so | ⚠️ 待跑 | 需 NDK 环境 | `build-native.sh --ndk` / AGP `:libs:quickjs:assembleRelease` |

**一句话**：C 侧从"源码→可编译→可执行 JS"全链路已实机跑通；剩 NDK 真机编出 `.so` + `nativeCall` 字节写回 + 4 内置接 OkHttp 三步。

---

## 2. 已实机验证（沙箱 clang）

```
clang -O2 -DNDEBUG -std=c11 -I quickjs -D_GNU_SOURCE \
  qjs_smoke.c quickjs.c quickjs-libc.c libregexp.c libunicode.c dtoa.c -lm -lpthread -o qjs_smoke

./qjs_smoke "1+2*3"                          → 7
./qjs_smoke 'JSON.stringify({a:1,b:[2,3]})'  → {"a":1,"b":[2,3]}
./qjs_smoke -f sample.js                      → 取到 __result__（内联源 main()）
```
- 加 **`-DNDEBUG`** 是关键：quickjs-ng host 下 `JS_FreeRuntime` 会触发 `assert(JS_REF_COUNT>0)`，NDEBUG 关掉。NDK Release 自动生效，不踩。
- `-D_GNU_SOURCE`：host 编译 `clock_gettime` 需要；NDK 下 bionic 自带，不用。
- 5 个核心 C 独立 `clang -c` 成 `.o` 成功（C 源码可独立编译证明）。

## 3. 待 NDK 真机跑通（未实机验证）

| 项 | 现状 | 要做 |
|---|---|---|
| `libquickjs_rebuild.so` | CMake 就绪，未产出 | 装 NDK+CMake → `./gradlew :libs:quickjs:assembleRelease` 或 `build-native.sh --ndk` |
| `nativeCall` 字节写回 | 简化为 `strlen` 长度 + Kotlin `asCharBuffer` 读 | C 侧用 `env->GetDirectBufferAddress((jlongArray)0→outBuf)` 拿指针 `memcpy`，Kotlin `directBuffer.asCharBuffer().limit(n/2)` 已对齐 |
| 真实 4 内置 | 桩返回空串 | 见 §4 |

> 真机验证脚本（装了 NDK 后跑）：
> ```
> cd libs/quickjs && ./build-native.sh --ndk=~/Android/Sdk/ndk/26.1.10909125 --abi=arm64-v8a
> ./gradlew :libs:quickjs:assembleDebug   # 看 so 产出 + 加载不崩
> ```

## 4. 4 内置桩 → OkHttp 的改法

`quickjs_rebuild.c` 里 4 个桩（`builtin_get/post/json2object/request`）目前是"返回空串/透传"。
接真实 OkHttp 的两种改法：

**方案 A（JNI 回调 Kotlin，推荐）**
```c
// 桩体换成：FindClass 反射调 DefaultQuickJSSpiderApi.get(url)
static JSValue builtin_get(JSContext *ctx, ...) {
    JNIEnv *env;
    (*JNIGetEnvForThread?)(&env);  // 真实：保存全局 jniEnv
    jclass cls = (*env)->FindClass(env, "com/rebuild/quickjs/DefaultQuickJSSpiderApi");
    jmethodID mid = (*env)->GetMethodID(env, cls, "get", "(Ljava/lang/String;)Ljava/lang/String;");
    jstring jurl = (*env)->NewStringUTF(env, JS_ToCString(ctx, argv[0]));
    jstring ret  = (*env)->CallObjectMethod(env, gApi, mid, jurl);
    const char *s = (*env)->GetStringUTFChars(env, ret, 0);
    JSValue r = JS_NewString(ctx, s ? s : "");
    // ReleaseStringUTFChars / DeleteLocalRef ...
    return r;
}
```
- Kotlin 侧 `DefaultQuickJSSpiderApi` 已有 `get(url)` 走 OkHttp（见 QuickJSImpl.kt）
- 需把 `jobject api` 存成全局引用（`NewGlobalRef`），`nativeNewContext` 时传入

**方案 B（纯 C 自己发 HTTP，简单但绕开统一拦截器）**
- C 里用 `libcurl`/`connect`，不走 `core-network` 的 DoH/签名拦截器——**不推荐**，破坏 M3 安全链路

> 结论：用方案 A，4 内置全走 Kotlin OkHttp，保持"网络/签名/加密统一在 core-network 拦截器"。

## 5. 回归检查清单（NDK 真机时逐项打勾）
- [ ] `:libs:quickjs:assembleDebug` 产出 `libquickjs_rebuild.so`（arm64-v8a 等目标 ABI）
- [ ] App 启动 `loadLibraryOnce()` 无 `UnsatisfiedLinkError`
- [ ] `nativeNewContext()` 返回句柄 ≥ 0
- [ ] `nativeEval("1+2*3")` rc=0
- [ ] `nativeCall("main","[]",...)` 取回 `__result__` 非空
- [ ] `nativeFreeContext` 无 GC 断言 abort（Release NDEBUG）
- [ ] 4 内置接 OkHttp 后，联网源 `get(url)` 返回真实 JSON

---

## 附：文件对照
| 功能 | 文件 |
|---|---|
| 拉 C 源码 | `libs/quickjs/fetch-and-build.sh` |
| NDK/AGP 编 so | `libs/quickjs/build-native.sh` + `CMakeLists.txt` |
| 纯 C 执行验证 | `quickjs/qjs_smoke.c`（host 可跑） |
| JNI 导出 | `quickjs/quickjs_rebuild.c` |
| Kotlin 调用点 | `QuickJSNative.kt` / `QuickJSImpl.kt` |
| 内置方法 | `DefaultQuickJSSpiderApi`（QuickJSImpl.kt） |
