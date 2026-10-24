import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import com.sweatsenhancements.feature.PanelLines;
import com.sweatsenhancements.core.RichText;

/**
 * The slayer block, in the shapes a real screen showed it in.
 *
 * The block was claimed by position - "the two lines after Slayer Quest" - so when the server put
 * its own address line inside the block, the quest's boss and progress were thrown out as
 * unrecognised. These cases are the ones /sweatsenhancements elements printed from the game, plus the orders the
 * server uses on other islands.
 */
public class SlayerTest {

    static void wipe(File dir) {
        File[] kids = dir.listFiles();
        if (kids != null) {
            for (File kid : kids) {
                if (kid.isDirectory()) wipe(kid); else kid.delete();
            }
        }
        dir.delete();
    }

    private static void show(String title, String... lines) {
        wipe(new File("cfg-slayer"));
        SweatsEnhancements.cfg = new Cfg(new File("cfg-slayer"));
        List<PanelLines.Raw> sidebar = new ArrayList<PanelLines.Raw>();
        for (String line : lines) sidebar.add(new PanelLines.Raw(line, 10, false));
        System.out.println("---- " + title);
        for (String line : lines) System.out.println("   in : " + line.replace('\u00a7', '&'));
        for (PanelLines.Row row : PanelLines.build(sidebar, null, null,
                java.util.Collections.<String>emptySet())) {
            System.out.println("   out: " + row.slot + "  " + row.text.replace('\u00a7', '&'));
        }
    }

    public static void main(String[] args) throws Exception {
        show("as the game printed it - the address line inside the block",
                "\u00a7f\u00a7lSKYBLOCK",
                "\u00a7fPurse: \u00a762,961,030",
                "\u00a7fSlayer Quest",
                "\u00a7emc.fakepixel.me",
                "\u00a7e(2,070/6,000) Combat",
                "\u00a7eRevenant Horror V");

        show("the order the dumps carry",
                "\u00a7fSlayer Quest",
                "\u00a75Revenant Horror V",
                "\u00a77(\u00a7e1,990\u00a77/\u00a7c6,000\u00a77) Combat",
                "\u00a7emc.fakepixel.me");

        show("a different island's quest, and a line after the block",
                "\u00a7fSlayer Quest",
                "\u00a7dTarantula Broodfather VIII",
                "\u00a77(\u00a7e1,000\u00a77/\u00a7c5,000\u00a77) Spider Slayer",
                "\u00a7fPurse: \u00a762,000");
    }
}
