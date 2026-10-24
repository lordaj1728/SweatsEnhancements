import com.sweatsenhancements.feature.DungeonScoreHud;
import java.util.Arrays;
public class DungeonScoreHudTest {
    private static void ok(boolean b, String message) { if (!b) throw new AssertionError(message); }
    public static void main(String[] args) {
        ok(DungeonScoreHud.number("Secrets Found: 82%", "Secrets Found:") == 82, "percent");
        int[] values=DungeonScoreHud.parseTab(Arrays.asList("§fSecrets Found: §e5%", "§aDeaths: §f(0)", "§fCrypts: §60"));
        ok(values[0]==5 && values[1]==0 && values[2]==0, "observed formatted tab rows");
        ok(DungeonScoreHud.parseTab(Arrays.asList("Unrelated"))[0]==-1, "no invented secrets");
        ok(DungeonScoreHud.number("Keys: 2", "Deaths:") == -1, "unrelated row");
        ok(DungeonScoreHud.time("01:41s") == 101, "sidebar elapsed");
        ok(DungeonScoreHud.time("03:05") == 185, "tab elapsed");
        ok(DungeonScoreHud.time("not a time") == -1, "unknown time");
        ok(DungeonScoreHud.skill(2,1,false) == 82, "skill penalties");
        ok(DungeonScoreHud.speed(480,0) == 100, "first speed boundary");
        ok(DungeonScoreHud.target(6) == 85 && DungeonScoreHud.target(16) == 85, "F6/M6 target");
        System.out.println("  Dungeon score inputs and boundaries OK");
    }
}
