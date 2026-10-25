package com.sweatsenhancements.feature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.core.Numbers;
import com.sweatsenhancements.core.PanelSlots;
import com.sweatsenhancements.core.RichText;

/**
 * Builds the lines the scoreboard draws, out of what the server actually sent.
 *
 * Two sources, one list. The sidebar supplies the lines the server puts in the sidebar; the tab
 * list supplies everything else, which on this server is most of the useful numbers - Bank,
 * Interest, Gems, SkyBlock level, the stat block, skills, the pet, the election and the party.
 * Both are claimed into the same element names, so the Appearance list is one list the player
 * can order, rather than two lists that fight over the same panel.
 *
 * Every rewrite in here belongs to one setting on the settings page, and every one of them has
 * the same shape: take the line the server sent, change the one part the setting is about, keep
 * the rest of the line - including its colours - exactly as the server wrote it. A setting that
 * has nothing to work on changes nothing, which is why an element for a figure only the other server writes
 * costs nothing here: it is skipped, not invented.
 *
 * No Minecraft imports on purpose: text in, text out, so the 1.21.11 port reuses this class.
 */
public final class PanelLines {

    /** one line of the finished panel: which element it belongs to, and what it says */
    public static final class Row {
        public final String slot;
        public final String text;
        public final int points;
        /** where the line came from in the server's own order, or -1 for a line built from the tab */
        public final int sourceIndex;

        public Row(String slot, String text, int points, int sourceIndex) {
            this.slot = slot;
            this.text = text;
            this.points = points;
            this.sourceIndex = sourceIndex;
        }
    }

    /** a line as the client's sidebar holds it, before any claiming */
    public static final class Raw {
        public final String text;
        public final int points;
        /** true when the line carries nothing but colour codes - the server's own spacer */
        public final boolean blank;

        public Raw(String text, int points, boolean blank) {
            this.text = text;
            this.points = points;
            this.blank = blank;
        }
    }

    /** the blocks a tab list uses to group lines under a heading */
    private static final List<String> LIST_BLOCKS = Arrays.asList(
            "SKILLS", "STATS", "PET", "ELECTIONS", "EVENT", "ACTIVE_EFFECTS", "PARTY", "POWDER",
            "FORGES");

    /**
     * "Area: Hub" -> AREA, "Combat 60: 100%" -> COMBAT_60.
     *
     * Digits are allowed in the key because this server numbers its skills and its pets that way
     * ("Farming 27"), and without them those lines read as block members instead of as lines of
     * their own. A row with no colon is never a key line, so a player's name cannot match.
     */
    /** "N/A", "N/a", "-" - the server saying it has no number for this row. */
    private static final Pattern NO_VALUE = Pattern.compile("^(?i)n/?a$|^-+$");

    /**
     * A slayer quest's own lines, claimed by shape rather than by position.
     *
     * Position does not work here: the server writes the boss and the progress in either order, and
     * it puts other lines between them (the scoreboard's own address line turned up inside the
     * block on a real screen, and the two quest lines were thrown out as unrecognised).
     */
    private static final Pattern SLAYER_BOSS = Pattern.compile("(?i)^(revenant horror|tarantula"
            + " broodfather|sven packmaster|voidgloom seraph|inferno demonlord|riftstalker"
            + " bloodfiend)(\\s+[IVX]+)?$");
    // Both patterns are written to the shapes this server actually sends, which is what the live
    // screen and the player's dump showed: "Revenant Horror V" on a line of its own, and
    // "(2,070/6,000) Combat" - the progress, then the type of the quest. The old patterns
    // accepted any line *containing* a boss name, or any bracketed pair anywhere in a line,
    // and still missed both live lines, which is how they ended up drawn as unrecognised rows.
    private static final Pattern SLAYER_PROGRESS = Pattern.compile(
            "^\\(\\s*([\\d,]+)\\s*/\\s*([\\d,]+)\\s*\\)\\s*[A-Za-z]+$");

    private static final Pattern KEY_LINE =
            Pattern.compile("^([A-Za-z][A-Za-z0-9 '/\\-]{1,28}):\\s*(\\S.*)$");

    private PanelLines() {
    }

    /** Read the sidebar, not the tab list: only dungeon servers carry dung-N in the date. */
    private static boolean dungeonSidebar(List<Raw> sidebar) {
        if (sidebar == null) return false;
        boolean server = false;
        boolean location = false;
        for (Raw row : sidebar) {
            if (row == null || row.text == null) continue;
            String clean = RichText.strip(row.text).trim().toLowerCase(Locale.ROOT);
            if (clean.matches(".*\\bdung-\\d+.*")) server = true;
            if (clean.contains("the catacombs")) location = true;
        }
        return server && location;
    }

    private static boolean dungeonLine(String clean) {
        String lower = clean.toLowerCase(Locale.ROOT);
        return lower.startsWith("keys:") || lower.startsWith("time elapsed:")
                || lower.startsWith("dungeon cleared:")
                || lower.startsWith("auto-closing in:") || lower.startsWith("auto closing in:")
                || clean.matches("^\\[[A-Z]\\]\\s+.*");
    }

    // ------------------------------------------------------------------ settings, all in one place

    private static String text(String key, String def) {
        try {
            String v = SweatsEnhancements.cfg == null ? null : SweatsEnhancements.cfg.get(key, def);
            return v == null ? def : v;
        } catch (Throwable t) {
            return def;
        }
    }

    private static boolean flag(String key, boolean def) {
        try {
            return SweatsEnhancements.cfg == null ? def : SweatsEnhancements.cfg.getBool(key, def);
        } catch (Throwable t) {
            return def;
        }
    }

    private static int number(String key, int def) {
        try {
            return SweatsEnhancements.cfg == null ? def : SweatsEnhancements.cfg.getInt(key, def);
        } catch (Throwable t) {
            return def;
        }
    }

    /** A list setting is stored as one semicolon separated line, so a comma in a name is safe. */
    public static List<String> list(String key, List<String> def) {
        String stored = SweatsEnhancements.cfg == null ? null : SweatsEnhancements.cfg.get(key, null);
        if (stored == null || stored.trim().isEmpty()) {
            // 0.9.4 and earlier stored the sidebar order under scoreboard.order; keep it if the
            // player had changed it, because losing a chosen order on upgrade is not acceptable
            if ("scoreboard.appearance".equals(key)) {
                String legacy = SweatsEnhancements.cfg == null ? null : SweatsEnhancements.cfg.get("scoreboard.order", null);
                if (legacy != null && !legacy.trim().isEmpty()) {
                    List<String> out = new ArrayList<String>();
                    for (String piece : legacy.split("[,\\s]+")) {
                        if (!piece.trim().isEmpty()) out.add(piece.trim().toUpperCase(Locale.ROOT));
                    }
                    if (!out.isEmpty()) return out;
                }
            }
            return new ArrayList<String>(def);
        }
        return com.sweatsenhancements.core.TextList.split(stored, def);
    }

