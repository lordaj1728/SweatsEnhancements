import com.sweatsenhancements.feature.SlayerNefPort;
public class SlayerNefPortTest {
 private static void ok(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args){
  ok("Revenant Horror".equals(SlayerNefPort.bossName("§cRevenant Horror 500k❤")),"NEF boss label");
  ok("Sven Alpha".equals(SlayerNefPort.miniName("§cSven Alpha 20k❤")),"NEF miniboss label");
  ok(SlayerNefPort.bossName("Bonzo 2m❤").isEmpty(),"dungeon boss not slayer");
  ok(SlayerNefPort.miniName("Party > Sven Alpha is here").equals("Sven Alpha"),"label classifier only; never apply to chat");
  ok("ASHEN".equals(SlayerNefPort.attunement("§8ASHEN ❤")),"attunement label");
  ok(SlayerNefPort.pillarSeconds("3s 4 hits")==3,"NEF fire pillar format");
  ok(SlayerNefPort.pillarSeconds("fake pillar")==-1,"no invented pillar");
  System.out.println("NEF Slayer label/attunement/pillar classifiers OK (offline)");
 }
}
