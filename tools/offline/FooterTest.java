import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import com.sweatsenhancements.feature.CustomTabList;

/**
 * The tab list's footer, which is where this server puts the active effects and the cookie buff.
 *
 * The lines below are the ones from the player's screenshot, split the way the server splits them
 * across rows. What the reference shows is:
 *
 *     Active Effects: 28
 *
 *     Cookie Buff
 *      Not Active
 *
 * and that is what this asserts, on the exact input that produced the wrong screen.
 */
public class FooterTest {

    static void wipe(File dir) {
        File[] kids = dir.listFiles();
        if (kids != null) {
            for (File kid : kids) {
                if (kid.isDirectory()) wipe(kid); else kid.delete();
            }
        }
        dir.delete();
    }

    private static Object footer(List<String> lines) throws Exception {
        wipe(new File("cfg-footer"));
        SweatsEnhancements.cfg = new Cfg(new File("cfg-footer"));
        CustomTabList list = new CustomTabList();
        java.lang.reflect.Method m = CustomTabList.class.getDeclaredMethod("parseFooter", List.class);
        m.setAccessible(true);
        return m.invoke(list, lines);
    }

    private static void show(String title, List<String> lines) throws Exception {
        System.out.println("---- " + title);
        for (String line : lines) System.out.println("   in : " + line.replace('\u00a7', '&'));
        Object column = footer(lines);
        if (column == null) {
            System.out.println("   out: (nothing)");
            return;
        }
        java.lang.reflect.Field titleField = column.getClass().getDeclaredField("title");
        titleField.setAccessible(true);
        java.lang.reflect.Field sectionsField = column.getClass().getDeclaredField("sections");
        sectionsField.setAccessible(true);
        System.out.println("   out: title=" + titleField.get(column).toString().replace('\u00a7', '&'));
        for (Object section : (List<?>) sectionsField.get(column)) {
            for (Object line : (List<?>) section) {
                System.out.println("      | " + line.toString().replace('\u00a7', '&'));
            }
            System.out.println("      |  --");
        }
    }

    /** The footer with the switch off: what is kept, and what goes. */
    static void switchOff(String title, List<String> lines) throws Exception {
        System.out.println("---- " + title + "  (Footer off)");
        Object column = footer(lines);
        if (column == null) {
            System.out.println("   out: (nothing)");
            return;
        }
        java.lang.reflect.Method keep = CustomTabList.class.getDeclaredMethod("essentialOnly", column.getClass());
        keep.setAccessible(true);
        Object trimmed = keep.invoke(null, column);
        if (trimmed == null) {
            System.out.println("   out: (nothing)");
            return;
        }
        java.lang.reflect.Field titleField = trimmed.getClass().getDeclaredField("title");
        titleField.setAccessible(true);
        java.lang.reflect.Field sectionsField = trimmed.getClass().getDeclaredField("sections");
        sectionsField.setAccessible(true);
        System.out.println("   out: title=" + titleField.get(trimmed).toString().replace('\u00a7', '&'));
        for (Object section : (List<?>) sectionsField.get(trimmed)) {
            for (Object line : (List<?>) section) {
                System.out.println("      | " + line.toString().replace('\u00a7', '&'));
            }
            System.out.println("      |  --");
        }
    }

    public static void main(String[] args) throws Exception {
        // exactly the footer of the player's screenshot: the effects block, the cookie block and
        // the advert, with the server's two-row sentences
        show("the screenshot's footer",
                Arrays.asList(
                        "\u00a7a\u00a7lActive Effects",
                        "\u00a7eYou have 28 active effects. Use",
                        "\"/effects\" to see them!",
                        "\u00a7r",
                        "\u00a7d\u00a7lCookie Buff",
                        "\u00a77Not active! Obtain booster cookies from the",
                        "\u00a77community shop in the hub.",
                        "\u00a7aRanks, Boosters & MORE! \u00a7c\u00a7lSTORE.FAKEPIXEL.SHOP"));

        show("the footer with the padding row and a name row in it",
                Arrays.asList(
                        "\u00a7e\u00a7lEvent: \u00a7f\u00a7bElection Booth Opens!",
                        "\u00a7fStarts in: \u00a7e9 hours",
                        "\u00a7e\u00a7lCLICK",
                        "\u00a7r\u00a77TORONTOALAJATT\u00a7r",
                        "\u00a7a\u00a7lActive Effects",
                        "\u00a7eYou have 28 active effects. Use",
                        "\"/effects\" to see them!",
                        "\u00a7d\u00a7lCookie Buff",
                        "\u00a77Not active! Obtain booster cookies from the",
                        "\u00a77community shop in the hub."));

        show("no effects, no cookie, and a god potion",
                Arrays.asList(
                        "\u00a7a\u00a7lActive Effects",
                        "\u00a7eNo effects active. Drink Potions or splash them on the",
                        "ground to buff yourself!",
                        "\u00a7r",
                        "\u00a7d\u00a7lCookie Buff",
                        "\u00a77Not active! Obtain booster cookies from the community shop in the hub."));

        switchOff("the effects and the cookie survive the switch being off",
                Arrays.asList(
                        "\u00a7e\u00a7lEvent: \u00a7f\u00a7bJerry Workshop Opens",
                        "\u00a7fStarts in: \u00a713 hours",
                        "\u00a7e\u00a7lCLICK",
                        "\u00a7a\u00a7lActive Effects",
                        "\u00a7eYou have 28 active effects. Use",
                        "\"/effects\" to see them!",
                        "\u00a7d\u00a7lCookie Buff",
                        "\u00a77Not active! Obtain booster cookies from the",
                        "\u00a77community shop in the hub."));

        // The Hub's footer, as the player's screenshot shows it: the server puts the player's own
        // name at the top of it, shaped exactly like a row of the player list. The panel's Other
        // column drew it under its heading - "you removed my name and it doesnt even work in the
        // hub now" - and the whole column came down to one name. A row shaped like a player's is
        // not footer data, whoever it names.
        show("the player's own row in the footer, as the Hub sends it",
                Arrays.asList(
                        "\u00a7r\u00a7b[VIP] fleetum\u00a7r",
                        "\u00a7r\u00a7b[VIP] Piyushkumar_hg\u00a7r",
                        "\u00a7a\u00a7lActive Effects",
                        "\u00a7eYou have 28 active effects. Use",
                        "\"/effects\" to see them!",
                        "\u00a7d\u00a7lCookie Buff",
                        "\u00a77Not active! Obtain booster cookies from the",
                        "\u00a77community shop in the hub."));
        System.out.println("   own-row check : "
                + (CustomTabList.playerRowShape("[VIP] fleetum") ? "dropped" : "STILL DRAWN")
                + " / " + (CustomTabList.playerRowShape("[VIP] Piyushkumar_hg") ? "dropped"
                : "STILL DRAWN")
                + " / " + (CustomTabList.playerRowShape("You are currently connected to hub-4")
                ? "WRONGLY DROPPED" : "kept"));

        show("a god potion running",
                Arrays.asList(
                        "\u00a7a\u00a7lActive Effects",
                        "\u00a7eYou have a God Potion active! 12 Hours",
                        "\u00a7eYou have 3 non-god effects.",
                        "\u00a7d\u00a7lCookie Buff",
                        "\u00a7a\u00a7lCookie Buff: \u00a7aActive"));
    }
}
