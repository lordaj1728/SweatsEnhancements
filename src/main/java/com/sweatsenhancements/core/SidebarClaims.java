package com.sweatsenhancements.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The sidebar, claimed into named slots and re-emitted in the player's own order.
 *
 * This is the claim model rather than the recognise model, and it is the better fit for this server:
 * The recognise model writes about thirty kinds of line and draws the ones it knows, so a line it has
 * never seen does not appear at all. Here every line the server sent is drawn, no matter what:
 * the ones a slot claims are moved, and everything else lands in EXTRA in its original order.
 * Nothing can be lost by a pattern being wrong - a wrong pattern only moves a line, and the only
 * thing this class throws away is blank padding.
 *
 * Two rules that follow from that:
 *   - A slot that claims nothing simply does not exist in the output. There are no empty gaps
 *     where a line "should have been".
 *   - A slot name in the order that this class does not know is ignored, and a slot this class
 *     knows that the order does not mention is added at the end. The output is never shorter
 *     than the input.
 *
 * No Minecraft imports: this is plain text in, ordered text out, so the same class is what the
 * 1.21.11 port will use. Colour codes are stripped with our own pattern rather than the client's.
 */
public final class SidebarClaims {

    /** the slot that takes everything no pattern claimed */
    public static final String EXTRA = "EXTRA";

    /**
     * The slots, in the order that reads best by default on Fakepixel: where you are, then when,
     * then what you have, then the quest, then the advert, then whatever else the server added.
     */
    public static final List<String> DEFAULT_ORDER = Collections.unmodifiableList(Arrays.asList(
            "LOCATION", "SERVER", "SEASON", "TIME", "EVENT", "PURSE", "BITS", "SLAYER", "WEBSITE", EXTRA));

    /** how many lines after "Slayer Quest" belong to the slayer block */
    private static final int SLAYER_FOLLOWERS = 2;

    /** "09/22/26 hub-4", "09/23/26 hub-3" - the date and the server id, in that order */
    private static final Pattern SERVER = Pattern.compile("\\s*\\d{2}/\\d{2}/\\d{2}.*");

    /** "Late Spring 11th", "Summer 30th", "Early Autumn 3rd" */
    private static final Pattern SEASON = Pattern.compile(
            "(Early|Late|Mid)?\\s*(Spring|Summer|Autumn|Winter)\\s+\\d{1,2}(st|nd|rd|th)?.*",
            Pattern.CASE_INSENSITIVE);

    /** "4:00pm", "7:10pm" - the sun and moon symbols after it do not matter */
    private static final Pattern TIME = Pattern.compile("\\d{1,2}:\\d{2}\\s*(am|pm).*", Pattern.CASE_INSENSITIVE);

    /** the island line, which is marked with the location symbol rather than a word */
    private static final String LOCATION_SYMBOL = "\u23e3";
    /** the same symbol as some keyboards and fonts produce it */
    private static final String LOCATION_SYMBOL_ALT = "\u0444";

    private SidebarClaims() {
    }

    /**
     * Which slot a single line belongs in, or null when nothing claims it.
     *
     * Matched against the line with its colour codes taken out, so the server may colour any of
     * this however it likes. An event pattern built as a list of the other server's event names would
     * miss this server's "Event: Farming Contest"; matching the word "Event:" instead claims any
     * event this server ever adds, which is the whole point.
     */
    public static String slotOf(String raw) {
        String clean = strip(raw).trim();
        if (clean.isEmpty()) return null;

        if (clean.contains(LOCATION_SYMBOL) || clean.contains(LOCATION_SYMBOL_ALT)) return "LOCATION";
        if (SERVER.matcher(clean).matches()) return "SERVER";
        if (SEASON.matcher(clean).matches()) return "SEASON";
        if (TIME.matcher(clean).matches()) return "TIME";

        String lower = clean.toLowerCase(Locale.ROOT);
        if (lower.startsWith("event:")) return "EVENT";
        if (lower.startsWith("purse:")) return "PURSE";
        if (lower.startsWith("bits:")) return "BITS";
        if (lower.startsWith("slayer quest")) return "SLAYER";
        if (lower.contains("fakepixel")) return "WEBSITE";
        return null;
    }

