import java.io.File;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import com.sweatsenhancements.feature.CustomScoreboard;
import com.sweatsenhancements.feature.Fullbright;
import com.sweatsenhancements.feature.CurrentPetHud;
import com.sweatsenhancements.feature.HudLayout;
import com.sweatsenhancements.feature.HudTools;

/**
 * Moving the scoreboard with H.
 *
 * The editor drags the panel's box around in screen pixels, and the panel stores its position as an
 * offset from the middle of the screen - the two numbers the Free Move alignment uses. This is the
 * conversion between them: a drag of so many pixels has to move the panel by exactly so many, and
 * putting it back has to leave the alignment as it was before anyone dragged anything.
 */
public class HudTest {

    public static void main(String[] args) throws Exception {
        java.io.File dir = new File("cfg-hud");
        wipe(dir);
        SweatsEnhancements.cfg = new Cfg(dir);
        SweatsEnhancements.cfg.load();
        String[] removed = {"CH002", "CH006", "CH020", "CH032", "CH039", "CH041", "MS004", "MS097"};
        for (String id : removed) {
            if (com.sweatsenhancements.config.Implemented.IDS.contains(id)
                    || com.sweatsenhancements.config.Features.byId(id) != null
                    || HudLayout.byKey(id) != null) {
                throw new AssertionError("removed feature still offered: " + id);
            }
        }
        System.out.println("  removed switches/catalog/HUD elements: 8 absent (want 8)");
        if (!com.sweatsenhancements.config.Implemented.IDS.contains("MS200")
                || !com.sweatsenhancements.config.Implemented.DEFAULT_OFF.contains("MS200")
                || com.sweatsenhancements.config.Features.byId("MS200") == null) {
            throw new AssertionError("Fullbright must have a default-off switch");
        }
        for (String id : new String[]{"MS201"}) {
            if (!com.sweatsenhancements.config.Implemented.IDS.contains(id)
                    || !com.sweatsenhancements.config.Implemented.DEFAULT_OFF.contains(id)
                    || com.sweatsenhancements.config.Features.byId(id) == null
                    || HudLayout.byKey(id) == null
                    || HudTools.sample(HudLayout.byKey(id)).isEmpty()) {
                throw new AssertionError("new HUD switch or editor preview missing: " + id);
            }
            SweatsEnhancements.cfg.setBool(Cfg.key(id), true);
            if (!HudTools.isEnabled(HudLayout.byKey(id))) throw new AssertionError("switch failed: " + id);
            SweatsEnhancements.cfg.setBool(Cfg.key(id), false);
        }
        CurrentPetHud.Pet firstPet = CurrentPetHud.parse(java.util.Arrays.asList(
                "§7[Lvl 145] §6Golden Dragon", "§eNext Level: §b310,106/1.8M - 16.4%",
                "§7[Lvl 25] §6Rose Dragon", "§bMAX LEVEL"));
        CurrentPetHud.Pet maxed = CurrentPetHud.parse(java.util.Arrays.asList(
                "§7[Lvl 100] §6Baby Yeti", "", "other tab row"));
        if (!"§5".equals(CurrentPetHud.colourOf("§7[Lvl 1] §5Endermite"))
                || !"§a".equals(CurrentPetHud.colourOf("§7[Lvl 1] §f§aEndermite"))
                || !"§6".equals(firstPet.colour))
            throw new AssertionError("pet rarity must use tab-list colour");
        if (firstPet == null || !"Golden Dragon".equals(firstPet.name)
                || firstPet.maxLevel || !"golden_dragon".equals(firstPet.iconKey())
                || !firstPet.status.contains("310,106/1.8M")
                || maxed == null || !maxed.maxLevel || !"MAX LEVEL".equals(maxed.status)
                || !"baby_yeti".equals(maxed.iconKey())
                || CurrentPetHud.parse(java.util.Arrays.asList("Pet:", "None")) != null
                || CurrentPetHud.parse(null) != null) throw new AssertionError("first pet / max level parsing");
        if (CurrentPetHud.parse(java.util.Arrays.asList("[Lvl 100] Golden Dragon")).maxLevel)
            throw new AssertionError("golden dragon level 100 isn't maxed");
        if (new java.io.File("../../src/main/resources/assets/sweatsenhancements/pets").exists())
            throw new AssertionError("third-party pet portraits should not ship in this release");
        HudLayout.Element petElement = HudLayout.byKey("MS201");
        HudLayout.Element panelElement = HudLayout.byKey("scoreboard");
        if (HudLayout.resized(100, 120, false) != 110
                || HudLayout.resized(50, -120, false) != 50
                || HudLayout.resized(200, 120, true) != 200) throw new AssertionError("resize bounds");
        HudLayout.resize(petElement, 120);
        HudLayout.resize(panelElement, -120);
        if (HudLayout.scalePercent(petElement) != 110
                || HudLayout.scalePercent(panelElement) != 90)
            throw new AssertionError("independent HUD and scoreboard resize");
        SweatsEnhancements.cfg.remove("hud.MS201.scale");
        SweatsEnhancements.cfg.remove(CustomScoreboard.SCALE_KEY);
        System.out.println("  rarity colours and per-element H resize config: OK");
        if (com.sweatsenhancements.config.Implemented.IDS.contains("MS202") || HudLayout.byKey("MS202") != null)
            throw new AssertionError("coordinates HUD still installed");
        if (com.sweatsenhancements.feature.GroundBottleHider.isExperienceBottle(null))
            throw new AssertionError("empty stack is not an XP bottle");
        if (!com.sweatsenhancements.config.Implemented.IDS.contains("MS003")
                || !com.sweatsenhancements.config.Implemented.DEFAULT_OFF.contains("MS003")
                || com.sweatsenhancements.config.Features.byId("MS003") == null)
            throw new AssertionError("hide ground XP bottles switch missing");
        System.out.println("  pet HUD preview/parser, ground XP bottle switch, coordinates removal OK");
        if (com.sweatsenhancements.config.Implemented.IDS.contains("MS203")
                || com.sweatsenhancements.config.Features.byId("MS203") != null
                || !com.sweatsenhancements.config.Implemented.IDS.contains("MS204")
                || !com.sweatsenhancements.config.Implemented.DEFAULT_OFF.contains("MS204")
                || com.sweatsenhancements.config.Features.byId("MS204") == null)
            throw new AssertionError("slot overlay removal / sprint switch");
        if (!com.sweatsenhancements.feature.AlwaysSprint.shouldHold(true, true, 29)
                || com.sweatsenhancements.feature.AlwaysSprint.shouldHold(false, true, 29)
                || com.sweatsenhancements.feature.AlwaysSprint.shouldHold(true, false, 29)
                || com.sweatsenhancements.feature.AlwaysSprint.shouldHold(true, true, 0))
            throw new AssertionError("sprint key input gate");
        System.out.println("  slot numbers removed; opt-in sprint key gating OK");
        if (!com.sweatsenhancements.config.Implemented.IDS.contains("MS205")
                || !com.sweatsenhancements.config.Implemented.DEFAULT_OFF.contains("MS205")
                || com.sweatsenhancements.config.Features.byId("MS205") == null
                || !com.sweatsenhancements.feature.HideFallingBlocks.hidesType(net.minecraft.entity.item.EntityFallingBlock.class)
                || com.sweatsenhancements.feature.HideFallingBlocks.hidesType(net.minecraft.entity.item.EntityItem.class)
                || com.sweatsenhancements.feature.HideFallingBlocks.hidesType(null))
            throw new AssertionError("falling block toggle / entity classification");
        com.sweatsenhancements.config.Cfg fishingCfg = new com.sweatsenhancements.config.Cfg(new java.io.File("cfg-fishing-gate"));
        if (!com.sweatsenhancements.feature.BobberTimer.enabled(fishingCfg))
            throw new AssertionError("bobber timer default enabled");
        fishingCfg.setBool(com.sweatsenhancements.config.Cfg.key("FI030"), false);
        if (com.sweatsenhancements.feature.BobberTimer.enabled(fishingCfg))
            throw new AssertionError("FI030 toggle fails to disable the bobber timer");
        if (com.sweatsenhancements.feature.PetCard.HEIGHT != 28)
            throw new AssertionError("pet text card height changed");
        System.out.println("  falling blocks: opt-in, entity type only; pet text card: 28px, vanilla icon");
        if (!com.sweatsenhancements.config.Implemented.IDS.contains("MS206")
                || !com.sweatsenhancements.config.Implemented.DEFAULT_OFF.contains("MS206")
                || com.sweatsenhancements.config.Features.byId("MS206") == null
                || !com.sweatsenhancements.feature.MuteTeleportItemSound.shouldMute(
                        "mob.endermen.portal", "§5Heroic Aspect of the Void")
                || !com.sweatsenhancements.feature.MuteTeleportItemSound.shouldMute(
                        "minecraft:mob.endermen.portal", "Aspect of the End")
                || com.sweatsenhancements.feature.MuteTeleportItemSound.shouldMute(
                        "mob.endermen.portal", "Enderman Pet")
                || com.sweatsenhancements.feature.MuteTeleportItemSound.shouldMute(
                        "random.orb", "Aspect of the End"))
            throw new AssertionError("teleport item sound toggle / sound and held-item gate");
        System.out.println("  teleport sound: AOTE/AOTV only; other sounds/items unaffected");
        Fullbright fullbright = new Fullbright();
        float first = fullbright.update(true, true, 0.42F);
        float ongoing = fullbright.update(true, true, first);
        float restored = fullbright.update(false, true, ongoing);
        float onAgain = fullbright.update(true, true, 0.7F);
        float leftWorld = fullbright.update(true, false, onAgain);
        if (first != 1000.0F || ongoing != 1000.0F || restored != 0.42F
                || leftWorld != 0.7F) throw new AssertionError("brightness was not restored");
        System.out.println("  Fullbright: 0.42 -> 1000 -> 0.42; world exit: 0.7 (want both originals)");
        if (CustomScoreboard.skyBlockTitle("§f§lFAKEPIXEL")
                || !CustomScoreboard.skyBlockTitle("§e§lSKYBLOCK")) {
            throw new AssertionError("lobby/game sidebar gate is wrong");
        }
        System.out.println("  lobby panel enabled: false (want false); SkyBlock panel: true (want true)");

        System.out.println("---- the panel is an element of the HUD editor");
        boolean found = false;
        for (HudLayout.Element e : HudLayout.ELEMENTS) {
            if (e.panel) {
                found = true;
                System.out.println("  editor element: " + e.label + " (key " + e.key + ", feature "
                        + e.feature + ", switched on: " + HudTools.isEnabled(e) + ")");
            }
        }
        System.out.println("  the editor holds the scoreboard: " + found);
        SweatsEnhancements.cfg.setBool(Cfg.key("MS085"), true);
        boolean panelInGameHud = false;
        boolean panelInEditor = false;
        for (HudTools.Placed p : HudTools.place(null, false)) {
            if (p.element.panel) panelInGameHud = true;
        }
        for (HudTools.Placed p : HudTools.place(null, true)) {
            if (p.element.panel) panelInEditor = true;
        }
        if (panelInGameHud || !panelInEditor) throw new AssertionError("panel must appear only in editor");
        System.out.println("  panel copies drawn by HUD during play: 0 (want 0); editor: 1 (want 1)");

        System.out.println("---- the position before anything is dragged");
        System.out.println("  horizontal: " + SweatsEnhancements.cfg.get("scoreboard.horizontalAlign", "Right")
                + ", vertical: " + SweatsEnhancements.cfg.get("scoreboard.verticalAlign", "Center"));

        // a panel on screen, as the drawing leaves it: box and text origin
        CustomScoreboard.boxWidth = 140;
        CustomScoreboard.boxHeight = 190;
        CustomScoreboard.boxLeft = 700;
        CustomScoreboard.boxTop = 40;
        CustomScoreboard.boxFirstY = 47;
        CustomScoreboard.boxBodyHeight = 180;

        // the player grabs it and moves it 120 across and 60 down, and what matters is where the
        // panel comes out: the offsets it stores are read back through its own alignment maths
        CustomScoreboard.moveTo(700 + 120, 40 + 60);
        int screenWidth = 1920;                 // the panel's own fallback with no client attached
        int screenHeight = 1080;
        int appliedLeft = screenWidth / 2 - CustomScoreboard.boxWidth / 2
                + SweatsEnhancements.cfg.getInt("scoreboard.freeX", 0);
        int appliedTop = screenHeight / 2 - CustomScoreboard.boxBodyHeight / 2
                + SweatsEnhancements.cfg.getInt("scoreboard.freeY", 0)
                - (CustomScoreboard.boxFirstY - CustomScoreboard.boxTop);   // the box, not the text
        System.out.println("  after a 120 x 60 drag: horizontal "
                + SweatsEnhancements.cfg.get("scoreboard.horizontalAlign", "") + ", vertical "
                + SweatsEnhancements.cfg.get("scoreboard.verticalAlign", "") + ", panel at "
                + appliedLeft + "," + appliedTop);
        System.out.println("  the panel is where it was dropped (820,100): "
                + (appliedLeft == 820 && appliedTop == 100 ? "yes" : "NO"));

        // and back: the panel is on the right edge, upright centre, with no free offsets left
        CustomScoreboard.resetPosition();
        System.out.println("  after reset: horizontal "
                + SweatsEnhancements.cfg.get("scoreboard.horizontalAlign", "") + ", vertical "
                + SweatsEnhancements.cfg.get("scoreboard.verticalAlign", "")
                + ", free keys present: " + SweatsEnhancements.cfg.contains("scoreboard.freeX"));

        // a drag with nothing drawn yet must not move the panel off the screen
        CustomScoreboard.boxWidth = 0;
        CustomScoreboard.boxHeight = 0;
        CustomScoreboard.moveTo(30, 30);
        System.out.println("  a drag with nothing drawn yet lands at free "
                + SweatsEnhancements.cfg.getInt("scoreboard.freeX", 0) + ","
                + SweatsEnhancements.cfg.getInt("scoreboard.freeY", 0) + " (a panel that far left is drawn, not lost)");
        CustomScoreboard.resetPosition();

        System.out.println("---- the preview the editor draws for it");
        for (String line : CustomScoreboard.previewLines()) {
            System.out.println("  | " + line);
        }
        wipe(dir);
    }

    static void wipe(File dir) {
        File[] kids = dir.listFiles();
        if (kids != null) {
            for (File kid : kids) {
                if (kid.isDirectory()) wipe(kid); else kid.delete();
            }
        }
        dir.delete();
    }
}
