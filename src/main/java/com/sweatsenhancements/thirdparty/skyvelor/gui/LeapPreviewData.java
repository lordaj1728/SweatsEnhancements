/* Adapted from Sky VeLor c1b56668155e31fca7a8d3c82058496d0e77d97e; MIT licence.
 * Package integration for SweatsEnhancements. Upstream originals and licence are distributed with source. */
package com.sweatsenhancements.thirdparty.skyvelor.gui;

import com.sweatsenhancements.thirdparty.skyvelor.feature.LeapClass;

/**
 * Local-only display samples for the Spirit Leap GUI preview. Live teammate resolution never reads this class.
 * Four cards are shown at once; cycling the starting index covers all five dungeon classes without changing the
 * reference 2-by-2 layout or touching a live cache/container.
 */
public final class LeapPreviewData {

    public static final int CARD_COUNT = 4;
    public static final int SAMPLE_COUNT = 5;

    private static final String[] USERNAMES = {
            "AmberArcher", "BerserkBee", "MossMage", "TurtleTank", "SuiHealer"
    };
    private static final String[] DISPLAY_NAMES = {
            "\u00a76[MVP+] \u00a7aAmberArcher",
            "\u00a7a[VIP] \u00a7cBerserkBee",
            "\u00a7d[MVP++] \u00a79MossMage",
            "\u00a73[VIP+] \u00a72TurtleTank",
            "\u00a75[MVP++] \u00a7dSuiHealer"
    };
    private static final LeapClass[] CLASSES = {
            LeapClass.ARCHER, LeapClass.BERSERK, LeapClass.MAGE, LeapClass.TANK, LeapClass.HEALER
    };

    private LeapPreviewData() {
    }

    /** Returns the sample for a visible card at a given cycle offset. */
    public static int sampleIndex(int offset, int cardIndex) {
        return Math.floorMod(offset + cardIndex, SAMPLE_COUNT);
    }

    public static String username(int sampleIndex) {
        return USERNAMES[Math.floorMod(sampleIndex, SAMPLE_COUNT)];
    }

    public static String displayName(int sampleIndex) {
        return DISPLAY_NAMES[Math.floorMod(sampleIndex, SAMPLE_COUNT)];
    }

    public static LeapClass playerClass(int sampleIndex) {
        return CLASSES[Math.floorMod(sampleIndex, SAMPLE_COUNT)];
    }
}
