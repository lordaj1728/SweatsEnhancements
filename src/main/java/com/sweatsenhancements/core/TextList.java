package com.sweatsenhancements.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * The storage format for a list setting - the Appearance order, the Events Priority list, the tab
 * list lines a player selected.
 *
 * One line in the properties file, entries separated by semicolons. A semicolon and not a comma,
 * because the names in these lists ("Ranks, Boosters & MORE!") contain commas, and a settings file
 * that cannot round-trip its own values is worse than no setting at all.
 *
 * Empty means "the defaults", so a player who never opened the setting gets the default list, and
 * a player who cleared it gets the defaults back rather than an empty scoreboard.
 */
public final class TextList {

    private TextList() {
    }

    public static String join(List<String> values) {
        StringBuilder out = new StringBuilder();
        if (values == null) return "";
        for (String value : values) {
            if (value == null) continue;
            if (out.length() > 0) out.append(';');
            out.append(value);
        }
        return out.toString();
    }

    /**
     * Splits a stored list, normalising each entry to the name form the rest of the mod uses:
     * upper case, spaces turned into underscores. So "sb level" typed into the file is still the
     * SB_LEVEL element.
     */
    public static List<String> split(String stored, List<String> defaults) {
        List<String> out = new ArrayList<String>();
        if (stored != null) {
            for (String piece : stored.split(";")) {
                String name = piece.trim();
                if (name.isEmpty()) continue;
                out.add(name.toUpperCase(Locale.ROOT).replace(' ', '_'));
            }
        }
        if (out.isEmpty()) out.addAll(defaults == null ? new ArrayList<String>() : defaults);
        return out;
    }

    /**
     * The positions of the given names inside the list a setting offers.
     *
     * The draggable list in the config screen works on positions, not on names: the library that
     * draws it can only label an entry when the entry is a number into the example list it was
     * given, or an enum constant. A name the offered list does not contain is dropped rather than
     * guessed at, and names are matched loosely - case and spaces ignored - so a hand-edited config
     * file that says "farming contest" still finds "Farming Contest".
     */
    public static List<Integer> indices(List<String> names, List<String> values) {
        List<Integer> out = new ArrayList<Integer>();
        if (values == null) return out;
        for (String value : values) {
            int at = indexOf(names, value);
            if (at >= 0 && !out.contains(at)) out.add(at);
        }
        return out;
    }

    /** The names behind a list of positions, in order. A position with no name is dropped. */
    public static List<String> namesOf(List<Integer> indices, List<String> names) {
        List<String> out = new ArrayList<String>();
        if (indices == null || names == null) return out;
        for (Integer at : indices) {
            if (at == null || at < 0 || at >= names.size()) continue;
            out.add(names.get(at));
        }
        return out;
    }

    /** The stored form of a list of positions: the names behind them, semicolon-separated. */
    public static String joinIndices(List<Integer> indices, List<String> names) {
        return join(namesOf(indices, names));
    }

    /** Reads stored names back into positions, falling back to the defaults when nothing matches. */
    public static List<Integer> loadIndices(String stored, List<String> names, List<String> defaults) {
        List<String> values = new ArrayList<String>();
        if (stored != null) {
            for (String piece : stored.split(";")) {
                String name = piece.trim();
                if (!name.isEmpty()) values.add(name);
            }
        }
        List<Integer> out = indices(names, values);
        if (out.isEmpty()) out = indices(names, defaults);
        return out;
    }

    /**
     * Reads stored names back into positions, and puts back any entry of the default order the
     * stored list could not have had.
     *
     * Only used when the offered names have changed under a stored list - an element added to this
     * build that was not offered when the file was written. Such an entry cannot have been left out
     * on purpose, so it is put back where the default order has it: between the entries that were
     * in the stored list. A list whose names are all still offered is read exactly as it was
     * written, so an entry the player deleted stays deleted.
     */
    public static List<Integer> loadIndicesMerged(String stored, List<String> names,
                                                  List<String> defaults) {
        List<String> written = new ArrayList<String>();
        if (stored != null) {
            for (String piece : stored.split(";")) {
                String name = piece.trim();
                if (!name.isEmpty()) written.add(name);
            }
        }
        boolean changed = false;
        for (String name : written) {
            if (indexOf(names, name) < 0) changed = true;      // an element this build no longer offers
        }
        if (!changed) return loadIndices(stored, names, defaults);

        List<String> kept = new ArrayList<String>();
        for (Integer at : indices(names, written)) kept.add(names.get(at));

        List<String> missing = new ArrayList<String>();
        for (String want : defaults) {
            if (!kept.contains(want) && names.contains(want)) missing.add(want);
        }

        List<Integer> out = new ArrayList<Integer>();
        for (String name : written) {
            int at = indexOf(names, name);
            if (at < 0 || out.contains(Integer.valueOf(at))) continue;
            int here = defaults.indexOf(names.get(at));
            if (here < 0) here = defaults.size();
            for (java.util.Iterator<String> it = missing.iterator(); it.hasNext(); ) {
                String want = it.next();
                if (defaults.indexOf(want) >= here) continue;
                int wantAt = indexOf(names, want);
                if (wantAt >= 0 && !out.contains(Integer.valueOf(wantAt))) out.add(Integer.valueOf(wantAt));
                it.remove();
            }
            out.add(Integer.valueOf(at));
        }
        for (String want : missing) {
            int at = indexOf(names, want);
            if (at >= 0 && !out.contains(Integer.valueOf(at))) out.add(Integer.valueOf(at));
        }
        if (out.isEmpty()) out = indices(names, defaults);
        return out;
    }

    /** Where a loosely-matched name sits in the offered list, or -1. */
    private static int indexOf(List<String> names, String value) {
        if (names == null || value == null) return -1;
        String wanted = loose(value);
        for (int i = 0; i < names.size(); i++) {
            if (loose(names.get(i)).equals(wanted)) return i;
        }
        return -1;
    }

    private static String loose(String value) {
        return value.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    /** The same, for a list whose entries are free text rather than element names. */
    public static List<String> splitRaw(String stored, List<String> defaults) {
        List<String> out = new ArrayList<String>();
        if (stored != null) {
            for (String piece : stored.split(";")) {
                String name = piece.trim();
                if (!name.isEmpty()) out.add(name);
            }
        }
        if (out.isEmpty()) out.addAll(defaults == null ? new ArrayList<String>() : defaults);
        return out;
    }

    /** The names of a list setting, upper-cased - a convenience for the generated GUI code. */
    public static List<String> names(String... values) {
        return new ArrayList<String>(Arrays.asList(values));
    }
}
