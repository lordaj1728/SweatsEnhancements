import com.sweatsenhancements.feature.DungeonCombatVisuals;
import com.sweatsenhancements.feature.DungeonFloorSixInfo;

public final class DungeonCombatTest {
    static void ok(boolean b, String label) { if (!b) throw new AssertionError(label); }
    public static void main(String[] args) {
        ok(DungeonFloorSixInfo.floorFromRows(java.util.Arrays.asList("§7The Catacombs (F1)")) == 1, "normal dungeon gate");
        ok(DungeonFloorSixInfo.floorFromRows(java.util.Arrays.asList("§7The Catacombs (M1)")) == 1, "master dungeon gate for existing visuals");
        ok(DungeonFloorSixInfo.floorFromRows(java.util.Arrays.asList("The Catacombs (M7)")) == 7, "upper master floor");
        ok(DungeonFloorSixInfo.floorFromRows(java.util.Arrays.asList("Catacombs (F8)")) == -1, "not a known floor");
        ok(DungeonFloorSixInfo.floorFromRows(null) == -1, "missing sidebar");
        ok(DungeonCombatVisuals.keyCount("§fKeys:§c ■ §c✘ §8■ §a1x") == 1, "one server-side key");
        ok(DungeonCombatVisuals.keyCount("§fKeys:§c ■ §c✘ §8■ §a0x") == 0, "no key");
        ok(DungeonCombatVisuals.keyCount("§fTime Elapsed: §a00:16s") == -1, "not other sidebar row");
        ok(DungeonCombatVisuals.dungeonElapsed("§fTime Elapsed: §a01:41s") == 101000,
                "blood rush uses actual sidebar run clock");
        ok(DungeonCombatVisuals.dungeonElapsed("§fKeys: §a1x") == -1, "unrelated line not timer");
        ok("witherColour".equals(DungeonCombatVisuals.starColourKey("✮ Withermancer")), "wither colour");
        ok("commanderColour".equals(DungeonCombatVisuals.starColourKey("✮ Zombie Commander")), "commander colour");
        ok("masterColour".equals(DungeonCombatVisuals.starColourKey("✮ Skeleton Master")), "master colour");
        ok("stormyColour".equals(DungeonCombatVisuals.starColourKey("✮ Stormy")), "stormy colour");
        ok("starredColour".equals(DungeonCombatVisuals.starColourKey("✮ Skeleton Grunt")), "normal star colour");
        ok(DungeonCombatVisuals.visibleStyle("Box") && DungeonCombatVisuals.visibleStyle("Filled Box")
                && !DungeonCombatVisuals.visibleStyle("Off"), "display style");
        String fel = java.util.Base64.getEncoder().encodeToString(
            "{\"textures\":{\"SKIN\":{\"url\":\"https://textures.minecraft.net/texture/7a59bb0a7a32965b3d90d8eafa899d1835f424509eadd4e6b709ada50b9cf\"}}}"
             .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        ok(DungeonCombatVisuals.felTexture(fel), "Fel skull signature");
        ok(!DungeonCombatVisuals.felTexture("not-base64"), "invalid skull ignored");
        ok(DungeonCombatVisuals.starred("§6✮ §cCrypt Dreadlord §a118k❤"), "starred name stand");
        ok(!DungeonCombatVisuals.starred("§e✰ §cWalker §a41000❤"), "blood marker is distinct");
        ok(DungeonCombatVisuals.starLabelAlive("§6✮ §cSkeleton Grunt §a62000§c❤"), "live starred stand");
        ok(!DungeonCombatVisuals.starLabelAlive("§6✮ §cSkeleton Grunt §a0§c❤"), "dead starred stand");
        ok(DungeonCombatVisuals.bloodMob("§e✰§5♃ §cWalker §a41000❤"), "blood Walker");
        ok(DungeonCombatVisuals.bloodMob("§e✰ §cMute §a36759❤"), "blood Mute");
        ok(!DungeonCombatVisuals.bloodMob("§6✮ §cSkeleton Soldier"), "not arbitrary star");
        ok(DungeonCombatVisuals.keyStand("§8Wither key") && DungeonCombatVisuals.keyStand("§cBlood key"), "key stands");
        ok(!DungeonCombatVisuals.keyStand("§cSkeletal key"), "unrelated key stand");
        ok(DungeonCombatVisuals.bloodStart("The BLOOD DOOR has been opened!"), "observed start");
        ok(DungeonCombatVisuals.bloodReady("[BOSS] The Watcher: That will be enough for now."), "observed ready");
        ok(DungeonCombatVisuals.bloodReady("[BOSS] The Watcher: You have proven yourself. You may pass."), "alternate ready");
        System.out.println("  Dungeon combat: real sidebar, starred/blood names, key stands and blood cues OK");
    }
}
