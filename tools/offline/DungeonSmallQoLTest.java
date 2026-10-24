import com.sweatsenhancements.feature.DungeonSmallQoL;
import com.sweatsenhancements.feature.DungeonControlShape;
import net.minecraft.util.EnumFacing;
import java.util.Arrays;
public class DungeonSmallQoLTest {
    static void ok(boolean b, String message) { if (!b) throw new AssertionError(message); }
    public static void main(String[] args) {
        ok(DungeonSmallQoL.signError("Unable to locate sign"), "sign filter");
        ok(DungeonSmallQoL.signError("Unable to locate sign at 1, 2, 3"), "positioned sign filter");
        ok(!DungeonSmallQoL.signError("Player: Unable to locate sign"), "leave player chat");
        ok("Wither".equals(DungeonSmallQoL.obtainedKey("[VIP+] aditbro has obtained Wither Key!")), "real wither pickup");
        ok("Blood".equals(DungeonSmallQoL.obtainedKey("fake_h4x has obtained Blood Key!")), "real blood pickup");
        ok("Wither".equals(DungeonSmallQoL.obtainedKey("A Wither Key was picked up!")), "automatic Wither pickup from user dump");
        ok("Blood".equals(DungeonSmallQoL.obtainedKey("A Blood Key was picked up!")), "automatic Blood pickup");
        ok(DungeonSmallQoL.obtainedKey("Party > player: A Wither Key was picked up!") == null, "do not parse quoted automatic pickup");
        ok(DungeonSmallQoL.obtainedKey("[VIP+] aditbro has obtained Superboom TNT!") == null, "not unrelated pickup");
        ok(DungeonSmallQoL.obtainedKey("Party > fake_h4x: aditbro has obtained Wither Key!") == null, "not player chat");
        ok(DungeonSmallQoL.normalMobHp("Zombie 23k❤"), "ordinary mob");
        ok(!DungeonSmallQoL.normalMobHp("✮ Zombie 23k❤"), "keep starred mob");
        ok(!DungeonSmallQoL.normalMobHp("Bonzo 10k❤"), "keep boss label");
        ok(DungeonSmallQoL.terminalTitle("Click in order!"), "terminal title");
        ok(!DungeonSmallQoL.terminalTitle("Dungeon Reward Chest"), "keep rewards");
        ok(DungeonSmallQoL.keyCount(Arrays.asList("Catacombs (F3)", "Keys: 2x")) == 2, "key sidebar");
        ok(DungeonSmallQoL.keyCount(Arrays.asList("Catacombs (F3)")) == -1, "unknown key");
        ok(DungeonControlShape.bounds(EnumFacing.NORTH,.5f)[2] == .5f, "NEF north face");
        ok(DungeonControlShape.bounds(EnumFacing.EAST,.25f)[3] == .25f, "NEF east face");
        System.out.println("Dungeon QoL classifiers and NEF-oriented controls OK (offline)");
    }
}
