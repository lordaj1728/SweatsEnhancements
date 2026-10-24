package com.arena.pekaclient.feature;

/**
 * Remembers when the player last used a Spirit Leap item. Used by {@link SpiritLeap} to recognise the menu
 * the server opens after that use. It only records a time, so it never changes the game.
 */
public final class LeapUse {

    private static long lastUseNs;

    private LeapUse() {
    }

    /** Records a use now. */
    public static void markUsed() {
        lastUseNs = System.nanoTime();
    }

    /** True when a use happened within {@code windowMs} milliseconds. */
    public static boolean recent(long windowMs) {
        return lastUseNs != 0L && System.nanoTime() - lastUseNs < windowMs * 1000000L;
    }

    /** True when a held item's display name (formatting removed) names a Spirit Leap or Infinileap item. */
    public static boolean isLeapItemName(String rawDisplayName) {
        String name = LeapText.strip(rawDisplayName).toLowerCase(java.util.Locale.ROOT);
        return name.contains("spirit leap") || name.contains("infinileap");
    }
}