    /**
     * The order the player asked for, as slot names.
     *
     * Anything may separate the names - commas, spaces, both - because this is typed into a config
     * box. Empty names are dropped, names are upper-cased, repeats are ignored (first one wins).
     */
    public static List<String> parseOrder(String configured) {
        List<String> out = new ArrayList<String>();
        if (configured == null) return out;
        String[] pieces = configured.split("[,\\s]+");
        for (String piece : pieces) {
            String name = piece.trim().toUpperCase(Locale.ROOT);
            if (!name.isEmpty() && !out.contains(name)) out.add(name);
        }
        return out;
    }

    /**
     * Claims every line, then emits the slots in the given order.
     *
     * A blank line is not claimed by anything and does not interrupt the slayer block - the block
     * is "Slayer Quest" plus the next two lines that have something on them, which is how it
     * arrives: the quest's name and its progress cannot be recognised on their own.
     */
    public static List<String> arrange(List<String> lines, List<String> order) {
        Map<String, List<String>> claimed = new LinkedHashMap<String, List<String>>();
        List<String> extra = new ArrayList<String>();
        if (lines == null) return extra;

        int i = 0;
        while (i < lines.size()) {
            String line = lines.get(i);
            if (strip(line).trim().isEmpty()) {
                // padding: the server's groups are separated by blank lines, and the order below
                // is what puts the groups back together. A blank line carries nothing, so it is
                // not a line this feature can lose.
                i++;
                continue;
            }
            String slot = slotOf(line);
            if (slot == null) {
                extra.add(line);
                i++;
                continue;
            }
            add(claimed, slot, line);
            i++;
            if ("SLAYER".equals(slot)) {
                int taken = 0;
                while (i < lines.size() && taken < SLAYER_FOLLOWERS) {
                    String next = lines.get(i);
                    if (strip(next).trim().isEmpty()) {
                        i++;
                        continue;
                    }
                    add(claimed, slot, next);
                    i++;
                    taken++;
                }
            }
        }

        List<String> wanted = new ArrayList<String>();
        List<String> requested = order == null || order.isEmpty() ? DEFAULT_ORDER : order;
        for (String name : requested) {
            String known = name.trim().toUpperCase(Locale.ROOT);
            if (!known.isEmpty() && !wanted.contains(known)) wanted.add(known);
        }
        // a slot the player forgot to name still gets drawn, at the end and in the default order
        for (String name : DEFAULT_ORDER) {
            if (!wanted.contains(name)) wanted.add(name);
        }

        List<String> out = new ArrayList<String>();
        for (String name : wanted) {
            if (EXTRA.equals(name)) {
                out.addAll(extra);
            } else {
                List<String> held = claimed.get(name);
                if (held != null) out.addAll(held);
            }
        }
        // a name in the config that this class does not know about claims nothing and draws nothing
        return out;
    }

    /** Everything a single slot holds, for the report. Empty when the slot claimed nothing. */
    public static List<String> slotLines(List<String> lines, String slot) {
        List<String> out = new ArrayList<String>();
        if (lines == null || slot == null) return out;
        String want = slot.trim().toUpperCase(Locale.ROOT);
        int i = 0;
        while (i < lines.size()) {
            String line = lines.get(i);
            String found = slotOf(line);
            if (found == null) {
                i++;
                continue;
            }
            if (found.equals(want)) out.add(line);
            i++;
            if ("SLAYER".equals(found) && found.equals(want)) {
                int taken = 0;
                while (i < lines.size() && taken < SLAYER_FOLLOWERS) {
                    String next = lines.get(i);
                    if (strip(next).trim().isEmpty()) {
                        i++;
                        continue;
                    }
                    out.add(next);
                    i++;
                    taken++;
                }
            }
        }
        return out;
    }

    /** The line without its colour codes, so a pattern never has to know them. */
    public static String strip(String raw) {
        if (raw == null) return "";
        StringBuilder out = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\u00a7' && i + 1 < raw.length()) {
                i++;
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }

    private static void add(Map<String, List<String>> claimed, String slot, String line) {
        List<String> held = claimed.get(slot);
        if (held == null) {
            held = new ArrayList<String>();
            claimed.put(slot, held);
        }
        held.add(line);
    }
}
