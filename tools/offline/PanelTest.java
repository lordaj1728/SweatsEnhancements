import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import com.sweatsenhancements.core.Numbers;
import com.sweatsenhancements.core.PanelSlots;
import com.sweatsenhancements.core.RichText;
import com.sweatsenhancements.core.TextList;
import com.sweatsenhancements.feature.PanelLines;

public class PanelTest {

    static void wipe(File dir) {
        File[] kids = dir.listFiles();
        if (kids != null) {
            for (File kid : kids) {
                if (kid.isDirectory()) wipe(kid); else kid.delete();
            }
        }
        dir.delete();
    }

    public static void main(String[] args) throws Exception {
        // A fresh config every run: the settings below are written to disk, so a second run would
        // otherwise draw the panel with the previous run's experiments still switched on.
        wipe(new File("cfg"));
        SweatsEnhancements.cfg = new Cfg(new File("cfg"));
        SweatsEnhancements.cfg.load();
        // 2026-09-28 in-game dump: Powder was in Appearance, Forges was not;
        // neither was in Select Tablist Lines. They therefore never reached the panel.
        SweatsEnhancements.cfg.set("scoreboard.appearance", "SERVER_DATE;TIME;SEASON;ISLAND;LOCATION;PROFILE;SB_LEVEL;PURSE;BANK;INTEREST;BITS;GEMS;COPPER;HEAT;NORTH_STARS;POWDER;COOKIE;POWER;TUNING;ELECTIONS;EVENT;SLAYER");
        SweatsEnhancements.cfg.set("scoreboard.tabLines", "BANK;INTEREST;GEMS;POWER;SB_LEVEL;PROFILE;EVENT;ELECTIONS");
        com.sweatsenhancements.config.Migrate.run(SweatsEnhancements.cfg);
        List<String> miningAppearance = TextList.split(SweatsEnhancements.cfg.get("scoreboard.appearance", ""), null);
        if (!miningAppearance.contains("DUNGEON_SIDEBAR")) {
            throw new AssertionError("dungeon element missing from saved appearance");
        }
        SweatsEnhancements.cfg.set("scoreboard.appearance", "LOCATION;PROFILE");
        com.sweatsenhancements.config.Migrate.run(SweatsEnhancements.cfg);
        if (TextList.split(SweatsEnhancements.cfg.get("scoreboard.appearance", ""), null).contains("DUNGEON_SIDEBAR")) {
            throw new AssertionError("explicit dungeon appearance removal was undone");
        }
        SweatsEnhancements.cfg.set("scoreboard.appearance", "");
        System.out.println("  dungeon element migrated once (want once); later removal respected");
        List<String> miningTabs = TextList.split(SweatsEnhancements.cfg.get("scoreboard.tabLines", ""), null);
        if (!miningAppearance.contains("POWDER") || !miningAppearance.contains("FORGES")
                || !miningTabs.contains("POWDER") || !miningTabs.contains("FORGES")) {
            throw new AssertionError("mining blocks missing after settings migration");
        }
        SweatsEnhancements.cfg.set("scoreboard.tabLines", "BANK;INTEREST");
        com.sweatsenhancements.config.Migrate.run(SweatsEnhancements.cfg);
        if (SweatsEnhancements.cfg.get("scoreboard.tabLines", "").contains("POWDER")) {
            throw new AssertionError("user removal must survive later starts");
        }
        System.out.println("  mining blocks offered by old config: Powder 1, Forges 1 (want 1 each); later removal respected");
        SweatsEnhancements.cfg.set("scoreboard.appearance", "");
        SweatsEnhancements.cfg.set("scoreboard.tabLines", "");

        for (String dump : args) {
            System.out.println("================ " + dump);
            List<String> lines = Files.readAllLines(Paths.get(dump), StandardCharsets.UTF_8);
            List<PanelLines.Raw> sidebar = new ArrayList<PanelLines.Raw>();
            List<String> tabRaw = new ArrayList<String>();
            List<String> tabClean = new ArrayList<String>();
            boolean inTab = false;
            boolean inSide = false;
            boolean inNames = false;
            java.util.Set<String> names = new java.util.HashSet<String>();
            for (String line : lines) {
                if (line.startsWith("=== SCOREBOARD ===")) { inSide = true; inTab = false; inNames = false; continue; }
                if (line.startsWith("=== TAB LIST ===")) { inTab = true; inSide = false; inNames = false; continue; }
                if (line.startsWith("===")) { inSide = false; inTab = false; inNames = false; continue; }
                if (line.contains("-- slot ")) {
                    // slot 1 is the sidebar, slot 2 is the health under every name: the names in
                    // slot 2 are this dump's list of the real players, which is what the tab list
                    // filter needs to tell a player row from a line the server made up
                    inNames = line.contains("-- slot 2 ");
                    inSide = line.contains("-- slot 1 ");
                    continue;
                }
                if (inNames) {
                    String t = line.trim();
                    if (!t.startsWith("[") || !t.contains("] ")) continue;
                    String name = t.substring(t.indexOf("] ") + 2).trim();
                    if (!name.isEmpty()) names.add(name.toLowerCase(java.util.Locale.ROOT));
                    continue;
                }
                if (inSide) {
                    String t = line.trim();
                    if (!t.startsWith("[") || !t.contains("] ")) continue;
                    int close = t.indexOf("] ");
                    int points = 0;
                    try { points = Integer.parseInt(t.substring(1, close)); } catch (Throwable ex) { continue; }
                    String text = t.substring(close + 2);
                    String clean = RichText.strip(text).trim();
                    sidebar.add(new PanelLines.Raw(text, points, clean.isEmpty()));
                } else if (inTab) {
                    if (!DumpRows.looksLikeRow(line)) continue;
                    // cut at the row's last field: the vote bars are '|' too, so the text cannot
                    // be split on the first one (see DumpRows)
                    String[] parts = DumpRows.of(line);
                    if (parts == null) continue;
                    String raw = parts[1];
                    if (raw.isEmpty()) continue;
                    tabRaw.add(raw);
                    tabClean.add(RichText.strip(raw).trim());
                }
            }
            System.out.println("sidebar lines: " + sidebar.size() + ", tab lines: " + tabRaw.size()
                    + ", players known: " + names.size());
            List<PanelLines.Row> rows = PanelLines.build(sidebar, tabRaw, tabClean, names);
            if (dump.contains("20260929-233716") || dump.contains("20260929-233857")) {
                int dungeonCount = 0;
                int mates = 0;
                int lastScore = Integer.MAX_VALUE;
                for (PanelLines.Row row : rows) {
                    if (!"DUNGEON_SIDEBAR".equals(row.slot)) continue;
                    dungeonCount++;
                    if (row.points > lastScore) throw new AssertionError("dungeon sidebar order reversed");
                    lastScore = row.points;
                    if (RichText.strip(row.text).trim().startsWith("[B]")) mates++;
                }
                for (PanelLines.Row row : rows) {
                    if (java.util.Arrays.asList("SEASON", "PROFILE", "BANK", "INTEREST", "GEMS", "EVENT")
                            .contains(row.slot)) throw new AssertionError("dungeon-only hide failed: " + row.slot);
                }
                System.out.println("  dungeon hidden: Season, Profile, Bank, Interest, Gems, Event");
                if (dungeonCount != 7 || mates != 4) {
                    throw new AssertionError("dungeon sidebar rows " + dungeonCount + ", teammates " + mates);
                }
                System.out.println("  dungeon sidebar rows: " + dungeonCount + " (want 7); teammate rows: " + mates + " (want 4)");
                SweatsEnhancements.cfg.setBool("scoreboard.dungeonSidebar", false);
                int hidden = 0;
                for (PanelLines.Row row : PanelLines.build(sidebar, tabRaw, tabClean, names)) {
                    if ("DUNGEON_SIDEBAR".equals(row.slot)) hidden++;
                }
                SweatsEnhancements.cfg.setBool("scoreboard.dungeonSidebar", true);
                if (hidden != 0) throw new AssertionError("dungeon switch off leaves rows");
                System.out.println("  dungeon rows with switch off: " + hidden + " (want 0)");
            }
            System.out.println("--- drawn (" + rows.size() + " rows)");
            for (PanelLines.Row row : rows) {
                System.out.println("    " + pad(row.slot) + " | " + row.text.replace('\u00a7', '&'));
            }
        }

        // End-of-run timer from the user's 2026-09-30 sidebar screenshot. It is
        // a sidebar row, not a tab-list field, and keeps its position below teammates.
        SweatsEnhancements.cfg.set("scoreboard.appearance", "SERVER_DATE;SEASON;LOCATION;DUNGEON_SIDEBAR;EVENT");
        java.util.List<PanelLines.Raw> ending = java.util.Arrays.asList(
                new PanelLines.Raw("§709/30/26 dung-1", 12, false),
                new PanelLines.Raw("§fLate Autumn 16th", 10, false),
                new PanelLines.Raw("§c⏣ The Catacombs (F3)", 8, false),
                new PanelLines.Raw("§a[A] §6aditbro", 7, false),
                new PanelLines.Raw("§fAuto-closing in: §c00:50", 3, false));
        int closeRows = 0;
        for (PanelLines.Row row : PanelLines.build(ending, null, null,
                java.util.Collections.<String>emptySet())) {
            if (RichText.strip(row.text).startsWith("Auto-closing in:")) {
                if (!"DUNGEON_SIDEBAR".equals(row.slot) || row.points != 3)
                    throw new AssertionError("auto-close row must keep its sidebar slot/order");
                closeRows++;
            }
        }
        if (closeRows != 1) throw new AssertionError("auto-close row missing: " + closeRows);
        SweatsEnhancements.cfg.setBool("scoreboard.dungeonSidebar", false);
        for (PanelLines.Row row : PanelLines.build(ending, null, null,
                java.util.Collections.<String>emptySet())) {
            if (RichText.strip(row.text).startsWith("Auto-closing in:"))
                throw new AssertionError("auto-close row remains with dungeon rows off");
        }
        SweatsEnhancements.cfg.setBool("scoreboard.dungeonSidebar", true);
        System.out.println("  dungeon auto-close countdown: 1 when on, 0 when off");

        // Outside dungeons, the server's event timer belongs directly under Event.
        SweatsEnhancements.cfg.set("scoreboard.appearance", "SERVER_DATE;LOCATION;EVENT;SEASON;PROFILE;BANK;INTEREST;GEMS");
        SweatsEnhancements.cfg.set("scoreboard.tabLines", "EVENT;PROFILE;BANK;INTEREST;GEMS");
        List<PanelLines.Raw> town = java.util.Arrays.asList(
                new PanelLines.Raw("09/29/26 hub-2", 3, false),
                new PanelLines.Raw("§7⏣ §aVillage", 2, false),
                new PanelLines.Raw("Late Autumn 3rd", 1, false));
        List<String> timerTab = java.util.Arrays.asList("§e§lEvent: §6Spooky Festival",
                "§fStarts in: §e12 hours", "Profile: Cucumber", "Bank: 2", "Interest: 4", "Gems: 17");
        List<String> timerClean = new ArrayList<String>();
        for (String text : timerTab) timerClean.add(RichText.strip(text));
        List<PanelLines.Row> townRows = PanelLines.build(town, timerTab, timerClean,
                java.util.Collections.<String>emptySet());
        int timerRows = 0;
        for (PanelLines.Row row : townRows) {
            if ("EVENT".equals(row.slot) && RichText.strip(row.text).contains("Starts in: 12 hours")) timerRows++;
        }
        if (timerRows != 1) throw new AssertionError("outdoor event timer missing: " + timerRows);
        for (String slot : new String[]{"SEASON", "PROFILE", "BANK", "INTEREST", "GEMS", "EVENT"}) {
            boolean found = false;
            for (PanelLines.Row row : townRows) if (slot.equals(row.slot)) found = true;
            if (!found) throw new AssertionError("outdoor row hidden: " + slot);
        }
        System.out.println("  outside dungeon: event timer " + timerRows
                + " (want 1); Season/Profile/Bank/Interest/Gems/Event visible");
        SweatsEnhancements.cfg.set("scoreboard.appearance", "");
        SweatsEnhancements.cfg.set("scoreboard.tabLines", "");

        // The elements the player asked out of every list: they are not offered any more, so a row
        // that lands in one of their slots is not drawn - and neither is a line no element claimed.
        System.out.println("================ the elements the player removed");
        String[][] gone = {
                { "AREA", "Area: Test Area" },
                { "SERVER", "Server: skyblock-9" },
                { "PLAYERS", "Players (3)" },
                { "MOTES", "Motes: 1,234" },
                { "COLD", "Cold: 3" },
                { "SOULFLOW", "Soulflow: 12" },
                { "FLINT_ARROW", "Fetchur: Flint and Steel x1" },
                { "SKILLS", "Skills:" },
                { "STATS", "Stats:" },
                { "PET", "Pet:" },
                { "PARTY", "Party (2)" },
                { "ACTIVE_EFFECTS", "Active Effects: 3" },
                { "WEBSITE", "mc.fakepixel.example" },
                { "EXTRA", "Some line nobody claimed" },
        };
        java.util.List<String> gRaw = new java.util.ArrayList<String>();
        java.util.List<String> gClean = new java.util.ArrayList<String>();
        for (String[] pair : gone) {
            gRaw.add(pair[1]);
            gClean.add(pair[1]);
        }
        StringBuilder gDrawn = new StringBuilder();
        for (PanelLines.Row row : PanelLines.build(new java.util.ArrayList<PanelLines.Raw>(), gRaw,
                gClean, new java.util.HashSet<String>())) {
            gDrawn.append(row.slot).append(' ').append(com.sweatsenhancements.core.RichText.strip(row.text)).append('\n');
        }
        for (String[] pair : gone) {
            System.out.println("  " + pad(pair[0]) + " offered: "
                    + com.sweatsenhancements.core.PanelSlots.ALL.contains(pair[0])
                    + "   drawn on the panel: " + gDrawn.toString().contains(pair[1]));
        }

        // an old settings file: the fourteen names this build dropped are replaced by the elements
        // this build offers, put back where the default order has them
        SweatsEnhancements.cfg.set("scoreboard.appearance",
                "SERVER_DATE;TIME;SEASON;LOCATION;AREA;SERVER;PLAYERS;PROFILE;PURSE;MOTES;SKILLS;"
                + "PET;WEBSITE;EXTRA");
        com.sweatsenhancements.config.Migrate.run(SweatsEnhancements.cfg);
        System.out.println("  an old file now reads as  : "
                + PanelLines.list("scoreboard.appearance", null));
        SweatsEnhancements.cfg.set("scoreboard.appearance", "");

        // The Dwarven Mines, exactly as the player's own screenshot of its tab list shows it: the
        // powder block is written with the name of the thing that yields each powder, and the forge
        // list is numbered rows under a heading of one word.
        System.out.println("================ the Dwarven Mines, from the player's screenshot");
        String[] dwarf = {
                "Players (4)", "aditbro", "Ufio1", "ISHANT3385", "Rashikh_Ayaan",
                "Server Info", "Area: The Forge", "Server: sbm-3", "Gems: 17",
                "Powders", "Mithril Powder: 58,608", "Gemstone Powder: 15,727",
                "Titanium Miner: 73.3%", "2x Mithril Powder Collector: 6%",
                "Upper Mines Mithril: 88.3%", "Raffle: 0%",
                "Forges", "1) EMPTY", "2) EMPTY", "3) EMPTY", "4) EMPTY", "5) EMPTY",
                "6) LOCKED", "7) LOCKED",
                "Profile: Cucumber", "SB Level: [217] 51/100 XP",
        };
        java.util.List<String> dRaw = new java.util.ArrayList<String>();
        java.util.List<String> dClean = new java.util.ArrayList<String>();
        java.util.Set<String> dPlayers = new java.util.HashSet<String>();
        for (String line : dwarf) {
            dRaw.add(line);
            dClean.add(line);
            // the roster lists these four; the two headings are name-shaped as well and are not players
            if (line.equals("aditbro") || line.equals("Ufio1") || line.equals("ISHANT3385")
                    || line.equals("Rashikh_Ayaan")) {
                dPlayers.add(line.toLowerCase(java.util.Locale.ROOT));
            }
        }
        int dwarfLines = 0;
        java.util.Map<String, java.util.List<String>> dwarfSlots =
                new java.util.HashMap<String, java.util.List<String>>();
        for (PanelLines.Row row : PanelLines.build(new java.util.ArrayList<PanelLines.Raw>(), dRaw, dClean, dPlayers)) {
            dwarfLines++;
            System.out.println("    " + pad(row.slot) + " | " + row.text);
            java.util.List<String> bucket = dwarfSlots.get(row.slot);
            if (bucket == null) {
                bucket = new java.util.ArrayList<String>();
                dwarfSlots.put(row.slot, bucket);
            }
            bucket.add(row.text);
        }
        System.out.println("  the Dwarven panel draws " + dwarfLines + " rows");
        // What the player's screenshot has to come out as: the powder block under its one-word
        // heading with the collector line that is not written as a key with the rest of them, the
        // forge list under its own, and none of the four names.
        System.out.println("  the powder block: " + count(dwarfSlots, "POWDER", null) + " rows (want 7,"
                + " heading and the collector line included) - "
                + (count(dwarfSlots, "POWDER", "2x Mithril Powder Collector") == 1
                        ? "the collector line is drawn" : "THE COLLECTOR LINE IS MISSING"));
        System.out.println("  the forge block: " + count(dwarfSlots, "FORGES", null) + " rows (want 8,"
                + " heading and seven numbered slots) - "
                + (count(dwarfSlots, "FORGES", "1) EMPTY") == 1
                        ? "the numbered rows are drawn" : "THE NUMBERED ROWS ARE MISSING"));
        int dwarfNames = 0;
        for (String name : new String[] {"aditbro", "Ufio1", "ISHANT3385", "Rashikh_Ayaan"}) {
            for (java.util.List<String> rows : dwarfSlots.values()) {
                for (String text : rows) {
                    if (text.trim().equals(name)) dwarfNames++;
                }
            }
        }
        System.out.println("  player names drawn anywhere: " + dwarfNames + " (want 0, the party element is gone)");

        System.out.println("================ the player's own screen, line by line");
        // The player sent a picture of what the panel should carry on Fakepixel and said "this much
        // information". These are its sidebar lines, kept exactly as the picture lays them out -
        // including the island line's symbol, which is the server's to choose, and the Fetchur row.
        // The picture's top line, `SKYBLOCK`, is not one of the sidebar's score entries: it is the
        // sidebar's objective, which is where the panel's own title comes from, so it is not here.
        wipe(new File("cfg-picture"));
        SweatsEnhancements.cfg = new Cfg(new File("cfg-picture"));
        List<PanelLines.Raw> picture = new ArrayList<PanelLines.Raw>();
        picture.add(new PanelLines.Raw("\u00a7f09/25/25 \u00a78hub-1", 14, false));
        picture.add(new PanelLines.Raw("\u00a7f9:00pm \u00a7e\u2600", 13, false));
        picture.add(new PanelLines.Raw("\u00a7fEarly Winter 23rd", 12, false));
        picture.add(new PanelLines.Raw("\u00a77\u32d6 \u00a7aSkyblock Hub", 11, false));
        picture.add(new PanelLines.Raw("\u00a77\u23e3 \u00a7bVillage", 10, false));
        picture.add(new PanelLines.Raw("\u00a7fPurse: \u00a76107,259,158.6", 9, false));
        picture.add(new PanelLines.Raw("\u00a7fBank: \u00a760/0", 8, false));
        picture.add(new PanelLines.Raw("\u00a7fBits: \u00a7b1,334", 7, false));
        picture.add(new PanelLines.Raw("\u00a7fGems: \u00a7a17", 6, false));
        picture.add(new PanelLines.Raw("\u00a7fPower: \u00a7dHurtful", 5, false));
        picture.add(new PanelLines.Raw("\u00a7fFetchur: \u00a7aFlint and Steel x1", 4, false));
        picture.add(new PanelLines.Raw("\u00a7fSlayer Quest", 3, false));
        picture.add(new PanelLines.Raw("\u00a75Revenant Horror V", 2, false));
        picture.add(new PanelLines.Raw("\u00a77(\u00a7e2,070\u00a77/\u00a7c6,000\u00a77) Combat", 1, false));
        picture.add(new PanelLines.Raw("\u00a7emc.fakepixel.me", 0, false));
        for (PanelLines.Row row : PanelLines.build(picture, null, null,
                java.util.Collections.<String>emptySet())) {
            System.out.println("  " + pad(row.slot) + "| " + row.text.replace('\u00a7', '&'));
        }

        System.out.println("================ the meter and the cookie line");
        // CSB's slayer meter, and the cookie line as JEF draws it: off while no booster runs, kept
        // as the server wrote it when one does.
        wipe(new File("cfg-meter"));
        SweatsEnhancements.cfg = new Cfg(new File("cfg-meter"));
        SweatsEnhancements.cfg.setBool("scoreboard.slayerMeter", true);
        List<PanelLines.Raw> meter = new ArrayList<PanelLines.Raw>();
        meter.add(new PanelLines.Raw("\u00a7fSlayer Quest", 3, false));
        meter.add(new PanelLines.Raw("\u00a75Revenant Horror V", 2, false));
        meter.add(new PanelLines.Raw("\u00a77(\u00a7e2,070\u00a77/\u00a7c6,000\u00a77) Combat", 1, false));
        meter.add(new PanelLines.Raw("\u00a7fCookie Buff: \u00a7cNot Active", 0, false));
        int bars = 0;
        for (PanelLines.Row row : PanelLines.build(meter, null, null,
                java.util.Collections.<String>emptySet())) {
            System.out.println("  " + pad(row.slot) + "| " + row.text.replace('\u00a7', '&'));
            // the bar is a line of its own under the progress line, never a tail on it: the player
            // asked for that - "move it up or down a line i dont like how its in the same line"
            String plain = RichText.strip(row.text);
            if (plain.indexOf('\u25ac') >= 0 && plain.indexOf("Kills") < 0) bars++;
        }
        System.out.println("  meter lines of their own: " + bars + "  (want 1)");
        System.out.println("  (the bar line carries the bar and the percentage; the progress"
                + " line above it is untouched)");

        List<PanelLines.Raw> withBooster = new ArrayList<PanelLines.Raw>();
        withBooster.add(new PanelLines.Raw("\u00a7dCookie Buff\u00a7f: \u00a7a3d 4h", 0, false));
        for (PanelLines.Row row : PanelLines.build(withBooster, null, null,
                java.util.Collections.<String>emptySet())) {
            System.out.println("  with a booster running -> " + pad(row.slot) + "| "
                    + row.text.replace('\u00a7', '&'));
        }

        // the bar's cells, by code point, so "the glyphs are the ones I meant" is shown and not
        // assumed: a console prints them as `?` and says nothing either way
        for (PanelLines.Row row : PanelLines.build(meter, null, null,
                java.util.Collections.<String>emptySet())) {
            if (!"SLAYER".equals(row.slot) || row.text.indexOf('\u25ac') < 0) continue;
            StringBuilder out = new StringBuilder("  bar cells : ");
            for (char c : row.text.toCharArray()) {
                if (c == '\u25ac' || c == '\u2591') out.append("U+").append(Integer.toHexString(c)).append(' ');
            }
            System.out.println(out.toString().trim());
        }

        // POWER out of the tab list, which is where this server keeps the figures JEF reads there
        List<String> tabRaw = new ArrayList<String>();
        List<String> tabClean = new ArrayList<String>();
        tabRaw.add("\u00a7fPower: \u00a7dHurtful");
        tabClean.add("Power: Hurtful");
        for (PanelLines.Row row : PanelLines.build(new ArrayList<PanelLines.Raw>(), tabRaw, tabClean,
                java.util.Collections.<String>emptySet())) {
            System.out.println("  Power from the tab list -> " + pad(row.slot) + "| "
                    + row.text.replace('\u00a7', '&'));
        }

        System.out.println("---- the live slayer block, exactly as the player's dump sent it");
        // The dump of 26/09/2026 has these three lines, and on the live screen the two shaped ones
        // arrive *after* another element's line - which is the case the old "within four lines of
        // the heading" rule missed, leaving them to be drawn as unrecognised rows and then listed a
        // second time under the unknown heading the player saw.
        java.util.List<PanelLines.Raw> live = new ArrayList<PanelLines.Raw>();
        live.add(new PanelLines.Raw("\u00a7709/26/2026 hub-5", 14, false));
        live.add(new PanelLines.Raw("\u00a7fPurse: \u00a76102,878,200", 8, false));
        live.add(new PanelLines.Raw("\u00a7fSlayer Quest", 5, false));
        live.add(new PanelLines.Raw("\u00a7emc.fakepixel.me", 1, false));
        live.add(new PanelLines.Raw(" \u00a77(\u00a7e2,070\u00a77/\u00a7c6,000\u00a77) Combat ", 3, false));
        live.add(new PanelLines.Raw("\u00a75Revenant Horror V", 4, false));
        int extras = 0;
        for (PanelLines.Row row : PanelLines.build(live, null, null,
                java.util.Collections.<String>emptySet())) {
            if (PanelSlots.EXTRA.equals(row.slot)) extras++;
            System.out.println("   " + row.slot + "   | " + row.text.replace('\u00a7', '&'));
        }
        System.out.println("   rows left unrecognised: " + extras + "  (want 0)");

        System.out.println("================ the mayor, as this server sends it");
        // The player: "mayor stats in sb doesnt work ... its in tablist but not scoreboard". This
        // server sends the election without any "Elections:" heading - the candidates carry their
        // own share of the vote in the key, the countdown is "Election: ...", the winner is
        // "Current Mayor: ...". Every one of those keys used to name an element of its own, none of
        // which is ever selected, so the block was read and then dropped on the floor. The share
        // bar is the shape that says "this is the election", and the winner and the countdown are
        // asked for by name.
        wipe(new File("cfg-mayor"));
        SweatsEnhancements.cfg = new Cfg(new File("cfg-mayor"));
        List<PanelLines.Raw> mayorSide = new ArrayList<PanelLines.Raw>();
        mayorSide.add(new PanelLines.Raw("\u00a7fPurse: \u00a76107,259,158.6", 9, false));
        List<String> mayorTab = new ArrayList<String>();
        List<String> mayorTabClean = new ArrayList<String>();
        String[] electionRows = {
                "\u00a7dScorpius: \u00a7d|\u00a7d|\u00a7d|\u00a7d|\u00a7d|\u00a7d|\u00a7d|\u00a7d|\u00a7d|\u00a7f| \u00a7f(98.3%)",
                "\u00a7cMarina: \u00a7f|\u00a7f|\u00a7f|\u00a7f|\u00a7f|\u00a7f|\u00a7f|\u00a7f|\u00a7f|\u00a7f| \u00a7f(0.9%)",
                "\u00a7eElection: \u00a7f2d 19m 39s",
                "\u00a7eCurrent Mayor: \u00a7aPaul",
                // a candidate with nothing behind the name is not data, and must not turn into an
                // element of its own
                "\u00a7cMarina:",
                "\u00a7fStrength: \u00a7c\u2741713.4"};
        for (String row : electionRows) {
            mayorTab.add(row);
            mayorTabClean.add(RichText.strip(row).trim());
        }
        int drawn = 0;
        for (PanelLines.Row row : PanelLines.build(mayorSide, mayorTab, mayorTabClean,
                java.util.Collections.<String>emptySet())) {
            System.out.println("  " + pad(row.slot) + "| " + row.text.replace('\u00a7', '&'));
            if ("ELECTIONS".equals(row.slot)) drawn++;
        }
        System.out.println("  election rows drawn: " + drawn + "  (want 4)");
        System.out.println("  keys : " + PanelSlots.slotOfTabKey("Election") + " / "
                + PanelSlots.slotOfTabKey("Current Mayor") + " / "
                + PanelSlots.slotOfTabKey("Minister") + " / " + PanelSlots.slotOfTabKey("Scorpius"));

        System.out.println("================ settings");
        // The experiments get their own config file, so they never colour a later run's draw.
        wipe(new File("cfg-settings"));
        SweatsEnhancements.cfg = new Cfg(new File("cfg-settings"));
        SweatsEnhancements.cfg.setBool("scoreboard.chunked", true);
        SweatsEnhancements.cfg.setBool("scoreboard.customLines", true);
        SweatsEnhancements.cfg.setBool("scoreboard.allEvents", true);
        SweatsEnhancements.cfg.set("scoreboard.numberDisplay", "123: Purse");
        List<PanelLines.Raw> few = new ArrayList<PanelLines.Raw>();
        few.add(new PanelLines.Raw("\u00a7fPurse: \u00a76pop", 8, false));
        System.out.println("lines with junk input: "
                + PanelLines.build(few, null, null, java.util.Collections.<String>emptySet()).size());
        System.out.println("rich text : " + RichText.render("<bold>Hi</bold> <green>there</green> <#f38ba8>red</#f38ba8>").replace('\u00a7', '&'));
        System.out.println("gradient  : " + RichText.render("<gradient:#f38ba8:#a6e3a1>abcdef</gradient>").replace('\u00a7', '&'));
        System.out.println("unknown   : " + RichText.render("<notatag> x &c y").replace('\u00a7', '&'));
        System.out.println("numbers   : " + Numbers.format(52763737, "1,234,567", true) + " / "
                + Numbers.formatShort(137600000, "1,234,567") + " / " + Numbers.parse("137.6M") + " / " + Numbers.parse("1,334"));
        System.out.println("list i/o  : " + TextList.join(TextList.split("bank; gems ;sb level", null)));
        System.out.println("slots     : " + PanelSlots.slotOf("\u00a7emc.fakepixel.me") + " / "
                + PanelSlots.slotOf("\u00a7fPurse: \u00a762,860,502") + " / " + PanelSlots.slotOf("hello world"));
    }

    /** how many rows of a slot there are, or how many of them mention a phrase when one is given */
    static int count(java.util.Map<String, java.util.List<String>> slots, String slot, String phrase) {
        java.util.List<String> rows = slots.get(slot);
        if (rows == null) return 0;
        if (phrase == null) return rows.size();
        int n = 0;
        for (String text : rows) {
            if (text.contains(phrase)) n++;
        }
        return n;
    }

    static String pad(String s) {
        while (s.length() < 16) s = s + " ";
        return s;
    }
}
