package com.arena.pekaclient.feature;

import java.util.Locale;

/**
 * The five dungeon classes and the colour each one is drawn in on the Spirit Leap selector.
 *
 * Pure: no Minecraft types, so it is unit-tested. {@link #parse} accepts only the exact class names, in any
 * letter case. Any other text, including an empty or missing value, returns null, and the caller shows the
 * class as unknown. Nothing is guessed from a partial match.
 */
public enum LeapClass {
    ARCHER("Archer", 0xFF57D68D),
    BERSERK("Berserk", 0xFFFF5A5A),
    MAGE("Mage", 0xFF5AAEFF),
    TANK("Tank", 0xFF43A96F),
    HEALER("Healer", 0xFFD5A2FF);

    /** Colour for a teammate whose class is missing or not one of the five. */
    public static final int UNKNOWN_COLOR = 0xFF8E8E93;

    private final String label;
    private final int color;
    private final char marker;

    LeapClass(String label, int color) {
        this.label = label;
        this.color = color;
        this.marker = label.charAt(0);
    }

    public String label() {
        return label;
    }

    /** The one-letter marker used by dungeon scoreboards. */
    public char marker() {
        return marker;
    }

    /** ARGB colour used for the class text and its accent. */
    public int color() {
        return color;
    }

    /**
     * The class for a one-letter scoreboard marker: A Archer, B Berserk, M Mage, T Tank, H Healer. Only the upper
     * case letter counts. Any other character returns null.
     */
    public static LeapClass fromMarker(char marker) {
        switch (marker) {
            case 'A':
                return ARCHER;
            case 'B':
                return BERSERK;
            case 'M':
                return MAGE;
            case 'T':
                return TANK;
            case 'H':
                return HEALER;
            default:
                return null;
        }
    }

    /** The class for an exact name (case-insensitive, surrounding spaces ignored), or null. */
    public static LeapClass parse(String text) {
        if (text == null) {
            return null;
        }
        String t = text.trim();
        if (t.isEmpty()) {
            return null;
        }
        for (LeapClass c : values()) {
            if (c.label.equalsIgnoreCase(t)) {
                return c;
            }
        }
        return null;
    }

    /** Colour for a class text: the class colour when it is one of the five, otherwise the unknown colour. */
    public static int colorFor(String text) {
        LeapClass c = parse(text);
        return c == null ? UNKNOWN_COLOR : c.color;
    }

    /**
     * Text shown under a teammate's name. A known class gives its label. A missing class gives "Unknown class",
     * because the class was not read, not because the teammate has none. Any other text is shown as it was read,
     * cut to 16 characters, in the unknown colour.
     */
    public static String displayText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "Unknown class";
        }
        LeapClass c = parse(text);
        if (c != null) {
            return c.label;
        }
        String t = text.trim();
        return t.length() > 16 ? t.substring(0, 16) : t;
    }

    /** Lower-cased label, used by tests and logs. */
    public String key() {
        return label.toLowerCase(Locale.ROOT);
    }
}
