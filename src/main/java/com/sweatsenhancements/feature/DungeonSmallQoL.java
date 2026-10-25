package com.sweatsenhancements.feature;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Independent, conservative dungeon QoL switches; no clicks or fabricated server facts. */
public final class DungeonSmallQoL {
    private static String lastKeyObservation = "none";
    private Object world;
    private int ticks;
    private int lastKeys = -1;
    private int floor = -1;
    private boolean score270, score300;
    private long lastParty, lastNamedKeyMs, pendingKeyUntil;
    private int pendingKeyCount = -1;
    private final List<String> rows = new ArrayList<String>();

    public static String keyStatus() { return lastKeyObservation; }
    private static boolean on(String id) {
        return SweatsEnhancements.cfg != null && SweatsEnhancements.cfg.getBool(Cfg.key(id), false);
    }
    private static String plain(String raw) { return DungeonFloorSixInfo.clean(raw); }

    /** World and run boundaries clear one-shot notifications. */
    private void reset() {
        lastKeys = -1; floor = -1; score270 = score300 = false;
        ticks = 0; lastParty = lastNamedKeyMs = pendingKeyUntil = 0; pendingKeyCount = -1;

        rows.clear();
    }
    public static boolean signError(String text) {
        return text != null && plain(text).matches("(?i)^unable to locate sign(?: at -?[0-9]+,? -?[0-9]+,? -?[0-9]+)?[.!]?$");
    }
    /** Exact server pickup line: unlike the sidebar counter, it identifies the key type. */
    public static String obtainedKey(String raw) {
        if (raw == null) return null;
        String s = plain(raw);
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "^(?:\\[[^\\]]{1,30}\\]\\s*){0,3}[A-Za-z0-9_]{1,16} has obtained (Wither|Blood) Key!$").matcher(s);
        if (m.matches()) return m.group(1);
        // Fakepixel's automatic collection message, captured verbatim in the user's 06:40 dump.
        m = java.util.regex.Pattern.compile("^A (Wither|Blood) Key was picked up!$").matcher(s);
        return m.matches() ? m.group(1) : null;
    }
    public static boolean normalMobHp(String name) {
        if (name == null) return false;
        String s = plain(name);
        String lower = s.toLowerCase(Locale.ROOT);
        if (DungeonVisualAdditions.bossFloor(s) > 0 || DungeonVisualAdditions.scarfMinion(s)
                || lower.contains("spirit bear") || lower.contains("wither key") || lower.contains("blood key")) return false;
        return s.contains("❤") && !s.contains("✮") && !s.contains("✰")
                && !s.contains("★") && !s.contains("☆") && !s.contains("✯")
                && !s.toLowerCase(Locale.ROOT).contains("boss");
    }
    public static boolean terminalTitle(String title) {
        if (title == null) return false;
        String s = plain(title).toLowerCase(Locale.ROOT);
        return s.startsWith("click in order") || s.startsWith("select all the ")
                || s.startsWith("what starts with") || s.startsWith("correct all the panes")
                || s.startsWith("change all to same color") || s.startsWith("navigate the maze");
    }
    private void sidebar() {
        rows.clear();
        if (CustomScoreboard.on() && SweatsEnhancements.scoreboard != null) rows.addAll(SweatsEnhancements.scoreboard.serverSidebarLines());
        else if (SweatsEnhancements.mc != null && SweatsEnhancements.mc.theWorld != null) {
            Scoreboard board = SweatsEnhancements.mc.theWorld.getScoreboard();
            ScoreObjective objective = board == null ? null : board.getObjectiveInDisplaySlot(1);
            if (objective != null) for (Score score : board.getSortedScores(objective)) {
                String name = score.getPlayerName();
                if (name != null && !name.startsWith("#"))
                    rows.add(ScorePlayerTeam.formatPlayerName(board.getPlayersTeam(name), name));
            }
        }
    }
    public static int keyCount(List<String> sidebar) {
        if (sidebar == null) return -1;
        int result = -1;
        for (String row : sidebar) {
            int n = DungeonCombatVisuals.keyCount(row);
            if (n >= 0) result = n;
        }
        return result;
    }
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (SweatsEnhancements.mc == null || SweatsEnhancements.mc.theWorld == null || SweatsEnhancements.mc.thePlayer == null) {
            if (world != null) reset(); world = null; return;
        }
        if (world != SweatsEnhancements.mc.theWorld) { reset(); world = SweatsEnhancements.mc.theWorld; }
        if (++ticks < 10) return;
        ticks = 0;
        try {
            sidebar();
            int f = DungeonFloorSixInfo.floorFromRows(rows);
            if (f < 1) { if (floor > 0) reset(); return; }
            if (floor > 0 && floor != f) reset();
            floor = f;
            long now = System.currentTimeMillis();
            if (pendingKeyCount >= 0 && now >= pendingKeyUntil) {
                if (now - lastNamedKeyMs > 1600L) {
                    // The sidebar proves a key count rose but does NOT identify Wither vs Blood.
                    String title = "Dungeon Key was picked up!";
                    lastKeyObservation = "sidebar count rose to " + pendingKeyCount + "; type unknown; local=" + on("DU027") + "; party=" + on("DU028");
                    if (on("DU027")) DungeonAlertCard.show(title,0xFFFFFFFF);
                    if (on("DU028") && now-lastParty >= 3000L) {
                        SweatsEnhancements.mc.thePlayer.sendChatMessage("/pc " + title);
                        lastParty = now;
                    }
                }
                pendingKeyCount = -1;
            }
            int keys = keyCount(rows);
            if (keys >= 0 && lastKeys >= 0 && keys > lastKeys && keys <= 9
                    && now-lastNamedKeyMs > 1200L) {
                pendingKeyCount = keys;
                pendingKeyUntil = now + 1200L;
            }
            if (keys >= 0) lastKeys = keys;
            int estimate = DungeonScoreHud.estimatedTotal();
            if (estimate >= 270 && !score270 && on("DU030")) {
                score270 = true; DungeonAlertCard.show("Estimated Score: 270+", 0xFFFFD95F);
            }
            if (estimate >= 300 && !score300 && on("DU031")) {
                score300 = true; DungeonAlertCard.show("Estimated Score: 300+", 0xFF76FF88);
            }
        } catch (Throwable t) { System.out.println("[SweatsEnhancements] dungeon small QoL read failed: " + t); }
    }
    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (event == null || event.message == null) return;
        String s = event.message.getUnformattedText();
        if (event.type != 2 && on("DU032") && signError(s)) { event.setCanceled(true); return; }
        if (floor < 1) {
            try { sidebar(); floor = DungeonFloorSixInfo.floorFromRows(rows); }
            catch (Throwable ignored) { /* keep unknown rather than guess location */ }
        }
        String key = obtainedKey(s);
        if (key != null) {
            lastNamedKeyMs = System.currentTimeMillis(); pendingKeyCount = -1;
            lastKeyObservation = key + " message type=" + event.type + "; dungeonFloor=" + floor
                    + "; local=" + on("DU027") + "; party=" + on("DU028");
        }
        if (floor > 0) {
            if (key != null) {
                String title = key + " Key was picked up!";
                if (on("DU027")) DungeonAlertCard.show(title, 0xFFFFFFFF);
                if (on("DU028") && SweatsEnhancements.mc != null && SweatsEnhancements.mc.thePlayer != null
                        && System.currentTimeMillis() - lastParty >= 3000L) {
                    SweatsEnhancements.mc.thePlayer.sendChatMessage("/pc " + title);
                    lastParty = System.currentTimeMillis();
                }
            }
        }
    }
    @SubscribeEvent
    public void onName(RenderLivingEvent.Specials.Pre event) {
        if (floor < 1 || !on("DU033") || event == null || !(event.entity instanceof EntityArmorStand)) return;
        if (normalMobHp(event.entity.getCustomNameTag())) event.setCanceled(true);
    }
    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        if (floor < 1 || !on("DU034") || event == null || event.toolTip == null
                || SweatsEnhancements.mc == null || !(SweatsEnhancements.mc.currentScreen instanceof GuiChest)) return;
        GuiChest gui = (GuiChest) SweatsEnhancements.mc.currentScreen;
        if (!(gui.inventorySlots instanceof ContainerChest)) return;
        String title = ((ContainerChest)gui.inventorySlots).getLowerChestInventory().getDisplayName().getUnformattedText();
        if (terminalTitle(title)) event.toolTip.clear();
    }
    @SubscribeEvent
    public void onUnload(WorldEvent.Unload event) {
        if (event.world != null && event.world.isRemote) { reset(); world = null; }
    }
}