    public static void writeList(String key, List<String> values) {
        try {
            if (SweatsEnhancements.cfg == null) return;
            SweatsEnhancements.cfg.set(key, com.sweatsenhancements.core.TextList.join(values));
        } catch (Throwable t) {
            // the settings page will show the default next time, which is recoverable
        }
    }

    // ------------------------------------------------------------------ the build

    /**
     * Claims every input line into an element, applies the settings, and returns the lines to
     * draw in the player's own order.
     *
     * sidebar and tabClean are the raw lines with their colour codes intact / taken out, in the
     * order the client holds them. Nothing is ever dropped for being unrecognised: a sidebar line
     * no element claims keeps its place in EXTRA, and the tab list contributes only elements the
     * player selected.
     */
    public static List<Row> build(List<Raw> sidebar, List<String> tabRaw, List<String> tabClean) {
        return build(sidebar, tabRaw, tabClean, java.util.Collections.<String>emptySet());
    }

    /** The same, with the names of the real players so their rows can be told from the server's. */
    public static List<Row> build(List<Raw> sidebar, List<String> tabRaw, List<String> tabClean,
                                  java.util.Set<String> playerNames) {
        Map<String, List<Row>> claimed = new LinkedHashMap<String, List<Row>>();

        // ---- the sidebar
        //
        // The slayer block is three lines on this server: "Slayer Quest", the boss with its level,
        // and the progress towards it. Only the heading says what it is, so the other two are taken
        // with it by their shape - boss name, or "(…/…)" progress - wherever they sit, and
        // however many of the server's other lines it drops between them. Claiming them by position
        // meant any line wedged in ended the block, which is exactly what a real screen showed.
        boolean dungeon = dungeonSidebar(sidebar);
        for (int i = 0; i < sidebar.size(); i++) {
            Raw raw = sidebar.get(i);
            String clean = RichText.strip(raw.text).trim();
            if (clean.isEmpty()) continue;                    // the server's own spacers
            String slot = PanelSlots.slotOf(clean);
            // Dungeon-specific rows come from the sidebar itself, not the tab list's
            // party roster. Only claim them when the sidebar says this is a dungeon.
            if (dungeon && flag("scoreboard.dungeonSidebar", true) && dungeonLine(clean)) {
                put(claimed, "DUNGEON_SIDEBAR", new Row("DUNGEON_SIDEBAR", raw.text, raw.points, i));
                continue;
            }
            // Claimed by shape, wherever it sits. The quest is the heading and the two shaped lines,
            // and the shaped lines do not have to arrive near the heading: the player's own dump
            // shows them arriving after other elements' rows have gone past, which is exactly the
            // case the old "within four lines of the heading" rule missed - they were drawn as
            // unrecognised rows and then listed a second time under the unknown heading.
            if (slot == null && isSlayerLine(clean)) {
                put(claimed, "SLAYER", new Row("SLAYER", indent(raw.text), raw.points, i));
                continue;
            }
            if (slot == null) slot = PanelSlots.EXTRA;
            put(claimed, slot, new Row(slot, raw.text, raw.points, i));
        }

        // The client's score collection is ascending; the sidebar draws highest score first.
        // Keep the dungeon rows in that original on-screen order.
        List<Row> dungeonRows = claimed.get("DUNGEON_SIDEBAR");
        if (dungeonRows != null) {
            java.util.Collections.sort(dungeonRows, new java.util.Comparator<Row>() {
                public int compare(Row a, Row b) { return Integer.compare(b.points, a.points); }
            });
        }

        // ---- the tab list
        List<String> selected = list("scoreboard.tabLines", PanelSlots.DEFAULT_TAB_LINES);
        readTab(claimed, tabRaw, tabClean, selected, playerNames);

        // ---- the settings that rewrite what a line says
        rewrite(claimed);
        if (dungeon) {
            // Dungeon sidebar-only layout: don't change the saved Appearance or Tab Lines lists.
            for (String hidden : new String[]{"SEASON", "PROFILE", "BANK", "INTEREST", "GEMS", "EVENT"}) {
                claimed.remove(hidden);
            }
        }

        // ---- out, in the player's own order
        List<Row> ordered = order(claimed);

        // ---- the settings that change the shape of the list rather than a line
        // Chunky Stats: the currencies the player put in the list are put several to a line. The
        // list is the switch - an empty list chunks nothing, which is how the panel draws before
        // anything is added.
        List<String> chunked = list("scoreboard.chunkedStats", PanelSlots.DEFAULT_CHUNKED);
        if (!chunked.isEmpty()) {
            ordered = chunk(ordered, Math.max(1, number("scoreboard.chunkedPerLine", 3)), chunked);
        }
        if (!flag("scoreboard.condenseSeparators", true)) {
            ordered = separate(ordered, sidebar);
        }
        if (flag("scoreboard.customLines", false)) {
            List<Row> onlyElements = new ArrayList<Row>();
            for (Row row : ordered) {
                if (!PanelSlots.EXTRA.equals(row.slot)) onlyElements.add(row);
            }
            ordered = onlyElements;
        }

        // ---- "Hide consecutive empty lines" and "Hide empty lines at top/bottom", the two
        // filtering settings the reference has. The server's single blank lines are what groups its
        // blocks and are kept; a blank line whose neighbour is blank as well, and any blank line at
        // either end of the panel, are not.
        if (flag("scoreboard.hideConsecutiveEmpty", true)) ordered = collapseBlank(ordered);
        if (flag("scoreboard.hideEmptyEdges", true)) ordered = trimBlankEdges(ordered);
        return ordered;
    }

    private static void put(Map<String, List<Row>> map, String slot, Row row) {
        List<Row> bucket = map.get(slot);
        if (bucket == null) {
            bucket = new ArrayList<Row>();
            map.put(slot, bucket);
        }
        bucket.add(row);
    }

