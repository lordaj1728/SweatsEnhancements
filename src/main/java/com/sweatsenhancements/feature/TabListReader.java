package com.sweatsenhancements.feature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.StringUtils;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Reads the server's tab list every second and pulls structured values out of it.
 *
 * Written after comparing the two reference implementations:
 *
 * Written from scratch. The reference mods were read to understand what these features have to
 * cope with, but no code was taken from any of them - not even where their licences would allow
 * it. The two things that shaped the design are facts about the platform, not about any mod:
 *
 *   1. Tab lines only come out in a usable order if they are sorted the way the vanilla client
 *      sorts them (spectator flag, team name, profile name). That order is Minecraft's, and it
 *      is visible in the client's own tab renderer.
 *   2. Anything keyed to another server's names fails silently. Naming a specific mod here would
 *      be beside the point: hardcoding the other server's area and mayor names is exactly why its mods
 *      do nothing on Fakepixel, so this class keeps the server's raw strings and interprets
 *      nothing.
 *
 * So: reading is kept separate from rendering (nothing renders here), no area or mayor is
 * hardcoded, every path is wrapped so one malformed line cannot wipe out good data, and the
 * whole parse is inspectable in game - because "it silently did nothing" has to be answerable
 * by running a command, not by reading code.
 */
public class TabListReader {

    /** One tab list line, in the order the client draws it. */
    public static class Line {
        public final String raw;        // with colour codes
        public final String clean;      // codes stripped
        public final String section;    // which block it fell under

        Line(String raw, String clean, String section) {
            this.raw = raw;
            this.clean = clean;
            this.section = section;
        }
    }

    /**
     * Sort order the vanilla tab list uses, taken from how the client's own tab renderer
     * orders entries. This is a property of Minecraft, not of any mod - if the order is
     * wrong every line below comes out scrambled, so it has to match vanilla exactly.
     */
    public static final Ordering<NetworkPlayerInfo> ORDERING = Ordering.from(new Comparator<NetworkPlayerInfo>() {
        @Override
        public int compare(NetworkPlayerInfo a, NetworkPlayerInfo b) {
            ScorePlayerTeam teamA = a.getPlayerTeam();
            ScorePlayerTeam teamB = b.getPlayerTeam();
            return ComparisonChain.start()
                    .compareTrueFirst(a.getGameType() != WorldSettings.GameType.SPECTATOR,
                            b.getGameType() != WorldSettings.GameType.SPECTATOR)
                    .compare(teamA != null ? teamA.getRegisteredName() : "",
                            teamB != null ? teamB.getRegisteredName() : "")
                    .compare(a.getGameProfile().getName(), b.getGameProfile().getName())
                    .result();
        }
    });

    /** The client's own tab list order, for anything else that has to walk the entries. */
    public static Ordering<NetworkPlayerInfo> ordering() {
        return ORDERING;
    }

    /**
     * The names of the players the client is holding, lower case.
     *
     * The server fills its tab list with fake entries - the stat lines, the headings, the padding
     * rows - and they are mixed in with the real players in sorted order. Telling the two apart by
     * the name the client already has is the only reliable way: an entry that is a real player is
     * a player, whatever else the server draws it as.
     */
    private volatile java.util.Set<String> playerNames = new java.util.HashSet<String>();

    private volatile List<Line> lines = Collections.emptyList();
    private volatile Map<String, List<String>> values = new LinkedHashMap<String, List<String>>();
    private volatile List<String> sections = new ArrayList<String>();
    private volatile String area = "";
    private volatile String server = "";
    private volatile String mayor = "";
    private volatile long lastReadMs = 0L;
    /** bumped only when the list's content actually changes, so readers can skip their own work */
    private volatile int version = 0;
    private volatile int contentHash = 0;
    private volatile int readCount = 0;
    private volatile int failCount = 0;
    private volatile String lastError = "";

    private int tickCounter = 0;
    private int intervalTicks = 20;

    public void setIntervalTicks(int ticks) {
        this.intervalTicks = Math.max(5, ticks);
    }

    public void onTick(TickEvent.ClientTickEvent event) {
        try {
            if (event.phase != TickEvent.Phase.END) return;
            tickCounter = (tickCounter + 1) % intervalTicks;
            if (tickCounter != 0) return;
            read();
        } catch (Throwable t) {
            failCount++;
            lastError = String.valueOf(t);
        }
    }

