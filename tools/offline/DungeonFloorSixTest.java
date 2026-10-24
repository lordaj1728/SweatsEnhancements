import com.sweatsenhancements.feature.DungeonFloorSixInfo;
import java.util.Arrays;

public class DungeonFloorSixTest {
    static void ok(boolean yes, String msg) { if (!yes) throw new AssertionError(msg); }
    public static void main(String[] args) {
        ok(DungeonFloorSixInfo.floorFromRows(Arrays.asList("§7⏣ §cThe Catacombs §7(F6)")) == 6, "F6 sidebar");
        ok(DungeonFloorSixInfo.floorFromRows(Arrays.asList("§7⏣ §cThe Catacombs §7(F3)")) == 3, "F3 not F6");
        ok(DungeonFloorSixInfo.terraStart("[BOSS] Sadan: My Terracotta Army is using some of the finest souls - old warriors and wizards!"), "observed start");
        ok(DungeonFloorSixInfo.terraEnd("[BOSS] Sadan: ENOUGH!"), "observed end");
        ok(!DungeonFloorSixInfo.terraEnd("[BOSS] The Watcher: That will be enough for now."), "not Watcher");
        ok(DungeonFloorSixInfo.golemCue("[BOSS] Sadan: Interesting strategy, waking up my Golems. Or was that unintentional?"), "golem before");
        ok(DungeonFloorSixInfo.golemCue("[BOSS] Sadan: You weren't supposed to wake up that Golem!"), "golem during");
        ok(!DungeonFloorSixInfo.golemCue("[BOSS] Sadan: My Terracotta Army is using some of the finest souls!"), "no invented golem");
        System.out.println("  F6 cues and golem acknowledgement OK");
    }
}
