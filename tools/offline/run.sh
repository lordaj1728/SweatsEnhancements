#!/bin/sh
# Run SweatsEnhancements's panels and config screen without Minecraft.
#
#   ./tools/offline/run.sh ../uploads/dump-*.txt
#
# PanelTest feeds a /sweatsenhancements dump through PanelLines and prints every line the panels would draw.
# MenuTest builds the whole config screen through the real MoulConfig processor and round-trips
# the settings through Cfg.
#
# The four files in stub/ stand in for the Minecraft classes MoulConfig touches while it builds a
# screen (Launch, Minecraft, FontRenderer, GuiTextField). Their member names have to match the
# production ones exactly, or MoulConfig dies part way through with NoSuchFieldError /
# NoSuchMethodError. If a Minecraft stub member is missing, the failure names it.
#
# Needs a built mod (./build.sh) and the Forge toolchain the build downloaded.

set -e
cd "$(dirname "$0")"
ROOT="$(cd ../.. && pwd)"
CACHE="$ROOT/toolchain/gradle-home/caches"
if [ ! -d "$CACHE" ]; then CACHE="${GRADLE_USER_HOME:-$HOME/.gradle}/caches"; fi
MAIN="$ROOT/build/classes/java/main:$ROOT/build/resources/main"
JAVA=/usr/lib/jvm/jdk-11/bin/java
JAVAC=/usr/lib/jvm/jdk-11/bin/javac

if [ ! -d "$ROOT/build/classes/java/main" ]; then echo "no classes in $MAIN - run ./build.sh first"; exit 1; fi

jar_path() {
    r="$(find "$CACHE" -name "$1" 2>/dev/null | head -1)"
    if [ -z "$r" ]; then echo "missing jar: $1" >&2; exit 1; fi
    printf '%s' "$r"
}

CP="$(jar_path 'forgeBin-1.8.9-*.jar')"
for j in 'lwjgl-2.9.4-nightly-20150209.jar' 'lwjgl_util-2.9.4-nightly-20150209.jar' \
         'librarylwjglopenal-20100824.jar' 'guava-*.jar' 'commons-lang3-3.3.2.jar' \
         'commons-io-2.4.jar' 'authlib-1.5.21.jar' 'log4j-api-2.0-beta9.jar' \
         'log4j-core-2.0-beta9.jar' 'brigadier-1.3.10.jar' 'kotlin-stdlib-2.3.0.jar' \
         'legacy-4.7.2.jar' 'mixin-0.7.11-SNAPSHOT.jar' 'gson-*.jar' 'launchwrapper-1.12.jar' 'jopt-simple-4.6.jar'; do
    CP="$CP:$(jar_path "$j")"
done

rm -rf out cfg cfg-protection cfg-migration
mkdir -p out
# the stubs name real Minecraft types in their signatures, so they compile against the same jars
$JAVAC -nowarn -cp "$CP" -d out $(find stub -name '*.java')
$JAVAC -nowarn -encoding UTF-8 -cp "out:$CP:$MAIN" -d out PanelTest.java MenuTest.java PanelStateTest.java TabListTest.java SlayerTest.java FooterTest.java PaddingTest.java ColumnsTest.java PerfTest.java PowerTest.java DumpRows.java HudTest.java CopyChatTest.java RequestedChatFiltersTest.java BatchFeaturesTest.java ItemVisualBatchTest.java ProtectedItemsTest.java LegacyConfigMigrationTest.java SmallQolTest.java SmoothTeleportTypeTest.java DungeonRunHudTest.java DungeonRequeueTest.java DungeonVisualsTest.java DungeonCombatTest.java DungeonFloorSixTest.java DungeonScoreHudTest.java      DungeonControlBoundsTest.java DungeonBlessingFilterTest.java DungeonVisualAdditionsTest.java DungeonPassFloorTipTest.java DungeonSmallQoLTest.java BlankGlassTooltipsTest.java  SlayerBasicsTest.java SlayerNefPortTest.java NEFDirectPortTest.java LeapMenuTest.java LeapSourcePortTest.java LeapScoreboardBridgeTest.java  

