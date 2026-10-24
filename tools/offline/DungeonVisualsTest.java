import com.sweatsenhancements.feature.DungeonBlessings;
import com.sweatsenhancements.feature.HudLayout;
import java.util.Map;
import java.util.List;

public final class DungeonVisualsTest {
    static void ok(boolean v, String label) { if (!v) throw new AssertionError(label); }
    public static void main(String[] args) {
        String footer = "Dungeon Buffs\n§4Blessing of Power VI\n§6Blessing of Time V\n§bBlessing of Wisdom VII\n§7Blessing of Stone III\n§2Blessing of Life II";
        Map<String,Integer> levels = DungeonBlessings.levels(footer);
        ok(levels.size() == 5 && levels.get("power") == 6 && levels.get("wisdom") == 7
                && levels.get("stone") == 3 && levels.get("time") == 5 && levels.get("life") == 2,
                "all five blessings from tab footer");
        List<HudLayout.Line> display = DungeonBlessings.format(levels, false);
        ok(display.size() == 5 && display.get(0).text.contains("Power§f: §a6")
                && display.get(1).text.contains("Time§f: §a5")
                && display.get(4).text.contains("Life§f: §a2"), "all five displayed in stable order");
        ok(DungeonBlessings.roman("VIII") == 8 && DungeonBlessings.roman("7") == 7, "roman and decimal input");
        ok("VIII".equals(DungeonBlessings.formatLevel(8, true)), "optional roman output");
        ok(DungeonBlessings.format(DungeonBlessings.levels("Blessing of Stone III"), false).size() == 1,
                "only observed nonzero blessings displayed");
        System.out.println("  Dungeon visuals: all five blessings, order, colours and Roman levels OK");
    }
}
