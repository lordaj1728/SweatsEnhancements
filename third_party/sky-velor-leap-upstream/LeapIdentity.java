package com.arena.pekaclient.feature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Pure helpers for resolving a Spirit Leap skull/display label to a Minecraft player identity.
 *
 * A displayed name is not an identity: it may include a dungeon marker, a rank, formatting, or a decorative
 * suffix. A username is accepted only when it is an exact Minecraft username or one unambiguous username token
 * in the display text. Loose substring matching is intentionally not provided.
 */
public final class LeapIdentity {

    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_]{1,16}");

    private LeapIdentity() {
    }

    /** Returns the canonical 1.8.9 username, or null if {@code value} is not one exact username. */
    public static String canonicalUsername(String value) {
        String normalized = LeapText.normalize(value);
        return USERNAME.matcher(normalized).matches() ? normalized : null;
    }

    /**
     * Finds an exact username token in a formatted/display string. Bracketed rank and class tokens are ignored.
     * If several tokens are present, a unique exact match in {@code knownNames} wins; otherwise no identity is
     * guessed. This handles labels such as "[B] [MVP+] Chimpanzee_7" without treating either prefix as a name.
     */
    public static String usernameFromDisplay(String display, Collection<String> knownNames) {
        String normalized = LeapText.normalize(display);
        if (normalized.isEmpty()) {
            return null;
        }

        List<String> candidates = tokensOutsideBrackets(normalized);
        if (candidates.isEmpty()) {
            return null;
        }

        Map<String, String> known = new HashMap<String, String>();
        if (knownNames != null) {
            for (String knownName : knownNames) {
                String canonical = canonicalUsername(knownName);
                if (canonical != null) {
                    String key = lower(canonical);
                    if (!known.containsKey(key)) {
                        known.put(key, canonical);
                    }
                }
            }
        }

        Set<String> exactKnown = new HashSet<String>();
        for (String candidate : candidates) {
            String matched = known.get(lower(candidate));
            if (matched != null) {
                exactKnown.add(lower(matched));
            }
        }
        if (exactKnown.size() == 1) {
            for (String candidate : candidates) {
                String matched = known.get(lower(candidate));
                if (matched != null) {
                    return matched;
                }
            }
        }
        if (exactKnown.size() > 1) {
            return null;
        }

        Set<String> unique = new HashSet<String>();
        String only = null;
        for (String candidate : candidates) {
            String key = lower(candidate);
            if (unique.add(key)) {
                only = candidate;
            }
        }
        return unique.size() == 1 ? only : null;
    }

    /** Exact canonical username equality; ranks, formatting and substrings are not considered. */
    public static boolean sameUsername(String left, String right) {
        String a = canonicalUsername(left);
        String b = canonicalUsername(right);
        return a != null && b != null && a.equalsIgnoreCase(b);
    }

    /** True only if the exact username occurs as a whole username token in display text. */
    public static boolean containsUsername(String display, String username) {
        String name = canonicalUsername(username);
        String text = LeapText.normalize(display);
        if (name == null || text.isEmpty()) {
            return false;
        }
        for (String token : tokensOutsideBrackets(text)) {
            if (name.equalsIgnoreCase(token)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Stable cache key: UUID when available, otherwise the canonical username. A formatted display name is never
     * accepted as a key.
     */
    public static String identityKey(UUID uuid, String username) {
        return identityKey(uuid, username, null, null);
    }

    /**
     * As above, but preserves a prior UUID key during a short tab-list gap when the exact canonical username still
     * matches. A newly observed UUID always wins, and a changed username cannot inherit the prior player's cache.
     */
    public static String identityKey(UUID uuid, String username, String previousKey, String previousUsername) {
        if (uuid != null) {
            return "uuid:" + uuid.toString().toLowerCase(Locale.ROOT);
        }
        String canonical = canonicalUsername(username);
        if (canonical == null) {
            return null;
        }
        if (previousKey != null && previousKey.startsWith("uuid:")
                && sameUsername(previousUsername, canonical)) {
            return previousKey;
        }
        return "name:" + lower(canonical);
    }

    private static List<String> tokensOutsideBrackets(String text) {
        List<String> out = new ArrayList<String>();
        StringBuilder token = new StringBuilder();
        boolean inBracket = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inBracket) {
                if (c == ']') {
                    inBracket = false;
                }
                continue;
            }
            if (c == '[') {
                flush(token, out);
                inBracket = true;
            } else if (isUsernameChar(c)) {
                token.append(c);
            } else {
                flush(token, out);
            }
        }
        flush(token, out);
        return out;
    }

    private static void flush(StringBuilder token, List<String> out) {
        if (token.length() == 0) {
            return;
        }
        String candidate = token.toString();
        if (USERNAME.matcher(candidate).matches()) {
            out.add(candidate);
        }
        token.setLength(0);
    }

    private static boolean isUsernameChar(char c) {
        return c == '_' || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
    }

    private static String lower(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
