package com.sweatsenhancements.core;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Number formatting for the scoreboard.
 *
 * Three settings in the reference settings page turn into this one class: the digit grouping
 * ("1,234,567" or "1.234.567"), the number display format (which side the label goes on), and the
 * "number format override" that forces the en_US form no matter what the player's own locale is.
 * A fourth, the compact bank, shortens the number instead: 137,600,000 becomes 137.6M.
 *
 * Written by hand rather than with the localised formatters, because the player chooses the format
 * from the settings page and the machine's locale must not be able to override that choice.
 */
public final class Numbers {

    private Numbers() {
    }

    /**
     * Groups a number's digits.
     *
     * style is the exact string from the settings page, so what the dropdown shows is what the
     * scoreboard does.
     */
    public static String group(double value, String style) {
        boolean dots = style != null && style.indexOf('.') > 0 && style.startsWith("1.");
        long whole = (long) Math.floor(value);
        boolean negative = whole < 0;
        String digits = Long.toString(Math.abs(whole));
        StringBuilder out = new StringBuilder();
        int count = 0;
        for (int i = digits.length() - 1; i >= 0; i--) {
            out.append(digits.charAt(i));
            if (++count % 3 == 0 && i > 0) out.append(dots ? '.' : ',');
        }
        out.reverse();
        return (negative ? "-" : "") + out.toString();
    }

    /**
     * The short form: 1.23M, 12.3k, 1,234.
     *
     * Kept to two decimals at most and the same grouping rule as the long form, so a shortened
     * number and a full one do not disagree about how they look.
     */
    public static String shorten(double value, String style) {
        double abs = Math.abs(value);
        if (abs < 1000.0) return group(value, style);
        String suffix;
        double scale;
        if (abs >= 1.0E12) {
            suffix = "T";
            scale = 1.0E12;
        } else if (abs >= 1.0E9) {
            suffix = "B";
            scale = 1.0E9;
        } else if (abs >= 1.0E6) {
            suffix = "M";
            scale = 1.0E6;
        } else {
            suffix = "k";
            scale = 1000.0;
        }
        double small = value / scale;
        DecimalFormat format = new DecimalFormat(abs < 10 * scale ? "0.##" : "0.#",
                new DecimalFormatSymbols(Locale.US));
        String text = format.format(small);
        if (text.endsWith(".0")) text = text.substring(0, text.length() - 2);
        return text + suffix;
    }

    /**
     * A number written the way the scoreboard settings ask for it.
     *
     * override is the "number format override" switch: with it on, the format above always wins,
     * which is what the en_US locale in the reference page means in practice. With it off the
     * number is still grouped, because an ungrouped 52863737 on a sidebar is unreadable either way.
     */
    public static String format(double value, String style, boolean override) {
        return group(value, style);
    }

    /**
     * The same number, either grouped or shortened - the "always compact" bank switch.
     *
     * A shortened number keeps two decimals below ten of a unit and one above it, so 137.6M and
     * 1.2M read the same way the reference panel reads.
     */
    public static String formatShort(double value, String style) {
        return shorten(value, style);
    }

    /** True when the text is a plain amount, which is what the currency rewrites work on. */
    public static boolean isAmount(String text) {
        if (text == null || text.isEmpty()) return false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!Character.isDigit(c) && c != ',' && c != '.' && c != '-') return false;
        }
        return true;
    }

    /**
     * The number out of a string like "2,860,502", "137.6M/0", "1.23k" or "17".
     *
     * Returns -1 when there is no number at all, which is how the callers tell "no data" from
     * "a number of zero" - the difference matters, because a zero purse hides on the scoreboard
     * and a missing purse line simply is not there.
     */
    public static double parse(String text) {
        if (text == null) return -1;
        String s = text.trim();
        if (s.isEmpty()) return -1;
        // A number runs until something that is not part of one: "2,860,502" is one number, and
        // so is "137.6M" - stopping at the first comma read the purse as the digit 2.
        int start = -1;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean partOfNumber = Character.isDigit(c) || c == ',' || c == '.' || c == '-';
            if (partOfNumber) {
                if (start < 0) start = i;
            } else if (start >= 0) {
                return value(s.substring(start, i), c);
            }
        }
        if (start < 0) return -1;
        return value(s.substring(start), ' ');
    }

    private static double value(String digits, char suffix) {
        String cleaned = digits.replace(",", "").replace(" ", "");
        if (cleaned.isEmpty() || cleaned.equals("-") || cleaned.equals(".")) return -1;
        double base;
        try {
            base = Double.parseDouble(cleaned);
        } catch (Throwable t) {
            return -1;
        }
        char s = Character.toLowerCase(suffix);
        if (s == 'k') return base * 1000.0;
        if (s == 'm') return base * 1000000.0;
        if (s == 'b') return base * 1000000000.0;
        if (s == 't') return base * 1000000000000.0;
        return base;
    }
}
