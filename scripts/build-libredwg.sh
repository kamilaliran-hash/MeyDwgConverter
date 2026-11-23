#!/usr/bin/env bash
# Cross-compiles LibreDWG as a static library for each Android ABI and installs
# it into app/src/main/cpp/libredwg/<abi>/{lib,include}.
#
# Requirements (Linux or macOS): Android NDK, curl, tar, make.
# Usage:
#   export ANDROID_NDK_HOME=$HOME/Android/Sdk/ndk/27.2.12479018
#   ./scripts/build-libredwg.sh
set -euo pipefail

LIBREDWG_VERSION="${LIBREDWG_VERSION:-0.13.3}"
API=28
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WORK="$ROOT/build-libredwg"
DEST="$ROOT/app/src/main/cpp/libredwg"

: "${ANDROID_NDK_HOME:?Set ANDROID_NDK_HOME to your NDK folder}"
case "$(uname -s)" in
  Linux)  HOST_TAG=linux-x86_64 ;;
  Darwin) HOST_TAG=darwin-x86_64 ;;   # NDK uses this name on Apple Silicon too
  *) echo "Use Linux, macOS or WSL"; exit 1 ;;
esac
TOOLCHAIN="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/$HOST_TAG"
JOBS="$(getconf _NPROCESSORS_ONLN 2>/dev/null || echo 4)"

mkdir -p "$WORK"
cd "$WORK"
TARBALL="libredwg-$LIBREDWG_VERSION.tar.xz"
if [ ! -f "$TARBALL" ]; then
  curl -fL -o "$TARBALL" "https://ftp.gnu.org/gnu/libredwg/$TARBALL" || \
  curl -fL -o "$TARBALL" "https://ftpmirror.gnu.org/libredwg/$TARBALL"
fi
rm -rf "libredwg-$LIBREDWG_VERSION"
tar xf "$TARBALL"
SRC="$WORK/libredwg-$LIBREDWG_VERSION"

build_abi() {
  local abi="$1" host="$2" clang_target="$3"
  echo "=== Building LibreDWG for $abi ==="
  local bdir="$WORK/build-$abi"
  rm -rf "$bdir"; mkdir -p "$bdir"; cd "$bdir"

  export CC="$TOOLCHAIN/bin/${clang_target}${API}-clang"
  export AR="$TOOLCHAIN/bin/llvm-ar"
  export RANLIB="$TOOLCHAIN/bin/llvm-ranlib"
  export STRIP="$TOOLCHAIN/bin/llvm-strip"
  export CFLAGS="-O2 -fPIC"

  # (Unknown --disable-* options only produce a warning in autoconf.)
  "$SRC/configure" \
    --host="$host" \
    --disable-shared --enable-static --with-pic \
    --disable-bindings --disable-python --disable-werror

  # Only the library is needed, not the command-line tools or docs.
  make -C src -j"$JOBS"

  mkdir -p "$DEST/$abi/lib" "$DEST/$abi/include"
  cp src/.libs/libredwg.a "$DEST/$abi/lib/"
  cp "$SRC/include/dwg.h" "$SRC/include/dwg_api.h" "$DEST/$abi/include/"
  # Internal headers (needed for the DXF writer, as LibreDWG's dwg2dxf uses it)
  mkdir -p "$DEST/$abi/include/internal"
  cp "$SRC"/src/*.h "$DEST/$abi/include/internal/"
  cp src/config.h "$DEST/$abi/include/internal/"
  cd "$WORK"
}

build_abi arm64-v8a   aarch64-linux-android   aarch64-linux-android
build_abi armeabi-v7a arm-linux-androideabi   armv7a-linux-androideabi
build_abi x86_64      x86_64-linux-android    x86_64-linux-android

echo
echo "Done. LibreDWG $LIBREDWG_VERSION installed in $DEST"
echo "Now open the project in Android Studio, or run: ./gradlew assembleDebug"
