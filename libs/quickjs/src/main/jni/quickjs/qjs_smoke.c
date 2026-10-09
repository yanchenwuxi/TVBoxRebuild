// qjs_smoke.c —— 纯 C 最小验证工具（证明拉到的 Bellard QuickJS C 源码能编译 + 能跑 JS）
// quickjs-ng v0.17.0 的 API 前缀是 JS_（类型是 JSValue / JSAtom，不是 QuickJS*）。
// 用法：
//   ./qjs_smoke "1+2*3"            # 跑表达式
//   ./qjs_smoke -f sample.js        # 跑脚本，打印 __result__（若定义）
// 编译（host，见 build-native.sh 的 host 段）：
//   clang -O2 -std=c11 -I quickjs -D_GNU_SOURCE \
//     quickjs/qjs_smoke.c quickjs/quickjs.c quickjs/quickjs-libc.c \
//     quickjs/libregexp.c quickjs/libunicode.c quickjs/dtoa.c -lm -lpthread -o qjs_smoke
// 注意：v0.17 在 host -O2 下 GC 有已知断言问题；Android NDK Release 下不复现，
// 或加 -O0 / 定义 JS_NAN_BOXING=1 绕过。
#include "quickjs.h"
#include "cutils.h"
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

// 桩内置 get()：返回空串（真实 Android 走 JNI 回调 Kotlin 的 OkHttp）
static JSValue stub_get(JSContext *ctx, JSValueConst this_val, int argc, JSValueConst *argv) {
    (void)this_val; (void)argc; (void)argv;
    return JS_NewString(ctx, "");
}

static void dumpException(JSContext *cx) {
    JSValue e = JS_GetException(cx);
    JSValue s = JS_ToString(cx, e);
    const char *msg = JS_ToCString(cx, s);
    printf("JS exception: %s\n", msg ? msg : "(null)");
    JS_FreeCString(cx, msg);
    JS_FreeValue(cx, s);
    JS_FreeValue(cx, e);
}

static void show(JSContext *cx, JSValue v) {
    JSValue s = JS_ToString(cx, v);
    const char *res = JS_ToCString(cx, s);
    printf("%s\n", res ? res : "(undefined)");
    JS_FreeCString(cx, res);
    JS_FreeValue(cx, s);
}

int main(int argc, char **argv) {
    if (argc < 2) { printf("usage: %s <expr>  |  %s -f <file>\n", argv[0], argv[0]); return 2; }

    JSRuntime *rt = JS_NewRuntime();
    JSContext *cx = JS_NewContext(rt);
    // 暴露桩 get()：用 JS_NewAtom 把 "get" 转成 JSAtom
    JSValue g = JS_GetGlobalObject(cx);
    JSValue gf = JS_NewCFunction(cx, stub_get, "get", 1);
    JSAtom ga = JS_NewAtom(cx, "get");
    JS_SetProperty(cx, g, ga, gf);
    JS_FreeAtom(cx, ga);

    int rc = 0;
    if (strcmp(argv[1], "-f") == 0) {
        FILE *f = fopen(argv[2], "rb");
        if (!f) { printf("open %s fail\n", argv[2]); rc = 2; goto cleanup; }
        char buf[8192]; int n = (int)fread(buf, 1, sizeof(buf) - 1, f); buf[n] = 0;
        JSValue v = JS_Eval(cx, buf, n, "source.js", 0 /* global */);
        if (JS_HasException(cx)) { dumpException(cx); rc = 1; }
        else {
            JSValue r = JS_GetPropertyStr(cx, g, "__result__");
            if (!JS_IsUndefined(r)) { printf("== __result__ ==\n"); show(cx, r); JS_FreeValue(cx, r); }
            JS_FreeValue(cx, v);
        }
    } else {
        JSValue v = JS_Eval(cx, argv[1], strlen(argv[1]), "<expr>", 0);
        if (JS_HasException(cx)) { dumpException(cx); rc = 1; }
        else { printf("== result ==\n"); show(cx, v); JS_FreeValue(cx, v); }
    }
cleanup:
    JS_FreeValue(cx, g); JS_FreeValue(cx, gf);
    JS_FreeContext(cx);
    JS_FreeRuntime(rt);
    return rc;
}
