# quickjs_rebuild.c —— Bellard QuickJS（quickjs-ng v0.17.0）的 Android JNI 包装
// 产物：libquickjs_rebuild.so
//
// 导出给 Kotlin（见 QuickJSNative.kt 的 @JvmStatic external 声明，包名 com.rebuild.quickjs.native）：
//   nativeNewContext / nativeFreeContext / nativeEval / nativeCall
//
// 4 个 JS 内置方法（get/post/json2object/request）在 C 侧注册为全局函数。
// 桩实现：get/post 返回空串，json2object 透传（真实 Android 经 JNI 回调 Kotlin 的 OkHttp）。
// 真实集成时把桩体换成 GetEnv->FindClass 反射调 DefaultQuickJSSpiderApi。
//
// 合规：Bellard quickjs-ng（MIT），不复用某商业闭源影音App libquickjs-android-wrapper.so。
#include <jni.h>
#include <string.h>
#include <stdlib.h>
#include "quickjs.h"
#include "cutils.h"

// ---- 上下文包装 ----
typedef struct {
    JSContext  *cx;
    JSRuntime  *rt;
} CtxWrap;

// ---- 4 个内置桩 ----
static JSValue builtin_get(JSContext *ctx, JSValueConst this_val, int argc, JSValueConst *argv) {
    (void)this_val; (void)argv;
    // 真实 Android：拼请求发 Kotlin OkHttp；桩：返回空串
    return JS_NewString(ctx, "");
}
static JSValue builtin_post(JSContext *ctx, JSValueConst this_val, int argc, JSValueConst *argv) {
    (void)this_val; (void)argc; (void)argv;
    return JS_NewString(ctx, "");
}
static JSValue builtin_json2object(JSContext *ctx, JSValueConst this_val, int argc, JSValueConst *argv) {
    (void)this_val;
    return (argc >= 1) ? JS_DupValue(ctx, argv[0]) : JS_NewString(ctx, "{}");
}
static JSValue builtin_request(JSContext *ctx, JSValueConst this_val, int argc, JSValueConst *argv) {
    (void)this_val; (void)argc; (void)argv;
    return JS_NewString(ctx, "");
}

// ---- JNI 导出（符号名对应 Java_com_rebuild_quickjs_native_QuickJSNative_*）----

JNIEXPORT jint JNICALL
Java_com_rebuild_quickjs_native_QuickJSNative_nativeNewContext(JNIEnv *env, jclass) {
    JSRuntime *rt = JS_NewRuntime();
    JSContext *cx = JS_NewContext(rt);
    CtxWrap *w = (CtxWrap *)malloc(sizeof(CtxWrap));
    w->cx = cx; w->rt = rt;
    JS_SetContextOpaque(cx, w);

    // 注册 4 个内置为 JS 全局
    JSValue g = JS_GetGlobalObject(cx);
    JSValue fns[4];
    const char *names[4] = { "get", "post", "json2object", "request" };
    JSCFunction *fcs[4] = { builtin_get, builtin_post, builtin_json2object, builtin_request };
    int lens[4] = { 1, 1, 1, 3 };
    for (int i = 0; i < 4; i++) {
        fns[i] = JS_NewCFunction(ctx, fcs[i], names[i], lens[i]);
        JSAtom a = JS_NewAtom(ctx, names[i]);
        JS_SetProperty(cx, g, a, fns[i]);
        JS_FreeAtom(ctx, a);
    }
    JS_FreeValue(ctx, g);
    for (int i = 0; i < 4; i++) JS_FreeValue(ctx, fns[i]);

    return (jint)(intptr_t)cx;
}

JNIEXPORT void JNICALL
Java_com_rebuild_quickjs_native_QuickJSNative_nativeFreeContext(JNIEnv *, jclass, jint h) {
    JSContext *cx = (JSContext *)(intptr_t)h;
    CtxWrap *w = (CtxWrap *)JS_GetContextOpaque(cx);
    JS_FreeContext(cx);
    JS_FreeRuntime(w->rt);
    free(w);
}

JNIEXPORT jint JNICALL
Java_com_rebuild_quickjs_native_QuickJSNative_nativeEval(JNIEnv *env, jclass, jint h,
                                                         jstring code, jstring filename) {
    JSContext *cx = (JSContext *)(intptr_t)h;
    const char *c = env->GetStringUTFChars(code, NULL);
    const char *f = env->GetStringUTFChars(filename, NULL);
    JSValue v = JS_Eval(cx, c, strlen(c), f, 0 /* global */);
    int rc = JS_HasException(cx) ? 1 : 0;
    JS_FreeValue(cx, v);
    env->ReleaseStringUTFChars(code, c);
    env->ReleaseStringUTFChars(filename, f);
    return rc;
}

// argsJson：Kotlin 侧拼好的参数 JSON（MVP 按 0 参调用；后续接 JSON C 库解析）
JNIEXPORT jint JNICALL
Java_com_rebuild_quickjs_native_QuickJSNative_nativeCall(JNIEnv *env, jclass, jint h,
                                                          jstring fn, jstring argsJson,
                                                          jobject outBuf, jint outCap) {
    JSContext *cx = (JSContext *)(intptr_t)h;
    const char *f = env->GetStringUTFChars(fn, NULL);
    const char *aj = env->GetStringUTFChars(argsJson, NULL);
    (void)aj;

    JSValue global = JS_GetGlobalObject(cx);
    JSValue fnv = JS_GetPropertyStr(cx, global, f);
    JSValue ret = JS_Call(cx, fnv, JS_UNDEFINED, 0, NULL);
    JS_FreeValue(cx, fnv);

    int rc = -1;
    if (!JS_IsUndefined(ret)) {
        JSValue sv = JS_ToString(cx, ret);
        const char *s = JS_ToCString(cx, sv);
        if (s && s[0]) {
            // 真实：jbyteArray / ByteBuffer GetDirectBufferAddress 写回
            // MVP：返回长度（Kotlin 侧用 asCharBuffer 读，见 QuickJSContextImpl.invoke）
            rc = (int)strlen(s);
            JS_FreeCString(cx, s);
        }
        JS_FreeValue(cx, sv);
    }
    JS_FreeValue(cx, ret);
    JS_FreeValue(cx, global);
    env->ReleaseStringUTFChars(fn, f);
    env->ReleaseStringUTFChars(argsJson, aj);
    return rc;
}