    private void read() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || mc.thePlayer.sendQueue == null) return;

        GuiPlayerTabOverlay tab = mc.ingameGUI == null ? null : mc.ingameGUI.getTabList();
        List<NetworkPlayerInfo> infos = ORDERING.sortedCopy(mc.thePlayer.sendQueue.getPlayerInfoMap());
        List<Line> newLines = new ArrayList<Line>();
        String section = "top";

        for (NetworkPlayerInfo info : infos) {
            String raw = null;
            try {
                raw = tab == null ? null : tab.getPlayerName(info);
            } catch (Throwable ignored) {
            }
            if (raw == null) {
                try {
                    raw = info.getDisplayName() == null ? "" : info.getDisplayName().getFormattedText();
                } catch (Throwable t) {
                    continue;
                }
            }
            if (raw.isEmpty()) continue;

            String clean = StringUtils.stripControlCodes(raw).trim();

            // section headers, as the server writes them. Matched loosely on the words, not on
            // exact colour codes, so a colour change on the server does not break detection.
            String lower = clean.toLowerCase();
            if (lower.endsWith("server info") || lower.endsWith("account info")
                    || lower.endsWith("player stats") || lower.endsWith("event")
                    || lower.endsWith("elections")) {
                section = lower;
            }
            newLines.add(new Line(raw, clean, section));
        }

        if (newLines.isEmpty()) return;   // not connected, or between worlds - keep the last good data

        Map<String, List<String>> newValues = new LinkedHashMap<String, List<String>>();
        List<String> seenSections = new ArrayList<String>();
        for (Line l : newLines) {
            if (!seenSections.contains(l.section)) seenSections.add(l.section);
            // "Key: value" lines are the bulk of what a SkyBlock tab list contains
            int colon = l.clean.indexOf(':');
            if (colon > 0 && colon < l.clean.length() - 1) {
                String key = l.clean.substring(0, colon).trim().toLowerCase();
                String value = l.clean.substring(colon + 1).trim();
                List<String> bucket = newValues.get(key);
                if (bucket == null) {
                    bucket = new ArrayList<String>();
                    newValues.put(key, bucket);
                }
                bucket.add(value);
            }
        }

        java.util.Set<String> names = new java.util.HashSet<String>();
        for (NetworkPlayerInfo info : infos) {
            try {
                String name = info.getGameProfile() == null ? null : info.getGameProfile().getName();
                if (name != null && !name.isEmpty()) names.add(name.toLowerCase(java.util.Locale.ROOT));
            } catch (Throwable t) {
                // an entry without a profile is not a player we can name
            }
        }
        this.playerNames = names;

        this.lines = newLines;
        this.values = newValues;
        this.sections = seenSections;
        this.area = first(newValues, "area");
        this.server = first(newValues, "server");
        this.mayor = first(newValues, "mayor");
        if (this.mayor.isEmpty()) this.mayor = first(newValues, "current mayor");
        if (this.mayor.isEmpty()) this.mayor = first(newValues, "winner");
        this.lastReadMs = System.currentTimeMillis();
        this.readCount++;
        // the server re-sends the same list most of the time; only a real change counts as a change
        int hash = 11;
        for (Line line : this.lines) {
            hash = hash * 31 + (line == null ? 0 : line.clean.hashCode());
        }
        hash = hash * 31 + this.playerNames.size();
        if (hash != this.contentHash) {
            this.contentHash = hash;
            this.version++;
        }
    }

    private static String first(Map<String, List<String>> map, String key) {
        List<String> list = map.get(key);
        return (list == null || list.isEmpty()) ? "" : list.get(0);
    }

    // ------------------------------------------------------------------ read access

    public List<Line> lines() {
        return lines;
    }

    /** Changes only when the list's content changes, so the scoreboard can skip a pointless rebuild. */
    public int version() {
        return version;
    }

    /** The names of the real players in the tab list, lower case. */
    public java.util.Set<String> playerNames() {
        return playerNames;
    }

    public List<String> sections() {
        return sections;
    }

    /** Every value seen for a key, in tab order. Key is lower-case, without the colon. */
    public List<String> all(String key) {
        List<String> list = values.get(key.toLowerCase());
        return list == null ? Collections.<String>emptyList() : list;
    }

    public String get(String key) {
        return first(values, key.toLowerCase());
    }

    public String area() {
        return area;
    }

    public String server() {
        return server;
    }

    public String mayor() {
        return mayor;
    }

    /** Lines whose text contains the given string, case-insensitive. */
    public List<String> containing(String needle) {
        List<String> out = new ArrayList<String>();
        String n = needle.toLowerCase();
        for (Line l : lines) {
            if (l.clean.toLowerCase().contains(n)) out.add(l.clean);
        }
        return out;
    }

    // ------------------------------------------------------------------ diagnostics

    public boolean hasData() {
        return !lines.isEmpty();
    }

    public String statusLine() {
        if (lines.isEmpty()) {
            return "no tab list data yet (last error: " + (lastError.isEmpty() ? "none" : lastError) + ")";
        }
        long ageSec = (System.currentTimeMillis() - lastReadMs) / 1000L;
        return lines.size() + " lines, " + values.size() + " keys, " + sections.size() + " sections, read "
                + readCount + "x, " + failCount + " failures, updated " + ageSec + "s ago";
    }

    public String summaryLine() {
        if (lines.isEmpty()) return "no data";
        StringBuilder sb = new StringBuilder();
        sb.append("area=").append(area.isEmpty() ? "?" : area);
        if (!server.isEmpty()) sb.append("  server=").append(server);
        if (!mayor.isEmpty()) sb.append("  mayor=").append(mayor);
        return sb.toString();
    }

    public int readCount() {
        return readCount;
    }

    public int failCount() {
        return failCount;
    }
}
