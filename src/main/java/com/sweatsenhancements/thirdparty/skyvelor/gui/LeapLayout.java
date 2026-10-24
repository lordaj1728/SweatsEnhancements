/* Adapted from Sky VeLor c1b56668155e31fca7a8d3c82058496d0e77d97e; MIT licence.
 * Package integration for SweatsEnhancements. Upstream originals and licence are distributed with source. */
package com.sweatsenhancements.thirdparty.skyvelor.gui;

/**
 * Responsive geometry for the Spirit Leap card overlay.
 *
 * The polished grid keeps two equal columns but uses a compact gap (about 3.6% of the viewport width) and a
 * reduced head (about one sixth of the card width) to reserve space for larger readable names. All values are
 * derived from Minecraft's scaled GUI dimensions, not screenshot-sized coordinates.
 */
public final class LeapLayout {

    public static final int COLS = 2;

    private static final int MIN_SIDE_MARGIN = 6;
    private static final int MIN_COLUMN_GAP = 6;
    private static final int MIN_ROW_GAP = 6;
    private static final int MIN_VERTICAL_MARGIN = 6;
    private static final int HEAD_BOTTOM_TOP_SPACE = 12;

    private LeapLayout() {
    }

    /** Horizontal outside margin: approximately 7.1% of the scaled viewport. */
    public static int horizontalMargin(int screenWidth) {
        return Math.max(MIN_SIDE_MARGIN, Math.max(0, screenWidth) / 14);
    }

    /** Compact space between columns: approximately 3.6% of the scaled viewport. */
    public static int horizontalGap(int screenWidth) {
        return Math.max(MIN_COLUMN_GAP, Math.max(0, screenWidth) / 28);
    }

    /**
     * Card width for the current scaled viewport. The height-based cap avoids excessively wide cards on unusually
     * wide, short windows while keeping the compact group inside the screen.
     */
    public static int cardWidth(int screenWidth, int screenHeight) {
        int width = Math.max(0, screenWidth);
        int height = Math.max(0, screenHeight);
        int availableWidth = width - 2 * horizontalMargin(width) - horizontalGap(width);
        int widthLimited = Math.max(1, availableWidth / COLS);
        int heightLimited = Math.max(1, height * 64 / 100);
        return Math.min(widthLimited, heightLimited);
    }

    /** Vertical outside margin used when limiting a multi-row layout. */
    public static int verticalMargin(int screenHeight) {
        return Math.max(MIN_VERTICAL_MARGIN, Math.max(0, screenHeight) / 16);
    }

    /** Compact space between teammate rows: approximately 6.25% of the scaled viewport height. */
    public static int verticalGap(int screenHeight, int rows) {
        if (rows <= 1) {
            return 0;
        }
        return Math.max(MIN_ROW_GAP, Math.max(0, screenHeight) / 16);
    }

    /**
     * Responsive card height. It follows the card width for a landscape rectangle, then shrinks as needed to fit
     * every row, the preview label, and safe margins on short screens.
     */
    public static int cardHeight(int screenWidth, int screenHeight, int rows, int topInset) {
        if (rows <= 0) {
            return 0;
        }
        int height = Math.max(0, screenHeight);
        int widthBased = Math.max(1, cardWidth(screenWidth, height) * 38 / 100);
        int gaps = Math.max(0, rows - 1) * verticalGap(height, rows);
        int reserved = 2 * verticalMargin(height) + Math.max(0, topInset) + gaps;
        int available = Math.max(1, height - reserved);
        int heightBased = Math.max(1, available / rows);
        return Math.min(widthBased, heightBased);
    }

    /** Head size scales with both card dimensions while remaining square and vertically inset. */
    public static int headSize(int cardWidth, int cardHeight) {
        if (cardWidth <= 0 || cardHeight <= 0) {
            return 0;
        }
        return Math.max(1, Math.min(cardWidth / 6, cardHeight - HEAD_BOTTOM_TOP_SPACE));
    }

    public static int headLeft(int cardWidth) {
        return Math.max(4, Math.max(0, cardWidth) / 32);
    }

    public static int headGap(int cardWidth) {
        return Math.max(3, Math.max(0, cardWidth) / 56);
    }

    public static int textRight(int cardWidth) {
        return Math.max(4, Math.max(0, cardWidth) / 48);
    }

    /** Width left for the name and class after the head and its padding. */
    public static int textWidth(int cardWidth, int headSize) {
        return Math.max(0, cardWidth - headLeft(cardWidth) - Math.max(0, headSize)
                - headGap(cardWidth) - textRight(cardWidth));
    }

    /** Hit test matching the rounded card silhouette, so the invisible corner pixels are not selectable. */
    public static boolean containsRoundedRect(int mouseX, int mouseY, int x, int y, int width, int height,
                                              float radius) {
        if (width <= 0 || height <= 0 || mouseX < x || mouseY < y
                || mouseX >= x + width || mouseY >= y + height) {
            return false;
        }
        float r = Math.max(0f, Math.min(radius, Math.min(width, height) * 0.5f));
        if (r == 0f) {
            return true;
        }
        float px = mouseX + 0.5f;
        float py = mouseY + 0.5f;
        float nearestX = Math.max(x + r, Math.min(px, x + width - r));
        float nearestY = Math.max(y + r, Math.min(py, y + height - r));
        float dx = px - nearestX;
        float dy = py - nearestY;
        return dx * dx + dy * dy <= r * r;
    }

    /** Horizontal scale that applies the requested font enlargement without clipping the complete string. */
    public static float fittedScale(int measuredTextWidth, int availableWidth, float requestedScale) {
        if (measuredTextWidth <= 0 || availableWidth <= 0 || requestedScale <= 0f) {
            return 0f;
        }
        return Math.min(requestedScale, availableWidth / (float) measuredTextWidth);
    }

    public static int gridWidth(int cardWidth, int columns, int gap) {
        return columns <= 0 ? 0 : columns * cardWidth + (columns - 1) * Math.max(0, gap);
    }

    public static int gridHeight(int cardHeight, int rows, int gap) {
        return rows <= 0 ? 0 : rows * cardHeight + (rows - 1) * Math.max(0, gap);
    }

    /** Number of rows needed for {@code cards} cards in the two-column grid. */
    public static int rows(int cards) {
        return cards <= 0 ? 0 : (cards + COLS - 1) / COLS;
    }
}
