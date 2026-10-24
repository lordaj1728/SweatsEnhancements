import com.sweatsenhancements.feature.DungeonControlShape;
import net.minecraft.util.EnumFacing;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.spongepowered.asm.mixin.injection.Inject;
public class DungeonControlBoundsTest {
    static void ok(boolean b, String msg) { if (!b) throw new AssertionError(msg); }
    public static void main(String[] args) {
        // Mixin 0.7.11 rejects non-private static helper methods at game startup.
        for (Method m : com.sweatsenhancements.mixin.DungeonControlBounds.class.getDeclaredMethods())
            ok(!Modifier.isStatic(m.getModifiers()) || Modifier.isPrivate(m.getModifiers()),
                    "mixin static helper must be private: " + m.getName());
        // Runtime Forge jar has the SRG name, and this mixin ships no refmap.
        for (Method m : com.sweatsenhancements.mixin.DungeonControlBounds.class.getDeclaredMethods()) {
            Inject hook = m.getAnnotation(Inject.class);
            if (hook != null) {
                ok(hook.method().length == 1 && "func_180654_a".equals(hook.method()[0]), "runtime target name");
                ok(!hook.remap(), "no refmap is bundled");
            }
        }
        for (EnumFacing face : EnumFacing.values()) {
            for (float thick : new float[]{.25f,.5f}) {
                float[] b = DungeonControlShape.bounds(face,thick);
                ok(b != null && b.length == 6, "bounds for " + face);
                int axis = face.getAxis() == EnumFacing.Axis.X ? 0 : face.getAxis() == EnumFacing.Axis.Y ? 1 : 2;
                for (int i=0;i<3;i++) {
                    ok(b[i] >= 0 && b[i+3] <= 1 && b[i] < b[i+3], "bounded positive volume");
                    ok(Math.abs((b[i+3]-b[i])-(i==axis ? thick : 1)) < .001, "face-aligned slice");
                }
            }
        }
        ok(DungeonControlShape.bounds(EnumFacing.NORTH,.5f)[2] == .5f, "north attachment");
        ok(DungeonControlShape.bounds(EnumFacing.SOUTH,.25f)[5] == .25f, "south attachment");
        ok(DungeonControlShape.bounds(EnumFacing.WEST,.5f)[0] == .5f, "west attachment");
        ok(DungeonControlShape.bounds(EnumFacing.EAST,.25f)[3] == .25f, "east attachment");
        System.out.println("  NEF-oriented lever/button face slices remain within one block OK");
    }
}
