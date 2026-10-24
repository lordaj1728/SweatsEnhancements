import com.sweatsenhancements.feature.BlankGlassTooltips;
public class BlankGlassTooltipsTest {
    public static void main(String[] args) {
        if (!BlankGlassTooltips.blankName(" ") || !BlankGlassTooltips.blankName("§r ")
                || BlankGlassTooltips.blankName("Glass Pane") || BlankGlassTooltips.blankName(null))
            throw new AssertionError("blank-name filter must leave meaningful names intact");
        System.out.println("Blank pane name guard OK (item/event logic needs in-game check)");
    }
}
