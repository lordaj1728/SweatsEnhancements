package com.sweatsenhancements.feature;

import com.sweatsenhancements.SweatsEnhancements;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** A server-data-only run HUD. No local clock is substituted for the server's timer. */
public final class DungeonRunHud {
    private static String time = "", cleared = "", score = "", defeated = "";
    private static boolean inDungeon;
    private static long lastSidebar;

    public static boolean isInDungeon() { return inDungeon; }

    public static void reset() {
        time = cleared = score = defeated = "";
        inDungeon = false;
        lastSidebar = 0;
    }

    static String clean(String text) {
        String s = EnumChatFormatting.getTextWithoutFormattingCodes(text == null ? "" : text);
        return s == null ? "" : s.trim();
    }

    /** Parses only exact field prefixes, not arbitrary chat or the tab list's separate Time field. */
    public static void acceptSidebar(List<String> lines, long now) {
        boolean dungeon = DungeonFloorSixInfo.floorFromRows(lines) > 0;
        String newTime = "", newCleared = "";
        for (String raw : lines) {
            String s = clean(raw);
            if (s.startsWith("Time Elapsed:")) newTime = s.substring("Time Elapsed:".length()).trim();
            if (s.startsWith("Dungeon Cleared:")) newCleared = s.substring("Dungeon Cleared:".length()).trim();
        }
        if (!dungeon) {
            if (inDungeon && now - lastSidebar > 3000L) reset();
            return;
        }
        inDungeon = true;
        lastSidebar = now;
        if (newTime.matches("[0-9]{2}:[0-9]{2}(?::[0-9]{2})?s?")) time = newTime;
        if (newCleared.matches("[0-9]{1,3}%")) cleared = newCleared;
    }

    /** Completion lines recorded in the supplied full Floor III log. */
    public static void acceptChat(String raw) {
        if (!inDungeon) return;
        String s = clean(raw);
        if (s.matches("Team Score: [0-9]{1,3} \\([A-Z+]+\\)")) score = s.substring("Team Score: ".length());
        if (s.matches("Defeated .+ in [0-9]+m [0-9]+s")) defeated = s;
    }

    public static List<HudLayout.Line> lines() {
        List<HudLayout.Line> result = new ArrayList<HudLayout.Line>();
        if (!inDungeon) return result;
        if (!time.isEmpty()) result.add(new HudLayout.Line("§bDungeon §f" + time, 0xffffff));
        if (!cleared.isEmpty()) result.add(new HudLayout.Line("§eCleared §f" + cleared, 0xffffff));
        if (!score.isEmpty()) result.add(new HudLayout.Line("§aScore §f" + score, 0xffffff));
        if (!defeated.isEmpty()) result.add(new HudLayout.Line("§a" + defeated, 0xffffff));
        return result;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        try {
            if (SweatsEnhancements.mc == null || SweatsEnhancements.mc.theWorld == null || SweatsEnhancements.mc.thePlayer == null) { reset(); return; }
            List<String> lines = new ArrayList<String>();
            if (CustomScoreboard.on() && SweatsEnhancements.scoreboard != null) {
                lines.addAll(SweatsEnhancements.scoreboard.serverSidebarLines());
            } else {
                Scoreboard board = SweatsEnhancements.mc.theWorld.getScoreboard();
                ScoreObjective objective = board == null ? null : board.getObjectiveInDisplaySlot(1);
                if (objective != null) {
                    for (Score row : board.getSortedScores(objective)) {
                        String name = row.getPlayerName();
                        if (name != null && !name.startsWith("#"))
                            lines.add(ScorePlayerTeam.formatPlayerName(board.getPlayersTeam(name), name));
                    }
                }
            }
            acceptSidebar(lines, System.currentTimeMillis());
        } catch (Throwable t) { System.out.println("[SweatsEnhancements] dungeon sidebar read failed: " + t); }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        try {
            if (event.type == 0 && event.message != null) acceptChat(event.message.getUnformattedText());
        } catch (Throwable t) { System.out.println("[SweatsEnhancements] dungeon milestone read failed: " + t); }
    }

    @SubscribeEvent
    public void onUnload(WorldEvent.Unload event) {
        if (event.world != null && event.world.isRemote) reset();
    }
}