if [ $# -gt 0 ]; then
    echo "================ PanelTest"
    $JAVA -cp "out:$CP:$MAIN" PanelTest "$@"
fi
echo "================ MenuTest"
$JAVA -cp "out:$CP:$MAIN" MenuTest
python3 ../check_config_presentation.py
echo "================ PanelStateTest"
$JAVA -cp "out:$CP:$MAIN" PanelStateTest
echo "================ TabListTest"
$JAVA -cp "out:$CP:$MAIN" TabListTest
echo "================ SlayerTest"
$JAVA -cp "out:$CP:$MAIN" SlayerTest
echo "================ FooterTest"
$JAVA -cp "out:$CP:$MAIN" FooterTest
echo "================ PaddingTest"
$JAVA -cp "out:$CP:$MAIN" PaddingTest
if [ $# -gt 0 ] || [ -f ../../uploads/dump-20260924-014112.txt ]; then
    echo "================ PerfTest"
    $JAVA -cp "out:$CP:$MAIN" PerfTest "$@"
else
    echo "================ PerfTest SKIPPED (historical dump unavailable)"
fi
echo "================ HudTest"
$JAVA -cp "out:$CP:$MAIN" HudTest
echo "================ CopyChatTest"
$JAVA -cp "out:$CP:$MAIN" CopyChatTest
echo "================ RequestedChatFiltersTest"
$JAVA -cp "out:$CP:$MAIN" RequestedChatFiltersTest
echo "================ BatchFeaturesTest"
$JAVA -cp "out:$CP:$MAIN" BatchFeaturesTest
echo "================ ItemVisualBatchTest"
$JAVA -cp "out:$CP:$MAIN" ItemVisualBatchTest
echo "================ ProtectedItemsTest"
$JAVA -cp "out:$CP:$MAIN" ProtectedItemsTest
echo "================ LegacyConfigMigrationTest"
$JAVA -cp "out:$CP:$MAIN" LegacyConfigMigrationTest
echo "================ SmallQolTest"
$JAVA -cp "out:$CP:$MAIN" SmallQolTest
echo "================ SmoothTeleportTypeTest"
$JAVA -cp "out:$CP:$MAIN" SmoothTeleportTypeTest
echo "================ DungeonRunHudTest"
$JAVA -cp "out:$CP:$MAIN" DungeonRunHudTest
echo "================ DungeonRequeueTest"
$JAVA -cp "out:$CP:$MAIN" DungeonRequeueTest
echo "================ DungeonVisualsTest"
$JAVA -cp "out:$CP:$MAIN" DungeonVisualsTest
echo "================ DungeonCombatTest"
$JAVA -cp "out:$CP:$MAIN" DungeonCombatTest
echo "================ DungeonFloorSixTest"
$JAVA -cp "out:$CP:$MAIN" DungeonFloorSixTest
echo "================ DungeonScoreHudTest"
$JAVA -cp "out:$CP:$MAIN" DungeonScoreHudTest
echo "================ DungeonControlBoundsTest"
$JAVA -cp "out:$CP:$MAIN" DungeonControlBoundsTest
echo "================ PowerTest"
$JAVA -cp "out:$CP:$MAIN" PowerTest
if [ $# -gt 0 ] || [ -f ../../uploads/dump-20260924-014112.txt ]; then
    echo "================ ColumnsTest"
    $JAVA -cp "out:$CP:$MAIN" ColumnsTest "$@"
else
    echo "================ ColumnsTest SKIPPED (historical dump unavailable)"
fi

echo "================ DungeonBlessingFilterTest"
$JAVA -cp "out:$CP:$MAIN" DungeonBlessingFilterTest

echo "================ DungeonVisualAdditionsTest"
$JAVA -cp "out:$CP:$MAIN" DungeonVisualAdditionsTest

echo "================ DungeonPassFloorTipTest"
$JAVA -cp "out:$CP:$MAIN" DungeonPassFloorTipTest

echo "================ DungeonSmallQoLTest"
$JAVA -cp "out:$CP:$MAIN" DungeonSmallQoLTest

echo "================ BlankGlassTooltipsTest"
$JAVA -cp "out:$CP:$MAIN" BlankGlassTooltipsTest


echo "================ SlayerBasicsTest"
$JAVA -cp "out:$CP:$MAIN" SlayerBasicsTest

echo "================ SlayerNefPortTest"
$JAVA -cp "out:$CP:$MAIN" SlayerNefPortTest

echo "================ NEFDirectPortTest"
$JAVA -cp "out:$CP:$MAIN" NEFDirectPortTest

echo "================ LeapMenuTest"
$JAVA -cp "out:$CP:$MAIN" LeapMenuTest


echo "================ LeapSourcePortTest"
$JAVA -cp "out:$CP:$MAIN" LeapSourcePortTest

echo "================ LeapScoreboardBridgeTest"
$JAVA -cp "out:$CP:$MAIN" LeapScoreboardBridgeTest