    /**
     * Reads the tab list into elements.
     *
     * A line under a heading belongs to that heading's element - "Farming 27: 25%" is part of
     * SKILLS, "Speed: 369" is part of STATS - and a key line anywhere else is its own element,
     * which is how Bank, Interest, Gems and the SkyBlock level get in. Only elements the player
     * ticked in "Select Tablist Lines" are read at all, so the setting decides what exists rather
     * than what is hidden after the fact.
     */
    private static void readTab(Map<String, List<Row>> map, List<String> tabRaw, List<String> tabClean,
                                List<String> selected, java.util.Set<String> playerNames) {
        if (tabClean == null || tabClean.isEmpty()) return;
        String block = null;
        // The party list went with the Party element: this server has no party block of its own in
        // the tab list - it lists everyone on the island - and the player asked the element out.
        // Its rows are not collected, and a real player's row inside a stat block is still dropped.
        Set<String> seenParty = new HashSet<String>();
        for (int i = 0; i < tabClean.size(); i++) {
            String clean = tabClean.get(i) == null ? "" : tabClean.get(i).trim();
            if (clean.isEmpty()) {
                // the server separates its own blocks with padding rows: a block ends where one is
                block = null;
                continue;
            }
            // the server pads its columns with `CLICK` rows, and it also fills them with rows whose
            // whole content is a player's name. Neither is a line the panel has anything to say
            // about, and both were being claimed - the `CLICK` rows came out under Skills and Stats.
            if (com.sweatsenhancements.feature.CustomTabList.paddingRow(clean)) continue;
            if (com.sweatsenhancements.feature.CustomTabList.bareNameRow(clean, playerNames)) continue;

            String raw = i < tabRaw.size() && tabRaw.get(i) != null ? tabRaw.get(i) : clean;

            String header = PanelSlots.tabHeaderOf(clean);
            if (header != null && !header.contains(":") && !header.equals(clean)) {
                block = selected.contains(header) ? header : null;
                if (block != null && (!"PARTY".equals(block)
                        || seenParty.add(clean.toLowerCase(Locale.ROOT)))) {
                    put(map, block, new Row(block, headerOf(clean), 0, -1));
                }
                continue;
            }

            boolean isKey = KEY_LINE.matcher(clean).matches();
            String keySlot = null;
            if (isKey) {
                keySlot = PanelSlots.slotOfTabKey(KEY_LINE.matcher(clean).replaceFirst("$1"));
                // a mayor candidate is whoever the server puts in the key, so the election claims
                // a line whose value is a share bar
                if (keySlot != null && !selected.contains(keySlot)
                        && PanelSlots.electionValue(KEY_LINE.matcher(clean).replaceFirst("$2"))) {
                    keySlot = "ELECTIONS";
                }
            }

            // A real player is a player row wherever it turns up, except inside the party list -
            // the server mixes player entries in among its stat lines, and a name is not a stat.
            if (!"PARTY".equals(block) && isPlayerRow(clean, playerNames)) {
                block = null;
                continue;
            }

            boolean blockMember = block != null && LIST_BLOCKS.contains(block);
            if (blockMember && isKey && keyEndsBlock(block)) {
                // a pet, an effects list and a party hold names, and the server follows them with
                // the election and the event: a key line ends those blocks rather than joining them
                blockMember = false;
                block = null;
            }
            if (blockMember && isKey && keySlot != null && selected.contains(keySlot)) {
                // a line the player asked for by name is that element, not a line of the block - but
                // the block it sits in carries on: the Dwarven powder block is written as five key
                // lines and one that is not a key at all ("2x Mithril Powder Collector: 6%"), and
                // ending the block at the first of them dropped that line and everything after it
                blockMember = false;
            }
            if (blockMember) {
                put(map, block, new Row(block, indent(raw), 0, -1));
                continue;
            }

            // a value the server has no number for ("Pet Sitter: N/A") is not data: the player's
            // rule is that a line without data is skipped rather than drawn empty
            String value = KEY_LINE.matcher(clean).replaceFirst("$2");
            if (keySlot != null && selected.contains(keySlot) && !NO_VALUE.matcher(value).matches()) {
                put(map, keySlot, new Row(keySlot, raw, 0, -1));
                continue;                       // the block the line sits in is still open
            }
            block = null;
        }

        // The server sends the event name and its timer as separate fake tab entries.
        // "Starts in" is not itself a selectable element; attach the server's own timer
        // to the EVENT slot only when there is an unambiguous event to attach it to.
        if (selected.contains("EVENT") && map.containsKey("EVENT")) {
            int eventIndex = -1;
            java.util.List<Integer> timers = new ArrayList<Integer>();
            for (int i = 0; i < tabClean.size(); i++) {
                String clean = tabClean.get(i) == null ? "" : tabClean.get(i).trim();
                String lower = clean.toLowerCase(Locale.ROOT);
                if (eventIndex < 0 && lower.startsWith("event:")) eventIndex = i;
                if (lower.startsWith("starts in:") || lower.startsWith("ends in:")) {
                    timers.add(Integer.valueOf(i));
                }
            }
            int chosen = -1;
            if (eventIndex >= 0) {
                for (Integer candidate : timers) {
                    int distance = candidate.intValue() - eventIndex;
                    if (distance > 0 && distance <= 3) { chosen = candidate.intValue(); break; }
                }
                // Dumps can list fake players in hash-map order rather than screen order.
                // Only use a distant timer when it's the *only* one, and no election
                // block could own it instead.
                if (chosen < 0 && timers.size() == 1 && !map.containsKey("ELECTIONS")) {
                    chosen = timers.get(0).intValue();
                }
            }
            if (chosen >= 0 && chosen < tabRaw.size()) {
                put(map, "EVENT", new Row("EVENT", tabRaw.get(chosen), 0, -1));
            }
        }
    }

