package com.arena.pekaclient.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;

import java.util.Collection;
import java.util.Locale;

/**
 * Whether the player is in a dungeon. The only signal used is the scoreboard sidebar: a line that names
 * "Catacombs", read from the team prefix, name and suffix together. The sidebar is read every {@link #SCAN_EVERY_TICKS} ticks, never every frame, and the result is
 * cached. If the sidebar does not show the line, the player is treated as not in a dungeon. That is the safe
 * default for overlays and for the leap menu.
 */
public final class DungeonContext {

    static final int SCAN_EVERY_TICKS = 20;
    private static int ticksSinceScan = SCAN_EVERY_TICKS;
    private static boolean inDungeon;

    private DungeonContext() {
    }

    /** True when the last sidebar scan found a dungeon line. */
    public static boolean inDungeon() {
        return inDungeon;
    }

    /** Called once per client tick. Resets when the world is gone. */
    public static void tick(Minecraft mc) {
        if (mc.theWorld == null || mc.thePlayer == null) {
            inDungeon = false;
            ticksSinceScan = SCAN_EVERY_TICKS;
            return;
        }
        if (++ticksSinceScan < SCAN_EVERY_TICKS) {
            return;
        }
        ticksSinceScan = 0;
        inDungeon = scan(mc.theWorld.getScoreboard());
    }

    private static boolean scan(Scoreboard board) {
        if (board == null) {
            return false;
        }
        ScoreObjective sidebar = board.getObjectiveInDisplaySlot(1);
        if (sidebar == null) {
            return false;
        }
        Collection<Score> scores = board.getSortedScores(sidebar);
        if (scores == null) {
            return false;
        }
        for (Score s : scores) {
            if (s == null) {
                continue;
            }
            // A sidebar line's visible text is the team prefix + the holder name + the team suffix. The holder
            // name alone is often only a hidden key, so the team text must be added before matching.
            String holder = s.getPlayerName();
            String line = ScorePlayerTeam.formatPlayerName(board.getPlayersTeam(holder), holder);
            if (isCatacombsLine(line)) {
                return true;
            }
        }
        return false;
    }

    /** Pure check used by the scan and by tests. Colour codes are removed before matching. */
    public static boolean isCatacombsLine(String rawLine) {
        if (rawLine == null) {
            return false;
        }
        String line = LeapText.strip(rawLine).toLowerCase(Locale.ROOT);
        return line.contains("catacombs");
    }
}
