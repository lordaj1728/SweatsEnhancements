import com.sweatsenhancements.feature.nef.MinibossAlert;
public class NEFDirectPortTest {
 private static void ok(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] a){
  ok(MinibossAlert.isSpawnSound("random.explode",.6f,9/7f),"NEF S29 miniboss cue");
  ok(!MinibossAlert.isSpawnSound("random.explode",1f,9/7f),"unrelated explosion volume");
  ok(!MinibossAlert.isSpawnSound("random.orb",.6f,9/7f),"no recursive sound");
  System.out.println("Direct NEF S29 miniboss cue predicate OK (offline; packet mixin not live-tested)");
 }
}
