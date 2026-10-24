import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import com.sweatsenhancements.feature.CustomScoreboard;
import com.sweatsenhancements.feature.PanelLines;

/**
 * The config menu's hitch, measured.
 *
 * The report: "everytime i open the config gui i get stutters every second". Two causes, and this
 * measures both:
 *
 *   * `Cfg.set` wrote the config file on **every call**, and the menu's mirror calls it for every
 *     option - hundreds of write/delete/rename cycles per pass, twice a second while the menu was
 *     open. The first half of this test lays that out: how many file writes a menu pass costs now,
 *     and how long a pass takes.
 *   * The scoreboard rebuilt its rows twenty times a second whether or not anything changed. The
 *     second half times one rebuild over a real dump, which is the work a skipped rebuild saves.
 *
 * Nothing here asserts a count of lines; it reports writes, milliseconds and fingerprints, so a
 * regression is visible as a number rather than as a hunch.
 */
public class PerfTest {

    static void wipe(File dir) {
        File[] kids = dir.listFiles();
        if (kids != null) {
            for (File kid : kids) {
                if (kid.isDirectory()) wipe(kid); else kid.delete();
            }
        }
        dir.delete();
    }

    /** The menu's mirror, in miniature: one set() call per option, all of them unchanged. */
    static void mirror(Cfg cfg) {
        for (int i = 0; i < 600; i++) {
            cfg.set("opt." + i, "value-" + i);
        }
    }

    public static void main(String[] args) throws Exception {
        File dir = new File("cfg-perf");
        wipe(dir);
        File home = new File(dir, "perf");
        Cfg cfg = new Cfg(home);
        SweatsEnhancements.cfg = cfg;

        // the first write, so the file exists and every later count is a change
        cfg.set("seed", "1");
        cfg.save();
        int baseline = Cfg.writes;

        System.out.println("---- the menu's mirror, unchanged");
        long t0 = System.nanoTime();
        for (int pass = 0; pass < 100; pass++) mirror(cfg);
        long ns = System.nanoTime() - t0;
        System.out.println("  100 passes x 600 options, " + ((ns) / 1_000_000L) + " ms total ("
                + (ns / 100 / 600) + " ns per option)");
        System.out.println("  file writes caused : " + (Cfg.writes - baseline));

        System.out.println("---- one option actually changes");
        cfg.set("opt.7", "changed");
        System.out.println("  dirty after the change : " + cfg.isDirty());
        System.out.println("  file writes before the flush : " + (Cfg.writes - baseline));
        cfg.flushIfDirty(0L);
        System.out.println("  file writes after the flush  : " + (Cfg.writes - baseline));

        System.out.println("---- and then 100 more passes with nothing changed");
        for (int pass = 0; pass < 100; pass++) mirror(cfg);
        cfg.flushIfDirty(0L);
        System.out.println("  file writes total : " + (Cfg.writes - baseline)
                + "   (was one per option per pass before)");

        System.out.println("---- the value survived the round trip");
        Cfg reread = new Cfg(home);
        reread.load();
        System.out.println("  opt.7 on disk : " + reread.get("opt.7", "(missing)")
                + "   (the last mirror put its own value back, which is a real change and the one"
                + " write counted above)");
        System.out.println("  keys on disk  : " + reread.keys().size());

        System.out.println("---- a rebuild of the panel's rows, over a real dump");
        File dump = new File(args.length > 0 ? args[0] : "../../uploads/dump-20260924-014112.txt");
        List<PanelLines.Raw> sidebar = new ArrayList<PanelLines.Raw>();
        List<String> tabRaw = new ArrayList<String>();
        List<String> tabClean = new ArrayList<String>();
        boolean inTab = false;
        boolean inSide = false;
        boolean slot1 = false;
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(dump), "UTF-8"));
        String line;
        while ((line = in.readLine()) != null) {
            if (line.startsWith("=== TAB LIST ===")) { inTab = true; inSide = false; continue; }
            if (line.startsWith("=== SCOREBOARD ===")) { inSide = true; inTab = false; continue; }
            if (line.startsWith("===")) { inTab = false; inSide = false; continue; }
            if (inSide && line.contains("-- slot ")) {
                // slot 1 is the sidebar; slot 2 is the health under every name, which the panel
                // never reads and which would otherwise make this workload look bigger than it is
                slot1 = line.contains("-- slot 1 ");
                continue;
            }
            if (inTab) {
                if (!DumpRows.looksLikeRow(line)) continue;
                String[] parts = DumpRows.of(line);
                if (parts == null) continue;
                String raw = parts[1];
                if (raw.isEmpty()) continue;
                tabRaw.add(raw);
                tabClean.add(com.sweatsenhancements.core.RichText.strip(raw).trim());
            } else if (inSide && slot1 && line.trim().startsWith("[") && line.contains("] ")) {
                // the dump walks the sidebar entries in score order; the panel's reader sorts them,
                // and the sort is not what is being measured here - the rebuild is
                String text = line.trim().substring(line.trim().indexOf("] ") + 2);
                sidebar.add(new PanelLines.Raw(text, 0, false));
            }
        }
        in.close();

        int builds = 200;
        long t1 = System.nanoTime();
        int rows = 0;
        for (int i = 0; i < builds; i++) {
            rows = PanelLines.build(sidebar, tabRaw, tabClean, java.util.Collections.<String>emptySet()).size();
        }
        long perBuild = (System.nanoTime() - t1) / builds;
        System.out.println("  " + tabClean.size() + " tab rows, " + sidebar.size() + " sidebar rows -> "
                + rows + " drawn rows");
        System.out.println("  one rebuild: " + (perBuild / 1000L) + " us;  20 rebuilds a second = "
                + (perBuild * 20 / 1_000_000L) + " ms of every second");

        System.out.println("---- the fingerprint the rebuild is skipped on");
        int a = CustomScoreboard.rowFingerprint(sidebar, 1, 3, 2);
        int b = CustomScoreboard.rowFingerprint(sidebar, 1, 3, 2);
        int movedTab = CustomScoreboard.rowFingerprint(sidebar, 2, 3, 2);
        int movedCfg = CustomScoreboard.rowFingerprint(sidebar, 1, 3, 3);
        List<PanelLines.Raw> other = new ArrayList<PanelLines.Raw>(sidebar);
        other.add(new PanelLines.Raw("Purse: 1", 0, false));
        int movedSide = CustomScoreboard.rowFingerprint(other, 1, 3, 2);
        System.out.println("  same inputs            : " + (a == b ? "same" : "DIFFERENT"));
        System.out.println("  a new tab list line    : " + (a != movedTab ? "different" : "SAME"));
        System.out.println("  a setting changed      : " + (a != movedCfg ? "different" : "SAME"));
        System.out.println("  a new sidebar line     : " + (a != movedSide ? "different" : "SAME"));
        System.out.println("  a changed sidebar line : "
                + (a != CustomScoreboard.rowFingerprint(replacePurse(sidebar), 1, 3, 2) ? "different" : "SAME"));
    }

    static List<PanelLines.Raw> replacePurse(List<PanelLines.Raw> sidebar) {
        List<PanelLines.Raw> out = new ArrayList<PanelLines.Raw>(sidebar);
        out.add(new PanelLines.Raw("Purse: 52,763,737", 0, false));
        return out;
    }
}
