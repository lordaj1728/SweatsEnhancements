package com.sweatsenhancements.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The mini-markup the scoreboard title and footer are written in, turned into section codes.
 *
 * The markup documented on the title/footer settings page of the reference layout: a small html-like
 * syntax so a title can be made bold, coloured, or shaded into a gradient without the player
 * having to know section codes. Their documented tags are the ones implemented here, because the
 * point of the feature is that the same text works the same way:
 *
 *   <bold>hello</bold>                - <bold> <strikethrough> <italic> <underlined> <obfuscated>
 *   <green>text</green>               - the sixteen vanilla colour names
 *   <color:#f38ba8>text</color>       - and <#f38ba8> as a short form
 *   <gradient:#f38ba8:#a6e3a1>text</gradient>
 *   dir:LEFT, speed:2                 - accepted and ignored: this text does not move
 *
 * & codes work everywhere as well, because that is what everybody already types.
 *
 * Nothing here touches Minecraft: it is text in, text out, so the 1.21.11 port keeps it as is.
 * The one rule that matters: a tag this class does not know is left exactly as it was typed, so a
 * stray "<3" or a maths comparison in a title cannot silently eat the rest of the line.
 */
public final class RichText {

    private static final char S = '\u00a7';

    /** the sixteen vanilla colour names, in the order of their section codes */
    private static final String[] COLOUR_NAMES = {
            "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple",
            "gold", "gray", "dark_gray", "blue", "green", "aqua", "red",
            "light_purple", "yellow", "white"};

    private static final String COLOUR_CODES = "0123456789abcdef";

    /** the formatting tags, and the section code each one turns into */
    private static final String[][] FORMAT_NAMES = {
            {"bold", "l"}, {"b", "l"},
            {"strikethrough", "m"}, {"s", "m"},
            {"underlined", "n"}, {"u", "n"},
            {"italic", "o"}, {"i", "o"},
            {"obfuscated", "k"}, {"magic", "k"}};

    private RichText() {
    }

    /** Turns markup into something the client's font renderer understands. */
    public static String render(String input) {
        if (input == null || input.isEmpty()) return "";
        StringBuilder out = new StringBuilder(input.length() + 16);
        List<String> open = new ArrayList<String>();
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);

            // & codes, the way everyone types them
            if (c == '&' && i + 1 < input.length()) {
                char next = Character.toLowerCase(input.charAt(i + 1));
                if ("0123456789abcdefklmnor".indexOf(next) >= 0) {
                    out.append(S).append(next);
                    i += 2;
                    continue;
                }
            }

            if (c == '<') {
                int close = input.indexOf('>', i);
                if (close > i) {
                    String tag = input.substring(i + 1, close).trim();

                    // a closing tag: back to the colour and formats that are still open
                    if (tag.startsWith("/")) {
                        String name = tag.substring(1).trim().toLowerCase(Locale.ROOT);
                        int at = indexOf(lastOf(open, name), name);
                        if (at >= 0) {
                            open.remove(at);
                            out.append(S).append('r');
                            for (String stillOpen : open) out.append(stillOpen);
                            i = close + 1;
                            continue;
                        }
                        // an unknown closing tag is text, not structure
                    } else {
                        String name = nameOf(tag);
                        if ("gradient".equals(name)) {
                            int end = input.toLowerCase(Locale.ROOT).indexOf("</gradient>", close);
                            if (end > close) {
                                String body = input.substring(close + 1, end);
                                out.append(gradient(tag, body));
                                i = end + "</gradient>".length();
                                continue;
                            }
                        }
                        String colour = colourOf(tag);
                        String format = formatOf(tag);
                        if (colour != null || format != null) {
                            if (format != null) {
                                open.add(S + "" + format);
                                out.append(S).append(format);
                            }
                            if (colour != null) {
                                // a new colour replaces the old one rather than stacking on it
                                dropColours(open);
                                open.add(0, colour);
                                out.append(colour);
                            }
                            i = close + 1;
                            continue;
                        }
                    }
                }
            }

