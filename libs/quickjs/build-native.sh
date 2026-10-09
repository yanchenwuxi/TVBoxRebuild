# build-native.sh —— 脱离 AGP 直接用 NDK 编 Bellard QuickJS → libquickjs_rebuild.so
# 用于 CI 或本地快速出 so，产物拷到 libs/quickjs/src/main/jniLibs/<abi>/
# 也支持 host 编译出 qjs_smoke（已实测能跑 JS，作为 C 源码自检）
#
# 用法：
#   ./libs/quickjs/build-native.sh [abi]            # NDK 编各 ABI 的 so
#   ./libs/quickjs/build-native.sh --host-smoke     # host 编 qjs_smoke（验证 C 源码能跑）
#   ./libs/quickjs/build-native.sh --all            # 上面两个都跑
# 前置：
#   1) ANDROID_NDK_HOME（或 --ndk <path>）
#   2) cmake 3.22+
#   3) 已跑 fetch-and-build.sh --src-only（或本脚本自动拉）
#
# 合规：Bellard quickjs-ng v0.17.0（MIT），不复用某商业闭源影音App libquickjs-android-wrapper.so

set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
JNIDIR="$HERE/src/main/jni"
JOBS="$(nproc 2>/dev/null || echo 2)"

ABIS=(arm64-v8a)
NDK_HOME="${ANDROID_NDK_HOME:-}"
MODE="so"   # so | smoke | all

# ---- 参数 ----
for a in "$@"; do
  case "$a" in
    --ndk=*)  NDK_HOME="${a#--ndk=}";;
    --abi=*)  ABIS=("${a#--abi=}");;
    --host-smoke) MODE="smoke";;
    --all)    MODE="all";;
    arm64-v8a|armeabi-v7a|x86_64|x86) ABIS=("$a"); MODE="so";;
  esac
done

# ---- 0. 确保 C 源码在 ----
if [[ ! -f "$JNIDIR/quickjs/quickjs.c" ]]; then
  echo "[*] C 源码缺失，先跑 fetch-and-build.sh --src-only ..."
  "$HERE/fetch-and-build.sh" --src-only
fi

# ---- 1. host 自检（qjs_smoke，证明 C 源码能编 + 能跑 JS）----
build_smoke() {
  echo "== host 自检：编 qjs_smoke（-O2 -DNDEBUG，关掉 QuickJS host 调试断言）=="
  local cc
  cc="$(command -v clang || command -v gcc || true)"
  [[ -n "$cc" ]] || { echo "  无 clang/gcc，跳过 host 自检"; return 0; }
  export TMPDIR=/tmp
  "$cc" -O2 -DNDEBUG -std=c11 -I "$JNIDIR/quickjs" -D_GNU_SOURCE \
    "$JNIDIR/quickjs/qjs_smoke.c" \
    "$JNIDIR"/quickjs/quickjs.c "$JNIDIR"/quickjs/quickjs-libc.c \
    "$JNIDIR"/quickjs/libregexp.c "$JNIDIR"/quickjs/libunicode.c "$JNIDIR"/quickjs/dtoa.c \
    -lm -lpthread -o /tmp/qjs_smoke
  echo "  编译 OK"
  local sm="/var/minis/workspace/tvbox_rebuild/5-纯自研工程方案/assets/sources/sample.js"
  [[ -f "$sm" ]] && { echo "  跑 sample.js:"; /tmp/qjs_smoke -f "$sm" 2>&1 | head -3; }
  echo "  [✓] C 源码可编译 + 可执行"
}

# ---- 2. NDK 编各 ABI 的 so ----
build_so() {
  [[ -n "$NDK_HOME" && -d "$NDK_HOME" ]] || { echo "需 ANDROID_NDK_HOME 或 --ndk <path>"; exit 1; }
  local TOOLCHAIN="$NDK_HOME/build/cmake/android.toolchain.cmake"
  [[ -f "$TOOLCHAIN" ]] || { echo "找不到 NDK toolchain：$TOOLCHAIN"; exit 1; }
  command -v cmake >/dev/null || { echo "需 cmake 3.22+"; exit 1; }

  for ABI in "${ABIS[@]}"; do
    local PLATFORM="android-26"
    case "$ABI" in
      armeabi-v7a) PLATFORM="android-21";;
    esac
    local BUILD="$HERE/build/ndk-$ABI"
    echo "== NDK 编 $ABI -> $BUILD =="
    cmake -S "$JNIDIR" -B "$BUILD" \
          -DCMAKE_TOOLCHAIN_FILE="$TOOLCHAIN" \
          -DANDROID_ABI="$ABI" -DANDROID_PLATFORM="$PLATFORM" \
          -DCMAKE_BUILD_TYPE=Release -DANDROID_STL=c++_shared
    cmake --build "$BUILD" --config Release -j"$JOBS"
    local OUT="$BUILD/quickjs_rebuild.so"
    [[ -f "$OUT" ]] || { echo "  ✗ $ABI 未产出 so"; exit 1; }
    ls -lh "$OUT"
    local DEST="$JNIDIR/../jniLibs/$ABI"   # src/main/jniLibs/<abi>/
    mkdir -p "$DEST"
    cp -f "$OUT" "$DEST/libquickjs_rebuild.so"
    echo "  [✓] 拷贝 -> $DEST/libquickjs_rebuild.so"
  done
}

case "$MODE" in
  smoke) build_smoke ;;
  so)    build_so ;;
  all)   build_smoke; build_so ;;
esac
echo "[完成]"