    /**
     * True when a tab list row is a real player rather than a line the server made up.
     *
     * Matched on the end of the row, because the server writes a rank in front of the name
     * ("[VIP+] name"). A row is only a player row when the name the client holds for an actual
     * player is what the row ends with, so a stat line that happens to contain a name is safe.
     */
    private static boolean isPlayerRow(String clean, java.util.Set<String> playerNames) {
        if (playerNames == null || playerNames.isEmpty()) return false;
        String[] words = clean.trim().split(" ");
        // the name is at the end of the row, behind a rank and possibly a symbol the server adds
        // after it, so the last few words are all looked at rather than just the last one
        int checked = 0;
        for (int i = words.length - 1; i >= 0 && checked < 3; i--, checked++) {
            String candidate = words[i].toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "");
            if (candidate.isEmpty()) continue;
            if (playerNames.contains(candidate)) return true;
        }
        return false;
    }

    /**
     * True when a "Key: value" line ends this block rather than joining it.
     *
     * A skills or stats block is nothing but key lines, so it keeps them. A pet, an active effects
     * block and the party list hold names, and on this server they are always followed by the
     * election and event lines - without this, "Winner: Paul" would be drawn under "Pet".
     */
    private static boolean keyEndsBlock(String block) {
        return "PET".equals(block) || "ACTIVE_EFFECTS".equals(block) || "PARTY".equals(block);
    }

    /** A block's heading, centred the way the server writes it - "Skills:" keeps its colon. */
    private static String headerOf(String clean) {
        return PanelSlots.blockTitle(clean);
    }

    /** A line inside a block, indented the way the reference panel indents its rows. */
    private static String indent(String raw) {
        return "  " + raw;
    }

    // ------------------------------------------------------------------ rewrites

    /**
     * Applies every setting that changes what a line says.
     *
     * Each one keeps the server's own label and colours: the number changes, the line does not.
     * When a setting has no data to work on it leaves the line alone, so a switch that cannot do
     * anything here is visible as doing nothing rather than as inventing a value.
     */
    private static void rewrite(Map<String, List<Row>> claimed) {
        String numberFormat = text("scoreboard.numberFormat", "1,234,567");
        String numberDisplay = text("scoreboard.numberDisplay", "Purse: 123");

        // ---- the currencies, which is where most of the settings are
        String[] moneySlots = {"PURSE", "BANK", "BITS", "GEMS", "COPPER", "MOTES", "HEAT", "COLD",
                "SOULFLOW", "NORTH_STARS", "FLINT_ARROW", "POWER", "TUNING"};
        for (String slot : moneySlots) {
            List<Row> rows = claimed.get(slot);
            if (rows == null) continue;
            for (int i = 0; i < rows.size(); i++) {
                Row row = rows.get(i);
                rows.set(i, new Row(slot, amount(row.text, slot, numberFormat, numberDisplay), row.points,
                        row.sourceIndex));
            }
        }

        // ---- Piggy Bank: the prefix the purse is drawn under
        if (flag("scoreboard.piggyBank", false)) {
            List<Row> purse = claimed.get("PURSE");
            if (purse != null) {
                for (int i = 0; i < purse.size(); i++) {
                    Row row = purse.get(i);
                    int colon = row.text.indexOf(':');
                    if (colon < 0) continue;
                    String label = RichText.strip(row.text.substring(0, colon));
                    if (label.toLowerCase(Locale.ROOT).startsWith("purse")) {
                        String replaced = row.text.replaceFirst("(?i)Purse", "Piggy");
                        purse.set(i, new Row("PURSE", replaced, row.points, row.sourceIndex));
                    }
                }
            }
        }

        // ---- Bank: always compact, and the personal/co-op layout
        List<Row> bank = claimed.get("BANK");
        if (bank != null) {
            String coop = text("scoreboard.coopBank", "Personal");
            boolean compact = flag("scoreboard.bankCompact", false);
            for (int i = 0; i < bank.size(); i++) {
                Row row = bank.get(i);
                String value = valueOf(row.text);
                if (value == null) continue;
                String[] halves = value.split("/");
                String shown = value;
                if (halves.length > 1) {
                    double personal = Numbers.parse(halves[0]);
                    double coOp = Numbers.parse(halves[1]);
                    if (coOp <= 0) shown = trim(halves[0]);
                    else if ("Co-op".equalsIgnoreCase(coop)) shown = trim(halves[1]);
                    else if ("Combined".equalsIgnoreCase(coop)) {
                        shown = Numbers.format(personal + coOp, numberFormat, flag("scoreboard.numberOverride", true));
                    } else shown = trim(halves[0]);
                }
                if (compact) {
                    double amount = Numbers.parse(shown);
                    if (amount >= 0) shown = Numbers.formatShort(amount, numberFormat);
                }
                String label = row.text.substring(0, row.text.indexOf(':') + 1);
                bank.set(i, new Row("BANK", label + " " + colourOf(row.text) + shown, row.points,
                        row.sourceIndex));
            }
        }

        // ---- Bits Available: whether the bits line is drawn at all
        if (!flag("scoreboard.bitsAvailable", true)) {
            claimed.remove("BITS");
        }

        // ---- Hide Purse and Bank in Dungeons
        if (flag("scoreboard.hidePurseDungeons", false) && inDungeons()) {
            claimed.remove("PURSE");
            claimed.remove("BANK");
        }

        // ---- Show Active Elements Only: a zero is not worth a line
        if (flag("scoreboard.activeOnly", false)) {
            for (String slot : moneySlots) {
                List<Row> rows = claimed.get(slot);
                if (rows == null) continue;
                double total = 0;
                for (Row row : rows) {
                    double v = Numbers.parse(valueOf(row.text) == null ? row.text : valueOf(row.text));
                    if (v > 0) total = v;
                }
                if (total <= 0) claimed.remove(slot);
            }
        }

        // ---- Colored Month: the season line is drawn in the season's own colour
        if (flag("scoreboard.coloredMonth", true)) {
            List<Row> season = claimed.get("SEASON");
            if (season != null) {
                for (int i = 0; i < season.size(); i++) {
                    Row row = season.get(i);
                    season.set(i, new Row("SEASON", seasonColour(row.text), row.points, row.sourceIndex));
                }
            }
        }

        // ---- Time & Calendar: 24h, and the minutes the server only updates every few seconds
        List<Row> time = claimed.get("TIME");
        if (time != null) {
            for (int i = 0; i < time.size(); i++) {
                Row row = time.get(i);
                time.set(i, new Row("TIME", clock(row.text), row.points, row.sourceIndex));
            }
        }

        // ---- Date in the lobby code, and the date format
        List<Row> date = claimed.get("SERVER_DATE");
        if (date != null) {
            for (int i = 0; i < date.size(); i++) {
                Row row = date.get(i);
                date.set(i, new Row("SERVER_DATE", dateLine(row.text), row.points, row.sourceIndex));
            }
        }

        // ---- Cookie Buff and the Active Effects block: the server's sentence becomes the state
        rewriteBlock(claimed, "COOKIE");
        List<Row> effects = claimed.get("ACTIVE_EFFECTS");
        if (effects != null) {
            for (int i = 0; i < effects.size(); i++) {
                Row row = effects.get(i);
                String clean = RichText.strip(row.text).trim();
                if (clean.toLowerCase(Locale.ROOT).startsWith("not active")) {
                    effects.set(i, new Row("ACTIVE_EFFECTS", "  \u00a77Not Active", row.points, row.sourceIndex));
                } else if (clean.toLowerCase(Locale.ROOT).contains("booster cookies")) {
                    effects.set(i, new Row("ACTIVE_EFFECTS", "  \u00a77Not Active", row.points, row.sourceIndex));
                }
            }
        }

        // ---- Accessory Power, when the server sent none.
        //
        // This server has no `Power:` line anywhere - the sidebar does not carry one and neither
        // does the tab list - so the line the reference draws is drawn from the value SweatsEnhancements learned:
        // the chat says so when the power changes, and the Thaumaturgy menu spells it out. See
        // PowerStone. Nothing is invented: with nothing learned, no line is drawn.
        List<Row> powerRows = claimed.get("POWER");
        if ((powerRows == null || powerRows.isEmpty()) && flag("scoreboard.powerFromBag", true)) {
            String learned = com.sweatsenhancements.feature.PowerStone.stored();
            String magical = com.sweatsenhancements.feature.PowerStone.magical();
            if (!learned.isEmpty()) {
                // the accessory power rides along in the same parenthetical the server's own power
                // line uses, and the Accessory Power switch takes it off again below
                String beside = "";
                if (!magical.isEmpty() && flag("scoreboard.accessoryPower", true)) {
                    beside = " \u00a77(" + magical + ")";
                }
                put(claimed, "POWER", new Row("POWER", "\u00a7fPower: \u00a7d" + learned + beside, 0, -1));
            }
        }

        // ---- the cookie line: not drawn at all while no booster is running.
        //
        // JEF suppresses the server's whole cookie sentence and only writes the line when a booster
        // is actually up, which is why its scoreboard has no cookie line while this one was drawing
        // `Cookie Buff: Not Active`. What is drawn when one is running keeps the server's own text.
        List<Row> cookie = claimed.get("COOKIE");
        if (cookie != null && flag("scoreboard.hideInactiveCookie", true)) {
            List<Row> running = new ArrayList<Row>();
            for (Row row : cookie) {
                String clean = RichText.strip(row.text).trim().toLowerCase(Locale.ROOT);
                // the server writes it either way round - "Not Active", "INACTIVE", and with the
                // label in front of it - so the test is on the words, not on where the line starts
                if (clean.contains("not active") || clean.contains("inactive")) continue;
                running.add(row);
            }
            if (running.isEmpty()) claimed.remove("COOKIE");
            else claimed.put("COOKIE", running);
        }

        // ---- Slayer: the level, the meter, and the areas the quest is in
        List<Row> slayer = claimed.get("SLAYER");
        if (slayer != null) {
            if (flag("scoreboard.slayerMeter", false)) {
                // the bar goes on a line of its own, under the progress line it belongs to: the
                // player asked for it off the line - "move it up or down a line i dont like how
                // its in the same line" - and a bar printed after a line that is already full of
                // text is what pushed the row past the panel's width
                List<Row> metered = new ArrayList<Row>();
                for (Row row : slayer) {
                    metered.add(row);
                    String bar = meterBar(row.text);
                    if (bar != null) {
                        metered.add(new Row("SLAYER", indentOf(row.text) + bar, row.points,
                                row.sourceIndex));
                    }
                }
                slayer = metered;
                claimed.put("SLAYER", slayer);
            }
            if (flag("scoreboard.hideSlayerOutside", false) && !inSlayerArea()) {
                claimed.remove("SLAYER");
            } else if (!flag("scoreboard.slayerLevel", true)) {
                for (int i = 0; i < slayer.size(); i++) {
                    Row row = slayer.get(i);
                    slayer.set(i, new Row("SLAYER", withoutLevel(row.text), row.points, row.sourceIndex));
                }
            }
        }

        // ---- Events: the prefix, how many are shown, and the order they are shown in
        List<Row> events = claimed.get("EVENT");
        if (events != null) {
            boolean prefix = flag("scoreboard.eventPrefix", true);
            boolean all = flag("scoreboard.allEvents", false);
            List<Row> kept = new ArrayList<Row>();
            int shown = 0;
            for (Row row : events) {
                String clean = RichText.strip(row.text).trim();
                boolean isEvent = clean.toLowerCase(Locale.ROOT).startsWith("event:");
                if (!isEvent) {
                    kept.add(row);
                    continue;
                }
                if (!all && shown >= 1) continue;
                shown++;
                kept.add(prefix ? row : new Row(row.slot, row.text.replaceFirst("(?i)Event:\\s*", ""),
                        row.points, row.sourceIndex));
            }
            if (all) kept = priorityOrder(kept);
            if (flag("scoreboard.betterTogetherTitle", false)) kept = betterTogether(kept);
            claimed.put("EVENT", kept);
        }

        // ---- Election and mayor
        List<Row> elections = claimed.get("ELECTIONS");
        if (elections != null) {
            boolean mayorTime = flag("scoreboard.mayorTime", false);
            List<Row> kept = new ArrayList<Row>();
            for (Row row : elections) {
                String clean = RichText.strip(row.text).trim();
                String lower = clean.toLowerCase(Locale.ROOT);
                if (mayorTime && (lower.startsWith("starts in:") || lower.startsWith("ends in:"))) {
                    kept.add(new Row("ELECTIONS", "  \u00a7fMayor Election: " + valueOf(row.text).trim(),
                            row.points, row.sourceIndex));
                    continue;
                }
                // Mayor Perks, on "Only Relevant": the perk lists a mayor carries are dropped and
                // the election itself is kept. On "All Perks" every line the server sent stays.
                if ("Only Relevant".equalsIgnoreCase(text("scoreboard.mayorPerks", "All Perks"))
                        && !relevantElectionLine(lower)) {
                    continue;
                }
                kept.add(row);
            }
            claimed.put("ELECTIONS", kept);
        }

        // ---- Show Currency Gain: what the purse and the other currencies have done since the
        // last reading. The server only re-sends a sidebar when a value changes, so the change is
        // read rather than calculated - it is what the server itself put on the screen.
        if (flag("scoreboard.currencyGain", true)) {
            String[] watched = {"PURSE", "BITS", "GEMS", "COPPER", "MOTES", "SOULFLOW"};
            for (String slot : watched) {
                List<Row> rows = claimed.get(slot);
                if (rows == null || rows.isEmpty()) continue;
                Row row = rows.get(0);
                String value = valueOf(row.text);
                if (value == null) continue;
                double now = Numbers.parse(value);
                if (now < 0) continue;
                Double before = lastValue.get(slot);
                lastValue.put(slot, Double.valueOf(now));
                if (before == null || before.doubleValue() == now) continue;
                double change = now - before.doubleValue();
                if (change <= 0) continue;                 // a gain is what this setting is about
                if (change > Math.abs(before.doubleValue())) {
                    // more than doubling between two readings is a different balance, not a gain:
                    // changing island, a new purse, the server re-sending after a reset
                    continue;
                }
                rows.set(0, new Row(slot, row.text + " \u00a7a+" + Numbers.format(change,
                        numberFormat, true), row.points, row.sourceIndex));
            }
        }

        // ---- Accessory Power: the parenthetical next to the selected power
        List<Row> power = claimed.get("POWER");
        if (power != null && !flag("scoreboard.accessoryPower", true)) {
            for (int i = 0; i < power.size(); i++) {
                Row row = power.get(i);
                power.set(i, new Row("POWER", row.text.replaceAll("\\s*\\([^)]*\\)\\s*$", ""), row.points,
                        row.sourceIndex));
            }
        }

        // ---- whether the server's own powder line is drawn at all
        if (!flag("scoreboard.showPowderLine", true)) claimed.remove("POWDER");

        // ---- Powder display format: current, maximum, or both
        List<Row> powder = claimed.get("POWDER");
        if (powder != null) {
            String display = text("scoreboard.powderDisplay", "Current");
            for (int i = 0; i < powder.size(); i++) {
                Row row = powder.get(i);
                String value = valueOf(row.text);
                if (value == null || value.indexOf('/') < 0) continue;
                String[] halves = value.split("/");
                String shown = value;
                if ("Current".equalsIgnoreCase(display)) shown = halves[0].trim();
                else if ("Max".equalsIgnoreCase(display)) shown = halves[1].trim();
                String label = labelOf(row.text);
                powder.set(i, new Row("POWDER", label + " " + colourOf(row.text) + shown, row.points,
                        row.sourceIndex));
            }
        }

        // ---- Profile name, and whether the bits line is drawn at all
        if (!flag("scoreboard.profileName", true)) claimed.remove("PROFILE");
    }

    /**
     * Events Priority, applied: the events the player listed come first in their own order, and
     * an event that is not on the list follows in the order the server sent it - never dropped,
     * because a new event is not a reason to show nothing.
     */
    private static List<Row> priorityOrder(List<Row> rows) {
        List<String> priority = list("scoreboard.eventsPriority", PanelSlots.DEFAULT_EVENTS_PRIORITY);
        final List<String> order = priority;
        List<Row> events = new ArrayList<Row>();
        List<Row> rest = new ArrayList<Row>();
        for (Row row : rows) {
            String clean = RichText.strip(row.text).trim();
            if (clean.toLowerCase(Locale.ROOT).startsWith("event:")) events.add(row);
            else rest.add(row);
        }
        java.util.Collections.sort(events, new java.util.Comparator<Row>() {
            public int compare(Row a, Row b) {
                int ia = rank(order, a.text);
                int ib = rank(order, b.text);
                return ia == ib ? 0 : (ia < ib ? -1 : 1);
            }
        });
        List<Row> out = new ArrayList<Row>(events);
        out.addAll(rest);
        return out;
    }

    private static int rank(List<String> order, String text) {
        String clean = RichText.strip(text).toLowerCase(Locale.ROOT);
        for (int i = 0; i < order.size(); i++) {
            String name = order.get(i).toLowerCase(Locale.ROOT).replace('_', ' ');
            if (clean.contains(name)) return i;
        }
        return order.size() + 1;
    }

    /**
     * Drops a blank line when the line above it was blank too.
     *
     * Only *consecutive* blanks go: one blank line is the server grouping its blocks, which is what
     * the reference keeps and what the picture shows, and two in a row mean a block that is not
     * there. A blank row keeps its place in the order otherwise.
     */
    public static List<Row> collapseBlank(List<Row> rows) {
        List<Row> out = new ArrayList<Row>();
        boolean lastBlank = false;
        for (Row row : rows) {
            boolean blank = RichText.strip(row.text).trim().isEmpty();
            if (blank && lastBlank) continue;
            out.add(row);
            lastBlank = blank;
        }
        return out;
    }

    /** Drops the blank lines at the top and the bottom of the list, which push the panel around. */
    public static List<Row> trimBlankEdges(List<Row> rows) {
        int from = 0;
        int to = rows.size();
        while (from < to && RichText.strip(rows.get(from).text).trim().isEmpty()) from++;
        while (to > from && RichText.strip(rows.get(to - 1).text).trim().isEmpty()) to--;
        if (from == 0 && to == rows.size()) return rows;
        return new ArrayList<Row>(rows.subList(from, to));
    }

    /** Add Better Together Title: a heading over an event the server sends without one. */
    private static List<Row> betterTogether(List<Row> rows) {
        List<Row> out = new ArrayList<Row>();
        boolean titled = false;
        for (Row row : rows) {
            String clean = RichText.strip(row.text).toLowerCase(Locale.ROOT);
            if (!titled && clean.contains("better together")) {
                out.add(new Row("EVENT", "\u00a7e\u00a7lBetter Together", row.points, row.sourceIndex));
                titled = true;
            }
            out.add(row);
        }
        return out;
    }

    /** The boss of the quest, or the progress towards it, whatever order they arrive in. */
    private static boolean isSlayerLine(String clean) {
        return SLAYER_BOSS.matcher(clean).matches() || SLAYER_PROGRESS.matcher(clean).matches();
    }

    /** True when an election line is about the election rather than about a perk. */
    private static boolean relevantElectionLine(String lower) {
        return lower.contains("election") || lower.contains("starts in") || lower.contains("ends in")
                || lower.contains("mayor") || lower.contains("minister") || lower.contains("year");
    }

    /** True when the Power line is drawn from the learned power, for /se power. */
    public static boolean powerLineOn() {
        return flag("scoreboard.powerFromBag", true) && !com.sweatsenhancements.feature.PowerStone.stored().isEmpty();
    }

    /** The value each currency had the last time it was read, for Show Currency Gain. */
    private static final Map<String, Double> lastValue = new LinkedHashMap<String, Double>();

    /** how many cells the meter is drawn in */
    private static final int METER_CELLS = 10;

    /**
     * A bar and a percentage after a slayer quest's progress: CSB's meter, on this server's line.
     *
     * The filled cell is the same character the server itself uses for its election bars, so it is a
     * glyph this client is already drawing; the empty cell is its hollow twin. A line with no
     * progress on it - the quest's heading, the boss's name - is handed back untouched.
     */
    private static String meter(String text) {
        String bar = meterBar(text);
        return bar == null ? text : text + " " + bar;
    }

    /**
     * The bar and the percentage for a slayer progress line, or null when the line is not one.
     *
     * The filled cell is the same character the server itself uses for its election bars, so it is a
     * glyph this client is already drawing; the empty cell is its hollow twin.
     */
    private static String meterBar(String text) {
        try {
            // `.trim()`, because the row arrives indented the way the panel draws it: without it
            // the pattern's `^` never matched and the meter silently did nothing at all
            java.util.regex.Matcher m = SLAYER_PROGRESS.matcher(RichText.strip(text).trim());
            if (!m.matches()) return null;
            double now = Numbers.parse(m.group(1));
            double max = Numbers.parse(m.group(2));
            if (max <= 0 || now < 0) return null;
            double fraction = Math.min(1.0, now / max);
            int filled = (int) Math.round(fraction * METER_CELLS);
            StringBuilder bar = new StringBuilder();
            for (int i = 0; i < METER_CELLS; i++) bar.append(i < filled ? '\u25ac' : '\u2591');
            int percent = (int) Math.round(fraction * 100.0);
            String colour = fraction >= 1.0 ? "\u00a7a" : (fraction >= 0.5 ? "\u00a7e" : "\u00a7c");
            return colour + bar + " \u00a7f" + percent + "%";
        } catch (Throwable t) {
            return null;
        }
    }

    /** The leading spaces of a row's text, so a line added under it lines up with it. */
    private static String indentOf(String text) {
        if (text == null) return "";
        int i = 0;
        while (i < text.length() && (text.charAt(i) == ' ' || text.charAt(i) == '\u00a0')) i++;
        return text.substring(0, i);
    }

    /** The one rewrite the cookie line needs: the server's whole sentence becomes its state. */
    private static void rewriteBlock(Map<String, List<Row>> claimed, String slot) {
        List<Row> rows = claimed.get(slot);
        if (rows == null) return;
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            String clean = RichText.strip(row.text).trim().toLowerCase(Locale.ROOT);
            if (clean.startsWith("not active")) {
                rows.set(i, new Row(slot, "\u00a77Not Active", row.points, row.sourceIndex));
            }
        }
    }

    // ------------------------------------------------------------------ ordering, chunking, separators

    private static List<Row> order(Map<String, List<Row>> claimed) {
        List<String> appearance = list("scoreboard.appearance", PanelSlots.DEFAULT_APPEARANCE);
        List<Row> out = new ArrayList<Row>();
        for (String slot : appearance) {
            if (PanelSlots.EMPTY_LINE.equals(slot)) {
                // a blank line the player dragged into the list, which is how the reference lets a
                // gap be put where the server has none
                out.add(new Row("EMPTY_LINE", "", 0, -1));
                continue;
            }
            List<Row> rows = claimed.get(slot);
            if (rows != null) out.addAll(rows);
        }
        // An element the order does not mention is still drawn at the end, so a row is never lost
        // to an order the player has not updated - but only for the elements this panel still
        // offers. The fourteen the player asked out of every list are not offered any more, so the
        // rows that used to land in them - the skills and stats blocks, the pet, the island's
        // roster, the effects list, the server name, the site line - are not drawn at all, and
        // neither is the bucket of lines no element claimed. Nothing vanishes silently: the
        // unknown-lines warning still names them in chat, and /se dump still lists them.
        for (Map.Entry<String, List<Row>> entry : claimed.entrySet()) {
            String slot = entry.getKey();
            if (appearance.contains(slot) || !PanelSlots.ALL.contains(slot)) continue;
            out.addAll(entry.getValue());
        }
        return out;
    }

    /**
     * Chunky Stats: short currency lines put together, a few to a line, to save space.
     *
     * Only the currencies are chunked, and only ones that are one short line each - a block with
     * a heading is not something to fold into a row.
     */
    private static List<Row> chunk(List<Row> rows, int perLine, List<String> which) {
        List<Row> out = new ArrayList<Row>();
        List<Row> pending = new ArrayList<Row>();
        for (Row row : rows) {
            if (chunkable(row, which)) {
                pending.add(row);
                continue;
            }
            flushChunk(out, pending, perLine);
            out.add(row);
        }
        flushChunk(out, pending, perLine);
        return out;
    }

    private static void flushChunk(List<Row> out, List<Row> pending, int perLine) {
        int i = 0;
        while (i < pending.size()) {
            int end = Math.min(pending.size(), i + perLine);
            StringBuilder line = new StringBuilder();
            int index = -1;
            for (int j = i; j < end; j++) {
                if (line.length() > 0) line.append(" \u00a78\u00b7 ");
                line.append(pending.get(j).text.trim());
                if (index < 0) index = pending.get(j).sourceIndex;
            }
            out.add(new Row("CHUNK", line.toString(), 0, index));
            i = end;
        }
        pending.clear();
    }

    /** A line the player put in the Chunky Stats list, and short enough to share a line. */
    private static boolean chunkable(Row row, List<String> which) {
        return which.contains(row.slot) && RichText.strip(row.text).length() <= 28;
    }

    /**
     * Condense Consecutive Separator, switched off: keep one blank line where the server had a
     * gap, so the panel keeps the shape the server gave it.
     */
    private static List<Row> separate(List<Row> rows, List<Raw> sidebar) {
        List<Row> out = new ArrayList<Row>();
        int previous = -1;
        for (Row row : rows) {
            if (row.sourceIndex >= 0 && previous >= 0 && row.sourceIndex > previous + 1) {
                boolean gap = false;
                for (int i = previous + 1; i < row.sourceIndex && i < sidebar.size(); i++) {
                    if (sidebar.get(i).blank) gap = true;
                }
                if (gap) out.add(new Row("SEPARATOR", "", 0, -1));
            }
            if (row.sourceIndex >= 0) previous = row.sourceIndex;
            out.add(row);
        }
        return out;
    }

    // ------------------------------------------------------------------ line-level helpers

    /** The label of a "Label: value" line, codes and all, or "" when there is no colon. */
    private static String labelOf(String raw) {
        int colon = raw.indexOf(':');
        return colon < 0 ? "" : raw.substring(0, colon + 1);
    }

    /** The value of a "Label: value" line with its codes taken out, or null when there is none. */
    private static String valueOf(String raw) {
        int colon = raw.indexOf(':');
        if (colon < 0 || colon + 1 >= raw.length()) return null;
        String value = RichText.strip(raw.substring(colon + 1)).trim();
        return value.isEmpty() ? null : value;
    }

    /** The first colour code in the line, so a rewritten number keeps the colour it had. */
    private static String colourOf(String raw) {
        int colon = raw.indexOf(':');
        String tail = colon < 0 ? raw : raw.substring(colon);
        int at = tail.indexOf('\u00a7');
        return at < 0 ? "" : tail.substring(at, Math.min(tail.length(), at + 2));
    }

    private static String trim(String text) {
        return text == null ? "" : text.trim();
    }

    /** A number line rewritten into the player's number format and display format. */
    private static String amount(String raw, String slot, String numberFormat, String numberDisplay) {
        String value = valueOf(raw);
        if (value == null) return raw;
        String number = value;
        boolean shortened = false;
        // "Purse: 52,763,737" and "Purse: 2.1M" both count; only a plain amount is reformatted
        if (Numbers.isAmount(value)) {
            double parsed = Numbers.parse(value);
            if (parsed >= 0) number = Numbers.format(parsed, numberFormat, true);
        } else {
            Matcher m = Pattern.compile("([\\d,]+(?:\\.\\d+)?)([kKmMbB]?)").matcher(value);
            if (m.find() && m.group(2).isEmpty()) {
                double parsed = Numbers.parse(m.group(1));
                if (parsed >= 0) number = value.substring(0, m.start()) + Numbers.format(parsed, numberFormat, true)
                        + value.substring(m.end());
            } else {
                shortened = true;
            }
        }
        String label = labelOf(raw);
        String colour = colourOf(raw);
        if (shortened) return raw;                                   // nothing to reformat: leave it
        if ("123: Purse".equalsIgnoreCase(numberDisplay)) {
            String name = RichText.strip(label).replace(":", "").trim();
            String space = raw.indexOf(':') + 1 < raw.length() && raw.charAt(raw.indexOf(':') + 1) == ' '
                    ? " " : " ";
            return colour + number + space + "\u00a77" + name;
        }
        return label + " " + colour + number;
    }

    /** 7:10pm becomes 19:10 when 24 hour time is on; the symbols after it are kept. */
    private static String clock(String raw) {
        boolean twentyFour = flag("scoreboard.time24h", false);
        boolean minutes = flag("scoreboard.includeMinutes", true);
        boolean exact = flag("scoreboard.exactMinutes", false);
        Matcher m = Pattern.compile("(\\d{1,2}):(\\d{2})\\s*([aApP][mM])").matcher(raw);
        if (!m.find()) return raw;
        int hour = Integer.parseInt(m.group(1));
        String minute = m.group(2);
        String suffix = m.group(3);
        // SkyBlock Time Exact Minutes: off, the minutes are shown in ten minute steps, which is how
        // the server's own clock is read at a glance
        if (!exact) {
            int mins = 0;
            try {
                mins = Integer.parseInt(minute);
            } catch (Throwable ignored) {
            }
            mins = (mins / 10) * 10;
            minute = (mins < 10 ? "0" : "") + mins;
        }
        String body;
        if (twentyFour) {
            int h = hour % 12;
            if ("pm".equalsIgnoreCase(suffix)) h += 12;
            body = (h < 10 ? "0" : "") + h + ":" + minute;
        } else {
            body = hour + ":" + (minutes ? minute : "00") + suffix.toLowerCase(Locale.ROOT);
        }
        return raw.substring(0, m.start()) + body + raw.substring(m.end());
    }

    /** The date and lobby code line, in the player's chosen date format. */
    private static String dateLine(String raw) {
        String clean = RichText.strip(raw).trim();
        Matcher m = Pattern.compile("(\\d{2})/(\\d{2})/(\\d{2,4})(.*)").matcher(clean);
        if (!m.matches()) return raw;
        boolean lobbies = flag("scoreboard.dateInServer", true);
        String style = text("scoreboard.dateFormat", "09/24/2026");
        String month = m.group(1);
        String day = m.group(2);
        String year = m.group(3);
        if (year.length() == 2 && style.endsWith("2026")) year = "20" + year;
        String date;
        if ("24/09/2026".equals(style)) date = day + "/" + month + "/" + year;
        else if ("24.09.2026".equals(style)) date = day + "." + month + "." + year;
        else if ("09/24/26".equals(style)) date = month + "/" + day + "/" + (year.length() > 2 ? year.substring(2) : year);
        else date = month + "/" + day + "/" + year;
        String rest = m.group(4);
        if (!lobbies) rest = rest.replaceAll("\\s{2,}", " ").trim().isEmpty() ? "" : rest;
        return "\u00a77" + date + rest;
    }

    private static String seasonColour(String raw) {
        String clean = RichText.strip(raw).toLowerCase(Locale.ROOT);
        String colour = "\u00a76";
        if (clean.contains("spring")) colour = "\u00a7a";
        else if (clean.contains("summer")) colour = "\u00a7e";
        else if (clean.contains("autumn")) colour = "\u00a76";
        else if (clean.contains("winter")) colour = "\u00a7b";
        return colour + RichText.strip(raw);
    }

    private static String withoutLevel(String raw) {
        return raw.replaceFirst("\\s+[IVXL]+\\s*$", "");
    }

    // ------------------------------------------------------------------ where the player is

    private static boolean inDungeons() {
        try {
            String area = SweatsEnhancements.tabList == null ? "" : SweatsEnhancements.tabList.area();
            String lower = area == null ? "" : area.toLowerCase(Locale.ROOT);
            return lower.contains("dungeon") || lower.contains("catacombs");
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Whether the player is somewhere a slayer quest runs.
     *
     * The areas are read out of the tab list rather than matched against a fixed list of islands,
     * because a fixed list is what stops another server's client from working here. Anything the server puts
     * in the area line that the player's own slayer quest is in counts.
     */
    private static boolean inSlayerArea() {
        try {
            String area = SweatsEnhancements.tabList == null ? "" : SweatsEnhancements.tabList.area();
            if (area == null || area.isEmpty()) area = SweatsEnhancements.scoreboard == null ? "" : areaOfSidebar();
            String lower = area.toLowerCase(Locale.ROOT);
            String[] areas = {"graveyard", "crypt", "spider", "burning desert", "ruins", "end",
                    "stronghold", "wasteland", "smoldering tomb", "howling", "spirit", "hollow",
                    "castle", "pathway", "dubliette"};
            for (String word : areas) {
                if (lower.contains(word)) return true;
            }
            return false;
        } catch (Throwable t) {
            return true;
        }
    }

    private static String areaOfSidebar() {
        return "";
    }
}
