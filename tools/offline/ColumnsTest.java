import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import com.sweatsenhancements.feature.CustomTabList;

/**
 * The reader that draws, run on a real dump.
 *
 * `readColumns` used to read the client directly, which is why every tab-list question so far has
 * been answered by guessing at the reader instead of running it. It takes the rows now - the display
 * name, or the profile name when the server set no display name, which is how this server sends its
 * `CLICK` padding and its filler names - so a dump's tab list can go through the reader that draws.
 *
 * The rows are sorted the way the client sorts them (team, then name). A dump does not carry the
 * teams, so the profile name is used, which is what the teams are built from here.
 */
public class ColumnsTest {

    static void wipe(File dir) {
        File[] kids = dir.listFiles();
        if (kids != null) {
            for (File kid : kids) {
                if (kid.isDirectory()) wipe(kid); else kid.delete();
            }
        }
        dir.delete();
    }

    /** The rows of the dump's tab list, as the client would read them: name, then the drawn text. */
    static List<String[]> rows(File dump) throws Exception {
        List<String[]> rows = new ArrayList<String[]>();
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(dump), "UTF-8"));
        boolean inTab = false;
        String line;
        while ((line = in.readLine()) != null) {
            if (line.startsWith("=== TAB LIST ===")) { inTab = true; continue; }
            if (line.startsWith("===")) { inTab = false; continue; }
            if (!inTab || !DumpRows.looksLikeRow(line)) continue;
            String[] parts = DumpRows.of(line);
            if (parts == null) continue;
            rows.add(parts);
        }
        in.close();
        return rows;
    }

    static void show(String title, File dump, List<String[]> rows, Set<String> players) throws Exception {
        show(title, dump, rows, players, null);
    }

    static Object show(String title, File dump, List<String[]> rows, Set<String> players,
                       String self) throws Exception {
        System.out.println("---- " + title + "  (" + rows.size() + " rows)");
        List<String[]> sorted = new ArrayList<String[]>(rows);
        Collections.sort(sorted, new java.util.Comparator<String[]>() {
            public int compare(String[] a, String[] b) {
                return a[0].compareToIgnoreCase(b[0]);
            }
        });
        List<String> displays = new ArrayList<String>();
        for (String[] row : sorted) displays.add(row[1]);

        Method read = CustomTabList.class.getDeclaredMethod(
                "readColumns", List.class, java.util.Set.class, String.class);
        read.setAccessible(true);
        Object result = read.invoke(null, displays, players, self);
        List<?> columns = (List<?>) result;
        System.out.println("     drawn as " + columns.size() + " column(s), "
                + CustomTabList.readEntries + " rows read, "
                + CustomTabList.droppedEntries + " dropped as padding or adverts");
        for (Object column : columns) {
            Field titleField = column.getClass().getDeclaredField("title");
            titleField.setAccessible(true);
            Field sectionsField = column.getClass().getDeclaredField("sections");
            sectionsField.setAccessible(true);
            String columnTitle = (String) titleField.get(column);
            System.out.println("  column title="
                    + (columnTitle.isEmpty() ? "(none)" : columnTitle.replace('\u00a7', '&')));
            for (Object section : (List<?>) sectionsField.get(column)) {
                for (Object text : (List<?>) section) {
                    System.out.println("      | " + text.toString().replace('\u00a7', '&'));
                }
                System.out.println("      |  --");
            }
        }
        return columns;
    }

    public static void main(String[] args) throws Exception {
        wipe(new File("cfg-columns"));
        SweatsEnhancements.cfg = new Cfg(new File("cfg-columns"));

        List<File> dumps = new ArrayList<File>();
        for (String arg : args) dumps.add(new File(arg));
        if (dumps.isEmpty()) dumps.add(new File("../../uploads/dump-20260924-014112.txt"));
        for (File dump : dumps) {
            if (!dump.exists()) {
                System.out.println("---- no such dump: " + dump);
                continue;
            }
            List<String[]> rows = rows(dump);
            // the server's padding has no name behind it, so nothing here is a known player: the
            // name-shaped rows are dropped on their shape alone, which is the stricter case
            show(dump.getName() + " (no known players)", dump, rows, new HashSet<String>());
            Set<String> players = new HashSet<String>();
            for (String[] row : rows) {
                if (row[0].matches("^[A-Za-z0-9_]{3,16}$")) {
                    players.add(row[0].toLowerCase(java.util.Locale.ROOT));
                }
            }
            show(dump.getName() + " (every name-shaped row treated as a player)", dump, rows, players);

            // ---- the live case the player reported in the Dungeon Hub: "i saw my name again in the
            // tablist or npc names in the tablist which shouldnt be there". The real players are
            // known here and the player at the keyboard is named, so their own row and an NPC's row
            // both have to go.
            List<String[]> withSelf = new ArrayList<String[]>(rows);
            withSelf.add(new String[] { "Mort", "\u00a78[\u00a7b217\u00a78] \u00a76Mort \u00a77\u14dd" });
            withSelf.add(new String[] { "aditbro", "\u00a7r\u00a76[MVP++\u00a7r\u00a76] aditbro\u00a7r" });
            Set<String> real = new HashSet<String>();
            real.add("aditbro");
            real.add("sk212");
            show(dump.getName() + " (the player named, the NPC and the self row injected)",
                    dump, withSelf, real, "aditbro");

            // The Dungeon Hub's own shape: the roster rows this server writes inside level
            // brackets, the player's own among them, and the shop links it stands at the bottom
            // of a column in capitals. None of the four may be drawn: two are players the client
            // holds and stay, the other two are furniture.
            List<String[]> hub = new ArrayList<String[]>(withSelf);
            hub.add(new String[] { "\u0002", "\u00a78[\u00a7b216\u00a78] \u00a7bpowerkill \u00a76\u2c58 \u00a7f\u23f8" });
            hub.add(new String[] { "\u0003", "\u00a78[\u00a7b118\u00a78] \u00a7fMOHNISHISHERE" });
            hub.add(new String[] { "\u0004", "\u00a7e\u00a7lGATE KEEPER" });
            hub.add(new String[] { "\u0005", "\u00a7e\u00a7lSHOP" });
            // a pet and its MAX LEVEL marker, as this server writes them, so the check does not
            // depend on which dump is being read
            // the player at the keyboard, as the roster writes them: their own row sits beside the
            // rank row the client draws for the same person, which is what makes the rank row a
            // duplicate rather than their only row
            hub.add(new String[] { "\u0008", "\u00a78[\u00a7b217\u00a78] \u00a76aditbro \u00a77\u2c58" });
            hub.add(new String[] { "\u0006", "\u00a7f\u00a77[Lvl 100] \u00a76Baby Yeti" });
            hub.add(new String[] { "\u0007", "\u00a77MAX LEVEL" });
            Set<String> roster = new HashSet<String>();
            roster.add("aditbro");
            roster.add("sk212");
            roster.add("powerkill");
            roster.add("mohnishishere");
            Object columns = show(dump.getName() + " (the Dungeon Hub's own shapes)", dump, hub, roster,
                    "aditbro");
            dropped(columns, "the shop links", "GATE KEEPER", "SHOP");
            kept(columns, "the roster", "powerkill", "MOHNISHISHERE");
            // The player's own name belongs in the roster - "my name not visible in players" is the
            // report that put it back, and their screenshot shows it as one of the six - so the
            // roster row for the player at the keyboard stays. What is dropped is the *rank* row for
            // the same person, the client's own second copy of them, which is what turned up above
            // the active effects: the row injected above is "[MVP++] aditbro".
            kept(columns, "the player's own roster row", "aditbro");
            dropped(columns, "the player's own rank row", "MVP++");
            // the pet line is written like a roster row - a bracket, then a name - but the name is
            // two words and neither is a player. It is data, and the MAX LEVEL marker hangs off it:
            // the player's own photograph has `Pet:` with nothing under it.
            kept(columns, "the pet line", "Baby Yeti", "MAX LEVEL");
        }

        java.io.InputStream in = new java.io.FileInputStream(dumps.get(0));
        in.close();
    }

    /** Everything the reader produced, as one string, for the presence checks below. */
    static String drawn(Object columns) throws Exception {
        StringBuilder out = new StringBuilder();
        for (Object column : (List<?>) columns) {
            Field titleField = column.getClass().getDeclaredField("title");
            titleField.setAccessible(true);
            out.append(titleField.get(column)).append('\n');
            Field sectionsField = column.getClass().getDeclaredField("sections");
            sectionsField.setAccessible(true);
            for (Object section : (List<?>) sectionsField.get(column)) {
                for (Object text : (List<?>) section) out.append(text).append('\n');
            }
        }
        return out.toString();
    }

    static void dropped(Object columns, String what, String... needles) throws Exception {
        String text = drawn(columns);
        for (String needle : needles) {
            System.out.println("     " + (text.contains(needle) ? "STILL DRAWN" : "not drawn  ")
                    + "  " + what + ": " + needle);
        }
    }

    static void kept(Object columns, String what, String... needles) throws Exception {
        String text = drawn(columns);
        for (String needle : needles) {
            System.out.println("     " + (text.contains(needle) ? "kept       " : "MISSING    ")
                    + "  " + what + ": " + needle);
        }
    }
}
