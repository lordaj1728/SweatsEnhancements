package com.sweatsenhancements.feature;

import java.util.ArrayList;
import java.util.List;

import com.sweatsenhancements.SweatsEnhancements;

/**
 * Where each HUD element sits, and what it is called.
 *
 * Elements that have never been dragged stack under the watermark in the order of this list, so
 * the default layout stays tidy whether or not everything is switched on. Once an element is
 * dragged in the HUD editor its position is written to the config and it stays where it was put.
 *
 * Dragging is the reference arrangement: elements are moved on screen rather than typed in as
 * coordinates.
 */
public final class HudLayout {

    /** one line of text and its colour */
    public static final class Line {
        public final String text;
        public final int colour;

        public Line(String text, int colour) {
            this.text = text;
            this.colour = colour;
        }
    }

    /** an element that can be drawn and moved */
    public static final class Element {
        /** config key stem, and the feature id it belongs to */
        public final String key;
        public final String label;
        /** the feature that switches it on, or null when it has no switch */
        public final String feature;
        /**
         * True for an element that is not part of this HUD at all, but another panel's own
         * drawing - the scoreboard. It is moved the same way on screen, but where it is stored is
         * the panel's own position settings rather than a HUD coordinate, so the editor hands the
         * move to that panel instead of writing it here.
         */
        public final boolean panel;

        Element(String key, String label, String feature) {
            this(key, label, feature, false);
        }

        Element(String key, String label, String feature, boolean panel) {
            this.key = key;
            this.label = label;
            this.feature = feature;
            this.panel = panel;
        }
    }

    private HudLayout() {
    }

    public static final List<Element> ELEMENTS = new ArrayList<Element>();

    static {
        // the scoreboard first: it is the biggest thing on the screen, and pressing H is how the
        // player moves it - "make scoreboard change positions using H"
        ELEMENTS.add(new Element("scoreboard", "Scoreboard", "MS085", true));
        ELEMENTS.add(new Element("watermark", "SweatsEnhancements watermark", null));
        ELEMENTS.add(new Element("MS028", "TPS Display", "MS028"));
        ELEMENTS.add(new Element("MS201", "Current Pet", "MS201"));
        ELEMENTS.add(new Element("MS226", "Clicks Per Second", "MS226"));
        ELEMENTS.add(new Element("DU001", "Dungeon Run", "DU001"));
        ELEMENTS.add(new Element("DU005", "Dungeon Blessings", "DU005"));
        ELEMENTS.add(new Element("DU010", "Blood Timer", "DU010"));
        ELEMENTS.add(new Element("DU014", "Terracotta Timer", "DU014"));
        ELEMENTS.add(new Element("DU016", "Dungeon Map", "DU016"));
        ELEMENTS.add(new Element("DU020", "Dungeon Score", "DU020"));
    }

    /** Independent persisted HUD scale; the scoreboard keeps its existing scale setting. */
    public static int scalePercent(Element e) {
        if (e.panel) {
            int n = SweatsEnhancements.cfg == null ? 100 : SweatsEnhancements.cfg.getInt(CustomScoreboard.SCALE_KEY, 100);
            return n <= 1 ? 100 : Math.max(25, Math.min(200, n));
        }
        int n = SweatsEnhancements.cfg == null ? 100 : SweatsEnhancements.cfg.getInt("hud." + e.key + ".scale", 100);
        return Math.max(50, Math.min(200, n));
    }

    public static int resized(int percent, int direction, boolean panel) {
        return Math.max(panel ? 25 : 50, Math.min(200, percent + (direction > 0 ? 10 : -10)));
    }

    public static void resize(Element e, int direction) {
        if (SweatsEnhancements.cfg == null || direction == 0) return;
        String key = e.panel ? CustomScoreboard.SCALE_KEY : "hud." + e.key + ".scale";
        SweatsEnhancements.cfg.setInt(key, resized(scalePercent(e), direction, e.panel));
    }

    public static Element byKey(String key) {
        for (Element e : ELEMENTS) {
            if (e.key.equals(key)) return e;
        }
        return null;
    }

    private static String xKey(Element e) {
        return "hud." + e.key + ".x";
    }

    private static String yKey(Element e) {
        return "hud." + e.key + ".y";
    }

    /** true once the element has been dragged somewhere */
    public static boolean isPinned(Element e) {
        try {
            return SweatsEnhancements.cfg != null && SweatsEnhancements.cfg.contains(xKey(e));
        } catch (Throwable t) {
            return false;
        }
    }

    public static int x(Element e) {
        try {
            return SweatsEnhancements.cfg.getInt(xKey(e), 2);
        } catch (Throwable t) {
            return 2;
        }
    }

    public static int y(Element e) {
        try {
            return SweatsEnhancements.cfg.getInt(yKey(e), 2);
        } catch (Throwable t) {
            return 2;
        }
    }

    public static void set(Element e, int x, int y) {
        try {
            SweatsEnhancements.cfg.setInt(xKey(e), x);
            SweatsEnhancements.cfg.setInt(yKey(e), y);
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] could not move " + e.key + ": " + t);
        }
    }

    /** Puts the element back in the automatic stack. */
    public static void unpin(Element e) {
        try {
            SweatsEnhancements.cfg.remove(xKey(e));
            SweatsEnhancements.cfg.remove(yKey(e));
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] could not reset " + e.key + ": " + t);
        }
    }

    public static void resetAll() {
        for (Element e : ELEMENTS) unpin(e);
    }

    /** how many elements are currently moved rather than stacked */
    public static int pinnedCount() {
        int n = 0;
        for (Element e : ELEMENTS) {
            if (isPinned(e)) n++;
        }
        return n;
    }
}
