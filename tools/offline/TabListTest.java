import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import com.sweatsenhancements.feature.CustomTabList;

/**
 * The tab list as it will be drawn, without a client.
 *
 * The list is read the way Minecraft sends it: in columns of twenty entries, the first of which is
 * that column's title (SkyHanni's `parseComponentColumns`). This test feeds it columns in that shape
 * - title, lines, blank separators, the server's padding - and prints the columns that would be
 * drawn, so the grouping can be checked without the game.
 *
 * Give it a dump and it also runs the rows of that dump through the same reader.
 */
public class TabListTest {

    static void wipe(File dir) {
        File[] kids = dir.listFiles();
        if (kids != null) {
            for (File kid : kids) {
                if (kid.isDirectory()) wipe(kid); else kid.delete();
            }
        }
        dir.delete();
    }

    private static CustomTabList fresh() {
        wipe(new File("cfg-tab"));
        SweatsEnhancements.cfg = new Cfg(new File("cfg-tab"));
        return new CustomTabList();
    }

    @SuppressWarnings("unchecked")
    private static List<Object> columns(CustomTabList list, List<String> entries) throws Exception {
        // readColumns needs a client, so the entries are laid out here exactly as it lays them out:
        // blocks of twenty, first entry a title, blanks kept as separators
        java.lang.reflect.Field read = CustomTabList.class.getDeclaredField("prepared");
        read.setAccessible(true);
        java.lang.reflect.Method combine = CustomTabList.class.getDeclaredMethod(
                "combine", Class.forName("java.util.List"), int.class);
        combine.setAccessible(true);

        List<Object> serverColumns = new ArrayList<Object>();
        Class<?> rawClass = Class.forName("com.sweatsenhancements.feature.CustomTabList$RawColumn");
        java.lang.reflect.Constructor<?> ctor = rawClass.getDeclaredConstructor();
        ctor.setAccessible(true);
        for (int entry = 0; entry < entries.size(); entry += 20) {
            Object column = ctor.newInstance();
            java.lang.reflect.Field titleField = rawClass.getDeclaredField("title");
            titleField.setAccessible(true);
            java.lang.reflect.Field sectionsField = rawClass.getDeclaredField("sections");
            sectionsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<List<String>> sections = (List<List<String>>) sectionsField.get(column);
            if (entry == 0 || true) {
                titleField.set(column, entries.get(entry));
            }
            List<String> section = new ArrayList<String>();
            for (int i = entry + 1; i < Math.min(entry + 20, entries.size()); i++) {
                String line = entries.get(i);
                if (line.isEmpty()) {
                    if (!section.isEmpty()) { sections.add(section); section = new ArrayList<String>(); }
                    continue;
                }
                if ("MAX LEVEL".equals(line)) { section.add(line); continue; }
                section.add(line);
            }
            if (!section.isEmpty()) sections.add(section);
            serverColumns.add(column);
        }
        return (List<Object>) combine.invoke(null, serverColumns, 22);
    }

