import com.sweatsenhancements.feature.SlayerBasics;
public class SlayerBasicsTest {
  private static void ok(boolean b,String msg) { if (!b) throw new AssertionError(msg); }
  public static void main(String[] args) {
    ok(SlayerBasics.isBossRow("§cSlay the boss!"),"colored boss row");
    ok(!SlayerBasics.isBossRow("Go slay the boss!"),"avoid unrelated text");
    ok(SlayerBasics.isSlayerQuestRow("§6Slayer Quest"),"quest marker");
    ok(SlayerBasics.shieldSpam("Your hit was reduced by Hellion Shield!"),"exact shield line");
    ok(SlayerBasics.shieldSpam("You need a matching attunement on your dagger!"),"dagger line");
    ok(!SlayerBasics.shieldSpam("Party > Hellion Shield is useful"),"leave player chat alone");
    System.out.println("Slayer sidebar and shield classifiers OK (offline)");
  }
}
