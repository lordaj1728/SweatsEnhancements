import com.sweatsenhancements.config.Cfg;
import com.sweatsenhancements.feature.DungeonRequeue;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;

public final class DungeonRequeueTest {
    static void ok(boolean b, String label) { if (!b) throw new AssertionError(label); }
    public static void main(String[] args) {
        ok(DungeonRequeue.acceptSend("/joindungeon 1",1000), "first requeue allowed");
        ok(!DungeonRequeue.acceptSend("/joindungeon 1",1001), "duplicate bus send blocked");
        ok(DungeonRequeue.acceptSend("/joindungeon 1",2500), "later manual requeue allowed");

        ok("F3".equals(DungeonRequeue.floorMarker(Arrays.asList("§6SKYBLOCK", "§7⏣ The Catacombs (F3)"))), "normal sidebar");
        ok("M7".equals(DungeonRequeue.floorMarker(Collections.singletonList("§7⏣ The Catacombs (M7)"))), "master sidebar");
        ok(DungeonRequeue.floorMarker(Collections.singletonList("§7⏣ The Catacombs (F8)")).isEmpty(), "reject out of range");
        ok(DungeonRequeue.floorMarker(Collections.singletonList("§7Different zone (F3)")).isEmpty(), "reject unrelated floor");
        ok("/joindungeon 3".equals(DungeonRequeue.joinFor("F3")), "normal command");
        ok("/joindungeon 7 master".equals(DungeonRequeue.joinFor("M7")), "master command");
        ok(DungeonRequeue.joinFor("F0") == null && DungeonRequeue.joinFor(null) == null, "never guess");
        Cfg cfg = new Cfg(new File("cfg/requeue-test")); cfg.load();
        ok(DungeonRequeue.savedCommand(cfg) == null, "unknown floor no command");
        cfg.setInt("dungeonRequeue.floor", 3); cfg.setBool("dungeonRequeue.master", false);
        ok("/joindungeon 3".equals(DungeonRequeue.savedCommand(cfg)), "saved normal");
        cfg.setBool("dungeonRequeue.master", true);
        ok("/joindungeon 3 master".equals(DungeonRequeue.savedCommand(cfg)), "saved master");
        cfg.setBool(Cfg.key("DU002"), false);
        ok(DungeonRequeue.savedCommand(cfg) == null, "disabled");
        cfg.save(); Cfg reloaded = new Cfg(new File("cfg/requeue-test")); reloaded.load();
        reloaded.setBool(Cfg.key("DU002"), true);
        ok("/joindungeon 3 master".equals(DungeonRequeue.savedCommand(reloaded)), "persists");
        System.out.println("  Dungeon requeue: exact floor, normal/master commands, unknown guard and persistence OK");
    }
}
