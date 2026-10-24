import java.io.File;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import com.sweatsenhancements.feature.CustomScoreboard;

import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.IScoreObjectiveCriteria;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * The scoreboard's on/off, tick by tick, outside the game.
 *
 * This is the one thing that cannot be checked by reading the code and cannot be checked by a
 * drawing test: the panel empties the sidebar's display slot, and what it knows on the NEXT tick
 * depends on what it did on this one. 0.9.6 read the slot it had just emptied, decided the server
 * had no sidebar, and dropped its own reference - leaving the player with no sidebar at all and no
 * way to get it back. Every line printed here is a claim about a different tick:
 *
 *   tick 1  switch on      - the client's sidebar is off the screen and the panel knows what it is
 *   tick 2  next tick      - and it still knows, although the slot is empty (the 0.9.6 bug)
 *   tick 3  switch off     - the client's sidebar is back in its display slot
 *   tick 4  slot taken     - emptied behind the panel's back: put back again while the switch-off
 *                           repair is running, and left alone once it has expired
 */
public class PanelStateTest {

    static void wipe(File dir) {
        File[] kids = dir.listFiles();
        if (kids != null) {
            for (File kid : kids) {
                if (kid.isDirectory()) wipe(kid); else kid.delete();
            }
        }
        dir.delete();
    }

    private static void line(Scoreboard board, ScoreObjective objective, String text, int points) {
        Score score = board.getValueFromObjective(text, objective);
        score.setScorePoints(points);
    }

    private static String shown(Scoreboard board) {
        ScoreObjective obj = board.getObjectiveInDisplaySlot(1);
        if (obj == null) return "(nothing)";
        return "\"" + obj.getName() + "\"";
    }

    public static void main(String[] args) throws Exception {
        wipe(new File("cfg-state"));
        SweatsEnhancements.cfg = new Cfg(new File("cfg-state"));
        SweatsEnhancements.cfg.setBool("features.MS085", true);

        Scoreboard board = new Scoreboard();
        ScoreObjective objective = board.addScoreObjective("se_test", IScoreObjectiveCriteria.DUMMY);
        objective.setDisplayName("\u00a7f\u00a7lSKYBLOCK");
        board.setObjectiveInDisplaySlot(1, objective);
        line(board, objective, "\u00a7f09/23/26 hub-3", 12);
        line(board, objective, "\u00a7e Summer 30th", 11);
        line(board, objective, "\u00a77\u23e3 \u00a7bVillage", 10);
        line(board, objective, "\u00a7fPurse: \u00a762,860,502", 9);
        line(board, objective, "\u00a7fBits: \u00a7b1,334", 8);
        line(board, objective, "\u00a7fSlayer Quest", 7);
        line(board, objective, "\u00a75Revenant Horror V", 6);
        line(board, objective, "\u00a7emc.fakepixel.me", 1);

        Minecraft.getMinecraft().theWorld = new WorldClient(board);
        CustomScoreboard panel = new CustomScoreboard();
        // the probes read the registered panel, which the mod sets while it starts up
        SweatsEnhancements.scoreboard = panel;
        TickEvent.ClientTickEvent tick = new TickEvent.ClientTickEvent(TickEvent.Phase.END);

        panel.onTick(tick);
        System.out.println("tick 1  switch on   slot 1 holds " + shown(board)
                + "   objective known: \"" + CustomScoreboard.probeObjective() + "\""
                + "   lines read: " + CustomScoreboard.probeRead()
                + "   lines drawn: " + CustomScoreboard.probeBuilt());

        panel.onTick(tick);
        System.out.println("tick 2  next tick   slot 1 holds " + shown(board)
                + "   objective known: \"" + CustomScoreboard.probeObjective() + "\""
                + "   lines read: " + CustomScoreboard.probeRead());

        SweatsEnhancements.cfg.setBool("features.MS085", false);
        panel.onTick(tick);
        System.out.println("tick 3  switch off  slot 1 holds " + shown(board)
                + "   (" + CustomScoreboard.why + ")");

        SweatsEnhancements.cfg.setBool("features.MS085", true);
        panel.onTick(tick);
        SweatsEnhancements.cfg.setBool("features.MS085", false);
        panel.onTick(tick);
        board.setObjectiveInDisplaySlot(1, null);      // as a packet that never arrived would leave it
        panel.onTick(tick);
        System.out.println("tick 4  slot taken  slot 1 holds " + shown(board)
                + "   (" + CustomScoreboard.why + ")");

        for (int i = 0; i < 110; i++) panel.onTick(tick);   // the repair's five seconds run out
        board.setObjectiveInDisplaySlot(1, null);
        panel.onTick(tick);
        System.out.println("after the repair has expired, slot 1 holds " + shown(board)
                + "  (the server's word is final again)");
    }
}
