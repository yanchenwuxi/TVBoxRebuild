# 拉取 Bellard QuickJS（quickjs-ng/quickjs，MIT，官方维护分叉）
# 并编出 libquickjs_rebuild.so（含 JNI 包装 quickjs_rebuild.c）。
#
# 用法（在 5-纯自研工程方案/ 下）：
#   ./libs/quickjs/fetch-and-build.sh
# 或仅拉源码（CMake 集成后由 AGP 自动编 so）：
#   ./libs/quickjs/fetch-and-build.sh --src-only
#
# 默认版本：quickjs-ng/quickjs v0.17.0（含 quickjs.c/quickjs-libc.c/cutils/libregexp 等）
# 若离线/内网，把 QUICKJS_TAR 指到本地已下载的 tar.gz 即可。

set -e

HERE="$(cd "$(dirname "$0")" && pwd)"
JNIDIR="$HERE/src/main/jni"
QUICKJS_DIR="$JNIDIR/quickjs"

# ---- 1. 拉 C 源码 ----
QUICKJS_TAR="${QUICKJS_TAR:-https://github.com/quickjs-ng/quickjs/archive/refs/tags/v0.17.0.tar.gz}"
WORK="/tmp/quickjs_src_$(date +%s)"

fetch_src() {
  echo "[*] 下载 QuickJS C 源码 -> $WORK"
  mkdir -p "$WORK"
  if [[ -f "$QUICKJS_TAR" ]] && [[ "$QUICKJS_TAR" != http* ]]; then
    tar xzf "$QUICKJS_TAR" -C "$WORK"
  else
    curl -sL "$QUICKJS_TAR" -o "$WORK/qs.tgz"
    tar xzf "$WORK/qs.tgz" -C "$WORK"
  fi
  local top
  top="$(ls -d "$WORK"/quickjs-* | head -1)"
  # 把需要的 C 文件铺进 jni/quickjs/（quickjs-ng v0.17 模块化布局：
  # quickjs.c 已并入 cutils，需 libregexp/libunicode/dtoa + 全部 16 个 .h）
  rm -rf "$QUICKJS_DIR"
  mkdir -p "$QUICKJS_DIR"
  # C 源
  cp -v "$top"/quickjs.c "$top"/quickjs.h "$QUICKJS_DIR"/
  cp -v "$top"/quickjs-libc.c "$top"/quickjs-libc.h "$QUICKJS_DIR"/
  cp -v "$top"/libregexp.c "$top"/libregexp.h "$QUICKJS_DIR"/
  cp -v "$top"/libunicode.c "$top"/libunicode.h "$QUICKJS_DIR"/
  cp -v "$top"/dtoa.c "$QUICKJS_DIR"/
  # 全部 16 个头文件（模块化编译必需；缺一个就编不过）
  cp -v "$top"/*.h "$QUICKJS_DIR"/ 2>/dev/null || \
  for h in quickjs-atom.h quickjs-opcode.h quickjs-c-atomics.h \
           cutils.h list.h libregexp-opcode.h libunicode-table.h \
           dtoa.h unicode_gen_def.h quickjs.h quickjs-libc.h \
           libregexp.h libunicode.h \
           builtin-iterator-zip.h builtin-iterator-zip-keyed.h \
           builtin-array-fromasync.h; do
    test -f "$top/$h" && cp -v "$top/$h" "$QUICKJS_DIR"/ || echo "  (跳过缺失 $h)"
  done
  echo "[✓] C 源码就绪: $(ls "$QUICKJS_DIR" | tr '\n' ' ')"
}

# ---- 2. （可选）本地直接编 so（验证 CMake 无误）----
build_so() {
  command -v cmake >/dev/null || { echo "需安装 cmake 3.22+"; return 0; }
  echo "[*] 本地 NDK 编译验证（可选）..."
  # Android NDK 编译由 AGP 自动触发；这里仅校验 CMake 配置语法
  cmake -S "$JNIDIR" -B "$WORK/build_check" \
        -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake" \
        -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-26 \
        -DCMAKE_BUILD_TYPE=Release 2>&1 | tail -5 || \
    echo "[!] 本地编译校验跳过（需设 ANDROID_NDK_HOME）；AGP 构建时会自动编 so"
}

# ---- 主流程 ----
case "${1:-all}" in
  --src-only) fetch_src ;;
  *) fetch_src; build_so ;;
esac

echo "[✓] 完成。AGP 构建 :libs:quickjs 时 CMake 会自动编出 libquickjs_rebuild.so"
