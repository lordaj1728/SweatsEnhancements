package com.arena.pekaclient.feature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure text helpers for the Spirit Leap teammate selector. No Minecraft types, so they can be unit-tested.
 *
 * Minecraft scoreboard text is assembled from team prefixes, entries and suffixes. Formatting and control
 * characters can be interleaved through those parts, so normalization is applied before matching or parsing.
 */
public final class LeapText {

    private LeapText() {
    }

    /** Removes Minecraft colour/style pairs (a section sign and its following code character). */
    public static String strip(String s) {
        if (s == null || s.indexOf('\u00a7') < 0) {
            return s == null ? "" : s;
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\u00a7') {
                if (i + 1 < s.length()) {
                    i++;
                }
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }

    /**
     * Formatting-free text used for identity and class parsing. Collapses whitespace, removes non-whitespace
     * controls and common zero-width decorations, and trims the result.
     */
    public static String normalize(String s) {
        String plain = strip(s);
        if (plain.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(plain.length());
        boolean pendingSpace = false;
        for (int i = 0; i < plain.length(); i++) {
            char c = plain.charAt(i);
            if (isSpace(c)) {
                pendingSpace = out.length() > 0;
                continue;
            }
            if (Character.isISOControl(c) || isInvisible(c)) {
                continue;
            }
            if (pendingSpace && out.length() > 0) {
                out.append(' ');
            }
            pendingSpace = false;
            out.append(c);
        }
        return out.toString();
    }

    /**
     * The class name from the first lore line that reads "Class: X", with formatting removed.
     * Returns null when no such line exists. Nothing is guessed from other lines.
     */
    public static String classFromLore(List<String> lore) {
        if (lore == null) {
            return null;
        }
        for (String line : lore) {
            String t = normalize(line);
            int colon = t.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String key = t.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            if (!"class".equals(key)) {
                continue;
            }
            String value = t.substring(colon + 1).trim();
            if (!value.isEmpty()) {
                return value;
            }
        }
        return null;
    }

    /** A bracketed group such as "(Archer XXV)" or "(Mage DEAD)": the first word is the candidate class. */
    private static final Pattern TAB_CLASS = Pattern.compile("\\(([A-Za-z]+)(?:\\s[^()]*)?\\)");

    /**
     * The class from one tab-list entry, such as "Name (Archer XXV)". The entry is used only when it names the
     * same player as the skull's owner as a whole word. Only an exact class name counts.
     */
    public static String classFromTab(String entryText, String owner) {
        if (entryText == null || owner == null || owner.isEmpty()) {
            return null;
        }
        String t = normalize(entryText);
        String name = normalize(owner);
        if (!containsWord(t, name)) {
            return null;
        }
        Matcher m = TAB_CLASS.matcher(t);
        while (m.find()) {
            LeapClass c = LeapClass.parse(m.group(1));
            if (c != null) {
                return c.label();
            }
        }
        return null;
    }

    /**
     * Class from a marker immediately before the exact owner's name, for example "[MVP+] [B] player". Rank
     * tokens are walked past but are not accepted as class markers. This method deliberately does not use substring
     * matching and does not treat a marker after the name as a prefix.
     */
    public static LeapClass classFromPrefix(String entryText, String owner) {
        if (entryText == null || owner == null || normalize(owner).isEmpty()) {
            return null;
        }
        String t = normalize(entryText);
        String name = normalize(owner);
        int from = 0;
        while (true) {
            int at = indexOfIgnoreCase(t, name, from);
            if (at < 0) {
                return null;
            }
            int end = at + name.length();
            boolean wholeWord = (at == 0 || !isNameChar(t.charAt(at - 1)))
                    && (end >= t.length() || !isNameChar(t.charAt(end)));
            if (wholeWord) {
                return markerBefore(t, at);
            }
            from = at + 1;
        }
    }

    /**
     * Class marker attached to a scoreboard line for one exact player. The class may be in a team prefix or
     * suffix. Prefix markers win; suffix markers are accepted only when the token following them is not another
     * known player's username (so "[A] alice [M] bob" never gives Alice Bob's class).
     */
    public static LeapClass classFromScoreboardLine(String entryText, String owner, Collection<String> knownNames) {
        if (entryText == null || owner == null || normalize(owner).isEmpty()) {
            return null;
        }
        String t = normalize(entryText);
        String name = normalize(owner);
        int from = 0;
        while (true) {
            int at = indexOfIgnoreCase(t, name, from);
            if (at < 0) {
                break;
            }
            int end = at + name.length();
            boolean wholeWord = (at == 0 || !isNameChar(t.charAt(at - 1)))
                    && (end >= t.length() || !isNameChar(t.charAt(end)));
            if (wholeWord) {
                LeapClass prefix = markerBefore(t, at);
                if (prefix != null) {
                    return prefix;
                }
                LeapClass suffix = markerAfter(t, end, name, knownNames);
                if (suffix != null) {
                    return suffix;
                }
            }
            from = at + 1;
        }
        return classFromTruncated(entryText, name, knownNames);
    }

    /**
     * The class from a name the server cut short with "..." (or the single ellipsis character), such as
     * "[B] anuptripah..." for owner "anuptripah_xyz". The kept part must uniquely identify that owner.
     */
    public static LeapClass classFromTruncated(String entryText, String owner, Collection<String> knownNames) {
        if (entryText == null || owner == null || knownNames == null) {
            return null;
        }
        String full = normalize(owner);
        String body = normalize(entryText);
        if (body.endsWith("\u2026")) {
            body = body.substring(0, body.length() - 1).trim();
        } else if (body.endsWith("...")) {
            body = body.substring(0, body.length() - 3).trim();
        } else {
            return null;
        }
        int end = body.length();
        int start = end;
        while (start > 0 && isNameChar(body.charAt(start - 1))) {
            start--;
        }
        String kept = body.substring(start, end);
        if (kept.length() < 3 || kept.length() >= full.length()
                || !full.regionMatches(true, 0, kept, 0, kept.length())) {
            return null;
        }
        if (start > 0 && body.charAt(start - 1) != ' ' && body.charAt(start - 1) != ']') {
            return null;
        }
        if (!isUniqueStart(kept, full, knownNames)) {
            return null;
        }
        return markerBefore(body, start);
    }

    /** Name equality for identity fields: formatting removed, whitespace normalized, case-insensitive. */
    public static boolean sameName(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        String x = normalize(a);
        String y = normalize(b);
        return !x.isEmpty() && x.equalsIgnoreCase(y);
    }

    /**
     * Keeps the formatted rank prefix, removes a dungeon class token and trailing tab-list details, and ends the
     * rendered label at the exact canonical username. If the display cannot be tied to that username, only the
     * canonical username is returned.
     */
    public static String playerDisplayName(String formatted, String username) {
        String name = LeapIdentity.canonicalUsername(username);
        if (name == null) {
            return normalize(formatted);
        }
        if (formatted == null || formatted.isEmpty()) {
            return name;
        }
        VisibleText visible = visibleText(formatted);
        int at = indexOfIgnoreCase(visible.text, name, 0);
        if (at < 0) {
            return name;
        }
        int end = at + name.length();
        boolean wholeWord = (at == 0 || !isNameChar(visible.text.charAt(at - 1)))
                && (end >= visible.text.length() || !isNameChar(visible.text.charAt(end)));
        if (!wholeWord) {
            return name;
        }
        int rawEnd = visible.rawEnds.get(end - 1);
        int[] marker = classMarkerRangeBefore(visible.text, at);
        String result;
        if (marker == null) {
            result = formatted.substring(0, rawEnd);
        } else {
            int rawMarkerStart = visible.rawStarts.get(marker[0]);
            int rawMarkerEnd = visible.rawEnds.get(marker[1] - 1);
            result = formatted.substring(0, rawMarkerStart) + formatted.substring(rawMarkerEnd, rawEnd);
        }
        result = cleanFormattedText(result);
        return normalize(result).isEmpty() ? name : result;
    }

    private static String cleanFormattedText(String formatted) {
        StringBuilder out = new StringBuilder(formatted.length());
        for (int i = 0; i < formatted.length(); i++) {
            char c = formatted.charAt(i);
            if (c == '\u00a7') {
                if (i + 1 < formatted.length()) {
                    out.append(c).append(formatted.charAt(++i));
                }
                continue;
            }
            if (Character.isISOControl(c) || isInvisible(c)) {
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }

    private static VisibleText visibleText(String formatted) {
        StringBuilder text = new StringBuilder(formatted.length());
        List<Integer> starts = new ArrayList<Integer>();
        List<Integer> ends = new ArrayList<Integer>();
        for (int i = 0; i < formatted.length(); i++) {
            char c = formatted.charAt(i);
            if (c == '\u00a7') {
                if (i + 1 < formatted.length()) {
                    i++;
                }
                continue;
            }
            if (Character.isISOControl(c) || isInvisible(c)) {
                continue;
            }
            text.append(c);
            starts.add(i);
            ends.add(i + 1);
        }
        return new VisibleText(text.toString(), starts, ends);
    }

    private static int[] classMarkerRangeBefore(String text, int nameStart) {
        int i = nameStart;
        while (i > 0) {
            if (isSpace(text.charAt(i - 1))) {
                i--;
                continue;
            }
            if (text.charAt(i - 1) != ']') {
                return null;
            }
            int close = i - 1;
            int open = text.lastIndexOf('[', close);
            if (open < 0) {
                return null;
            }
            String token = text.substring(open + 1, close).trim();
            if (token.length() == 1 && LeapClass.fromMarker(token.charAt(0)) != null) {
                return new int[]{open, close + 1};
            }
            i = open;
        }
        return null;
    }

    private static final class VisibleText {
        final String text;
        final List<Integer> rawStarts;
        final List<Integer> rawEnds;

        VisibleText(String text, List<Integer> rawStarts, List<Integer> rawEnds) {
            this.text = text;
            this.rawStarts = rawStarts;
            this.rawEnds = rawEnds;
        }
    }

    /** True when exactly one distinct known username starts with {@code kept}, and it is {@code owner}. */
    static boolean isUniqueStart(String kept, String owner, Collection<String> names) {
        if (names == null) {
            return false;
        }
        Set<String> matches = new HashSet<String>();
        String prefix = normalize(kept).toLowerCase(Locale.ROOT);
        String expected = normalize(owner).toLowerCase(Locale.ROOT);
        for (String n : names) {
            String name = LeapIdentity.canonicalUsername(n);
            if (name != null && name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                matches.add(name.toLowerCase(Locale.ROOT));
            }
        }
        return matches.size() == 1 && matches.contains(expected);
    }

    /**
     * Walks back over whitespace and bracketed tags before {@code nameStart}. Returns the first class marker and
     * ignores rank tags such as [VIP] or [MVP+].
     */
    static LeapClass markerBefore(String t, int nameStart) {
        int i = nameStart;
        while (i > 0) {
            char c = t.charAt(i - 1);
            if (Character.isWhitespace(c)) {
                i--;
                continue;
            }
            if (c != ']') {
                return null;
            }
            int open = t.lastIndexOf('[', i - 1);
            if (open < 0) {
                return null;
            }
            String inside = t.substring(open + 1, i - 1).trim();
            if (inside.length() == 1) {
                LeapClass cls = LeapClass.fromMarker(inside.charAt(0));
                if (cls != null) {
                    return cls;
                }
            }
            i = open;
        }
        return null;
    }

    private static LeapClass markerAfter(String text, int nameEnd, String owner, Collection<String> knownNames) {
        int i = nameEnd;
        while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
            i++;
        }
        while (i < text.length() && text.charAt(i) == '[') {
            int close = text.indexOf(']', i + 1);
            if (close < 0) {
                return null;
            }
            String inside = text.substring(i + 1, close).trim();
            LeapClass cls = inside.length() == 1 ? LeapClass.fromMarker(inside.charAt(0)) : null;
            if (cls != null) {
                int next = close + 1;
                while (next < text.length() && Character.isWhitespace(text.charAt(next))) {
                    next++;
                }
                // A rank token may separate this marker from a following player's name in a combined line.
                while (next < text.length() && text.charAt(next) == '[') {
                    int rankEnd = text.indexOf(']', next + 1);
                    if (rankEnd < 0) {
                        break;
                    }
                    next = rankEnd + 1;
                    while (next < text.length() && Character.isWhitespace(text.charAt(next))) {
                        next++;
                    }
                }
                int wordEnd = next;
                while (wordEnd < text.length() && isNameChar(text.charAt(wordEnd))) {
                    wordEnd++;
                }
                if (wordEnd > next && isKnownDifferentName(text.substring(next, wordEnd), owner, knownNames)) {
                    return null;
                }
                return cls;
            }
            i = close + 1;
            while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
                i++;
            }
        }
        return null;
    }

    private static boolean isKnownDifferentName(String candidate, String owner, Collection<String> knownNames) {
        if (knownNames == null) {
            return false;
        }
        String canonical = LeapIdentity.canonicalUsername(candidate);
        if (canonical == null || canonical.equalsIgnoreCase(owner)) {
            return false;
        }
        for (String known : knownNames) {
            if (LeapIdentity.sameUsername(canonical, known)) {
                return true;
            }
        }
        return false;
    }

    /** True when {@code word} appears in {@code text} with no username character on either side. */
    static boolean containsWord(String text, String word) {
        if (word.isEmpty()) {
            return false;
        }
        int from = 0;
        while ((from = indexOfIgnoreCase(text, word, from)) >= 0) {
            int end = from + word.length();
            boolean before = from == 0 || !isNameChar(text.charAt(from - 1));
            boolean after = end >= text.length() || !isNameChar(text.charAt(end));
            if (before && after) {
                return true;
            }
            from = end;
        }
        return false;
    }

    private static int indexOfIgnoreCase(String text, String word, int from) {
        int last = text.length() - word.length();
        for (int i = Math.max(0, from); i <= last; i++) {
            if (text.regionMatches(true, i, word, 0, word.length())) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isNameChar(char c) {
        return c == '_' || Character.isLetterOrDigit(c);
    }

    private static boolean isSpace(char c) {
        return Character.isWhitespace(c) || Character.isSpaceChar(c);
    }

    private static boolean isInvisible(char c) {
        int type = Character.getType(c);
        return c == '\u200b' || c == '\u200c' || c == '\u200d' || c == '\u2060'
                || c == '\u00ad' || c == '\ufeff' || type == Character.FORMAT
                || type == Character.PRIVATE_USE || type == Character.SURROGATE;
    }

    /** True when a container title, without formatting or control characters, is the Spirit Leap menu. */
    public static boolean isLeapTitle(String title) {
        return "Spirit Leap".equals(normalize(title));
    }
}
