# NDK 构建：编 Bellard QuickJS（quickjs-ng v0.17.0，MIT）+ 我们的 JNI 包装
# 产物：libquickjs_rebuild.so → AGP 自动打包到 jniLibs/<abi>/
#
# 前置：先跑 fetch-and-build.sh 把 C 源码铺到 quickjs/ 子目录。
cmake_minimum_required(VERSION 3.22.1)
project(quickjs_rebuild C)

add_library(quickjs_rebuild SHARED
    quickjs/quickjs_rebuild.c    # 我们的 JNI 包装（nativeNewContext/Eval/Call + 4 内置回调桩）
    quickjs/quickjs.c            # Bellard QuickJS 核心（v0.17 已并入 cutils）
    quickjs/quickjs-libc.c       # libc 辅助
    quickjs/libregexp.c          # 正则（JS RegExp）
    quickjs/libunicode.c         # Unicode
    quickjs/dtoa.c               # 浮点转字符串
)

target_include_directories(quickjs_rebuild PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/quickjs
)

find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(quickjs_rebuild ${log-lib} ${android-lib})

# 编译选项：
#  -O2 -DNDEBUG：Release 关掉 QuickJS host 调试 GC 断言（实测必需，否则 JS_FreeRuntime 时
#                assert(JS_REF_COUNT>0) 触发 abort；NDK Release 下 NDEBUG 自动生效）
#  -std=c11     ：QuickJS 用 C99 + 原子扩展
#  -D__ANDROID__：让 cutils/quickjs 走 bionic 路径
target_compile_options(quickjs_rebuild PRIVATE
    -O2 -std=c11 -fno-exceptions -fno-rtti -DNDEBUG
    -D__ANDROID__
)

# jni.h 路径由 NDK 自动提供
set_target_properties(quickjs_rebuild PROPERTIES
    OUTPUT_NAME "quickjs_rebuild"
    POSITION_INDEPENDENT_CODE ON
)