    private static void show(String title, List<String> entries) throws Exception {
        CustomTabList list = fresh();
        System.out.println("---- " + title);
        int i = 0;
        for (Object column : columns(list, entries)) {
            java.lang.reflect.Field lines = column.getClass().getDeclaredField("lines");
            lines.setAccessible(true);
            java.lang.reflect.Field titleField = column.getClass().getDeclaredField("title");
            titleField.setAccessible(true);
            System.out.println("   column " + (++i) + " title=" + titleField.get(column));
            for (Object line : (List<?>) lines.get(column)) {
                System.out.println("      | " + line.toString().replace('\u00a7', '&'));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void privateIslandSelf() throws Exception {
        java.lang.reflect.Method read = CustomTabList.class.getDeclaredMethod(
                "readColumns", List.class, java.util.Set.class, String.class);
        read.setAccessible(true);
        List<String> rows = new ArrayList<String>(Arrays.asList(
                "Island (4)", "[MVP++] aditbro", "Piet (Offline 35d+)", "", "Server Info"));
        java.util.Set<String> players = new java.util.HashSet<String>(Arrays.asList("aditbro"));
        List<?> cols = (List<?>) read.invoke(null, rows, players, "aditbro");
        java.lang.reflect.Field sections = cols.get(0).getClass().getDeclaredField("sections");
        sections.setAccessible(true);
        int visible = 0;
        for (List<String> section : (List<List<String>>) sections.get(cols.get(0))) {
            for (String line : section) if (line.contains("aditbro")) visible++;
        }
        if (visible != 1) throw new AssertionError("private island own name: " + visible + " (want 1)");
        System.out.println("  private island own name: " + visible + " (want 1)");
        rows.set(1, "[217] aditbro");
        rows.set(2, "[MVP++] aditbro");
        cols = (List<?>) read.invoke(null, rows, players, "aditbro");
        visible = 0;
        for (List<String> section : (List<List<String>>) sections.get(cols.get(0))) {
            for (String line : section) if (line.contains("aditbro")) visible++;
        }
        if (visible != 1) throw new AssertionError("roster + rank copies: " + visible + " (want 1)");
        System.out.println("  roster and rank own-name copies: " + visible + " (want 1)");
        List<String> spread = new ArrayList<String>();
        spread.add("Players (2)");
        spread.add("[217] aditbro");
        spread.add("[46] PlayerB");
        while (spread.size() < 20) spread.add("");
        spread.add("[VIP] PlayerB"); // at the first slot of another column
        spread.add("[VIP] PlayerC"); // not on the roster; must not drift over effects
        spread.add("Active Effects: 28");
        java.util.Set<String> both = new java.util.HashSet<String>(Arrays.asList("aditbro", "playerb", "playerc"));
        cols = (List<?>) read.invoke(null, spread, both, "aditbro");
        visible = 0;
        int stray = 0;
        for (Object col : cols) {
            for (List<String> section : (List<List<String>>) sections.get(col)) {
                for (String line : section) {
                    if (line.contains("PlayerB")) visible++;
                    if (line.contains("PlayerC")) stray++;
                }
            }
        }
        if (visible != 1 || stray != 0) throw new AssertionError("roster copies " + visible + ", stray ranks " + stray);
        System.out.println("  cross-column roster copies: " + visible + " (want 1); stray ranks: " + stray + " (want 0)");
    }

    public static void main(String[] args) throws Exception {
        privateIslandSelf();
        show("this server's own columns: a stat block under its title, blanks between groups",
                Arrays.asList(
                        "\u00a7a\u00a7lPlayers \u00a7f(3)",
                        "\u00a78[\u00a7b216\u00a78] \u00a7aaditbro \u00a7a\u2726",
                        "\u00a7a\u00a7lServer Info",
                        "\u00a7b\u00a7lArea: \u00a77Village",
                        "\u00a7fServer: \u00a78skyblock-3",
                        "\u00a7fGems: \u00a7a17",
                        "",
                        "\u00a7a\u00a7lSkills:",
                        "\u00a7fMining 60: \u00a7b100%",
                        "\u00a7fCombat 60: \u00a7b100%",
                        "",
                        "\u00a7e\u00a7lPet:",
                        "\u00a7f\u00a77[Lvl 100] \u00a76Baby Yeti",
                        "MAX LEVEL"));

        show("a pet at max level, the effects block, and the server's padding rows",
                Arrays.asList(
                        "\u00a7a\u00a7lActive Effects: \u00a7e28",
                        "\u00a7d\u00a7lCookie Buff\u00a78: \u00a7cINACTIVE",
                        "",
                        "\u00a7e\u00a7lProfile: \u00a7aCucumber",
                        "\u00a7fSB Level: \u00a78[\u00a7b216\u00a78] \u00a7b18\u00a73/\u00a7b100 XP",
                        "",
                        "\u00a7e\u00a7lEvent: \u00a7f\u00a7bElection Booth Opens!",
                        "\u00a7fStarts in: \u00a7e9 hours"));
    }
}
