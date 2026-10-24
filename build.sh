#!/usr/bin/env bash
# Build FakepixelEnhancements for Minecraft 1.8.9.
#
# Forge 1.8.9 needs an old toolchain, and its original tooling has rotted (the
# MCP mapping host export.mcpbot.bspk.rs is gone, and the decompiler download is
# dead). This project therefore uses the actively-maintained ForgeGradle fork
# published on JitPack, and pins Gradle to 6.x because Gradle 7 refuses to run
# that fork.
#
# Anything missing is downloaded into ./toolchain on first run.
#
#   ./build.sh                 -> build/libs/FakepixelEnhancements-1.8.9-0.1.0.jar
#   ./build.sh clean           -> wipe build output
#   ./build.sh --info          -> any other gradle task also works

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TOOLCHAIN="${TOOLCHAIN_DIR:-$PROJECT_DIR/toolchain}"
JDK="$TOOLCHAIN/jdk8"
GRADLE="$TOOLCHAIN/gradle6"

mkdir -p "$TOOLCHAIN"

# --- JDK 8 -----------------------------------------------------------------
# Minecraft 1.8.9 and this toolchain expect Java 8. Change the URL for macOS
# (swap linux/x64 for mac/x64) or for an ARM machine (linux/aarch64).
if [ ! -x "$JDK/bin/java" ]; then
  echo "==> JDK 8 not found, downloading Temurin 8"
  curl -L --fail --progress-bar -o "$TOOLCHAIN/jdk8.tar.gz" \
    "https://api.adoptium.net/v3/binary/latest/8/ga/linux/x64/jdk/hotspot/normal/eclipse"
  tar xzf "$TOOLCHAIN/jdk8.tar.gz" -C "$TOOLCHAIN"
  mv "$TOOLCHAIN"/jdk8u* "$JDK"
  rm -f "$TOOLCHAIN/jdk8.tar.gz"
fi

# --- Gradle 6.9.4 ----------------------------------------------------------
if [ ! -x "$GRADLE/bin/gradle" ]; then
  echo "==> Gradle 6.9.4 not found, downloading"
  curl -L --fail --progress-bar -o "$TOOLCHAIN/gradle6.zip" \
    "https://services.gradle.org/distributions/gradle-6.9.4-bin.zip"
  unzip -q "$TOOLCHAIN/gradle6.zip" -d "$TOOLCHAIN"
  mv "$TOOLCHAIN/gradle-6.9.4" "$GRADLE"
  rm -f "$TOOLCHAIN/gradle6.zip"
fi

export JAVA_HOME="$JDK"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$TOOLCHAIN/gradle-home}"

cd "$PROJECT_DIR"
if [ "$#" -eq 0 ]; then
  set -- build
fi
"$GRADLE/bin/gradle" "$@" --console=plain --no-daemon

echo
echo "Finished. The installable mod is in build/libs/"