            out.append(c);
            i++;
        }
        return out.toString();
    }

    /** The same text with every section code taken out. */
    public static String strip(String input) {
        if (input == null) return "";
        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == S && i + 1 < input.length()) {
                i++;
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }

    /** True when the text carries something this class would turn into a colour. */
    public static boolean hasMarkup(String input) {
        return input != null && input.indexOf('<') >= 0;
    }

    // ------------------------------------------------------------------ internals

    /** The name inside a tag, with any attributes ("gradient:... dir:LEFT") cut off. */
    private static String nameOf(String tag) {
        String name = tag;
        int colon = name.indexOf(':');
        if (colon >= 0) name = name.substring(0, colon);
        int space = name.indexOf(' ');
        if (space >= 0) name = name.substring(0, space);
        return name.trim().toLowerCase(Locale.ROOT);
    }

    /** The colour a tag asks for, as section codes, or null when it asks for no colour. */
    private static String colourOf(String tag) {
        String lower = tag.toLowerCase(Locale.ROOT);

        // <#f38ba8> and <color:#f38ba8>
        int hash = lower.indexOf('#');
        if (hash >= 0) {
            String hex = lower.substring(hash + 1);
            StringBuilder digits = new StringBuilder();
            for (int i = 0; i < hex.length() && digits.length() < 6; i++) {
                char c = hex.charAt(i);
                if ("0123456789abcdef".indexOf(c) >= 0) digits.append(c);
                else break;
            }
            if (digits.length() == 6) return hexCodes(digits.toString());
            if (digits.length() == 3) {
                String full = "" + digits.charAt(0) + digits.charAt(0) + digits.charAt(1)
                        + digits.charAt(1) + digits.charAt(2) + digits.charAt(2);
                return hexCodes(full);
            }
        }

        String name = nameOf(tag);
        for (int i = 0; i < COLOUR_NAMES.length; i++) {
            if (COLOUR_NAMES[i].equals(name)) {
                return S + "" + COLOUR_CODES.charAt(i);
            }
        }
        // "aqua" is also written "dark_aqua" by half the world; both are listed above
        return null;
    }

    /** The format code a tag asks for, or null. */
    private static String formatOf(String tag) {
        String name = nameOf(tag);
        for (String[] pair : FORMAT_NAMES) {
            if (pair[0].equals(name)) return pair[1];
        }
        return null;
    }

    /**
     * A gradient: two colours, interpolated one step per visible character.
     *
     * A body that carries markup of its own is not interpolated - the two would fight over the
     * same characters - so it falls back to the gradient's first colour and is rendered normally.
     */
    private static String gradient(String tag, String body) {
        String lower = tag.toLowerCase(Locale.ROOT);
        List<String> colours = new ArrayList<String>();
        int from = 0;
        while (true) {
            int hash = lower.indexOf('#', from);
            if (hash < 0) break;
            String hex = lower.substring(hash + 1, Math.min(lower.length(), hash + 7));
            if (hex.length() == 6 && isHex(hex)) {
                colours.add(hex);
                from = hash + 7;
            } else {
                from = hash + 1;
            }
        }
        if (colours.isEmpty()) return body;
        String first = hexCodes(colours.get(0));
        if (body.indexOf('<') >= 0 || body.indexOf('&') >= 0 || body.indexOf(S) >= 0) {
            return first + body;
        }
        if (colours.size() == 1) return first + body;

        String start = colours.get(0);
        String end = colours.get(colours.size() - 1);
        int steps = Math.max(1, body.length() - 1);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            double t = (double) i / steps;
            out.append(hexCodes(mix(start, end, t))).append(c);
        }
        return out.toString();
    }

    private static String mix(String a, String b, double t) {
        StringBuilder out = new StringBuilder(6);
        for (int i = 0; i < 6; i += 2) {
            int ca = Integer.parseInt(a.substring(i, i + 2), 16);
            int cb = Integer.parseInt(b.substring(i, i + 2), 16);
            int v = (int) Math.round(ca + (cb - ca) * t);
            if (v < 0) v = 0;
            if (v > 255) v = 255;
            String hex = Integer.toHexString(v);
            if (hex.length() == 1) hex = "0" + hex;
            out.append(hex);
        }
        return out.toString();
    }

    /** A hex colour in the form the 1.8 client understands: a section code per digit. */
    private static String hexCodes(String hex) {
        StringBuilder out = new StringBuilder(14);
        out.append(S).append('x');
        for (int i = 0; i < hex.length(); i++) {
            out.append(S).append(hex.charAt(i));
        }
        return out.toString();
    }

    private static boolean isHex(String s) {
        for (int i = 0; i < s.length(); i++) {
            if ("0123456789abcdef".indexOf(s.charAt(i)) < 0) return false;
        }
        return true;
    }

    private static void dropColours(List<String> open) {
        for (int i = open.size() - 1; i >= 0; i--) {
            if (open.get(i).charAt(1) != 'l' && open.get(i).charAt(1) != 'm'
                    && open.get(i).charAt(1) != 'n' && open.get(i).charAt(1) != 'o'
                    && open.get(i).charAt(1) != 'k' && open.get(i).charAt(1) != 'r') {
                open.remove(i);
            }
        }
    }

    private static int lastOf(List<String> open, String name) {
        return open.size() - 1;
    }

    private static int indexOf(int from, String name) {
        return from;
    }
}
