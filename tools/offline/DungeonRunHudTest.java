import com.sweatsenhancements.feature.DungeonRunHud;
import com.sweatsenhancements.feature.HudLayout;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class DungeonRunHudTest {
    static void ok(boolean value, String what) { if (!value) throw new AssertionError(what); }
    static String text(List<HudLayout.Line> lines) {
        StringBuilder s = new StringBuilder();
        for (HudLayout.Line line : lines) s.append(line.text).append('\n');
        return s.toString();
    }
    public static void main(String[] args) {
        DungeonRunHud.reset();
        DungeonRunHud.acceptSidebar(Arrays.asList("§6SKYBLOCK", "§7⏣ The Catacombs (F3)",
                "§fTime Elapsed: §a02:49s", "§fDungeon Cleared: §e18%"), 1000);
        ok(text(DungeonRunHud.lines()).contains("02:49s"), "server timer with suffix");
        DungeonRunHud.acceptSidebar(Arrays.asList("§7⏣ The Catacombs (F3)",
                "§fTime Elapsed: §a02:49", "§fDungeon Cleared: §e18%"), 1100);
        ok(text(DungeonRunHud.lines()).contains("02:49"), "server time");
        ok(text(DungeonRunHud.lines()).contains("18%"), "server clear");
        DungeonRunHud.acceptChat("Team Score: 196 (B)");
        DungeonRunHud.acceptChat("Defeated The Professor in 8m 35s");
        ok(text(DungeonRunHud.lines()).contains("196 (B)"), "completion score");
        ok(text(DungeonRunHud.lines()).contains("8m 35s"), "completion duration");
        DungeonRunHud.acceptSidebar(Collections.singletonList("§6SKYBLOCK"), 5000);
        ok(DungeonRunHud.lines().isEmpty(), "leave dungeon resets");
        DungeonRunHud.acceptSidebar(Arrays.asList("The Catacombs (M2)", "Time Elapsed: 03:12"), 6000);
        ok(DungeonRunHud.isInDungeon(), "master floor counts as dungeon");
        DungeonRunHud.reset();
        DungeonRunHud.acceptChat("Team Score: 300 (S)");
        ok(DungeonRunHud.lines().isEmpty(), "chat outside dungeon ignored");
        System.out.println("  Dungeon HUD: sidebar time/clear, completion, world-exit guard OK");
    }
}
