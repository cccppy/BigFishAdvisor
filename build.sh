#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
GAME_DIR="${1:-${STS_DIR:-}}"
MTS_JAR="${2:-${MTS_JAR:-$ROOT/tools/ModTheSpire.jar}}"

if [[ -z "$GAME_DIR" || ! -f "$GAME_DIR/desktop-1.0.jar" ]]; then
  echo "Usage: ./build.sh /path/to/SlayTheSpire /path/to/ModTheSpire.jar" >&2
  exit 1
fi
if [[ ! -f "$MTS_JAR" ]]; then
  echo "ModTheSpire.jar not found: $MTS_JAR" >&2
  exit 1
fi
if ! command -v javac >/dev/null 2>&1; then
  echo "JDK 8 is required (javac not found)." >&2
  exit 1
fi

rm -rf "$ROOT/build/classes" "$ROOT/build/jar" "$ROOT/dist"
mkdir -p "$ROOT/build/classes" "$ROOT/build/jar" "$ROOT/dist"
CP="$GAME_DIR/desktop-1.0.jar:$MTS_JAR"
find "$ROOT/src" -name '*.java' -print0 | xargs -0 javac -encoding UTF-8 -source 8 -target 8 -cp "$CP" -d "$ROOT/build/classes"
cp -R "$ROOT/resources/bigfish" "$ROOT/build/jar/"
cp "$ROOT/resources/ModTheSpire.json" "$ROOT/LICENSE" "$ROOT/README.md" "$ROOT/OFL.txt" "$ROOT/build/jar/"
cp -R "$ROOT/build/classes/." "$ROOT/build/jar/"
( cd "$ROOT/build/jar" && jar cf "$ROOT/dist/BigFishAdvisor.jar" . )
echo "Built: $ROOT/dist/BigFishAdvisor.jar"

# Direct-launch shim for config.json
rm -rf "$ROOT/build/launcher"
mkdir -p "$ROOT/build/launcher"
javac -encoding UTF-8 -source 8 -target 8 -cp "$MTS_JAR" -d "$ROOT/build/launcher" \
  "$ROOT/launcher/bigfish/launcher/BigFishLauncher.java"
( cd "$ROOT/build/launcher" && jar cf "$ROOT/dist/BigFishLauncher.jar" . )
echo "Built: $ROOT/dist/BigFishLauncher.jar"

# Replacement for Steam's Workshop-only mts-launcher.jar
rm -rf "$ROOT/build/mtslauncher"
mkdir -p "$ROOT/build/mtslauncher"
javac -encoding UTF-8 -source 8 -target 8 -d "$ROOT/build/mtslauncher" \
  "$ROOT/launcher/com/megacrit/mtslauncher/Main.java"
( cd "$ROOT/build/mtslauncher" && jar cfe "$ROOT/dist/MtsLocalLauncher.jar" com.megacrit.mtslauncher.Main . )
echo "Built: $ROOT/dist/MtsLocalLauncher.jar"
