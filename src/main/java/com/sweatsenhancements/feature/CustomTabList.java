package com.sweatsenhancements.feature;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.chat.Report;
import com.sweatsenhancements.config.Cfg;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import org.lwjgl.opengl.GL11;

/**
 * Compact Tab List - the client's tab list, rebuilt as columns of compacted lines.
 *
 * This follows the reference compact tab list closely in what it puts on the screen,
 * because that is the thing being replaced. Their geometry, all of it: 22 lines per column, line
 * height 9, 3 pixels of padding inside a column, 6 between columns, and a dark panel drawn behind
 * each column (0x8F262626). Their reader: split the list on blank lines into sections, keep each
 * section's own title line with it, and re-pack the sections into columns - re-adding the title when
 * a section runs on into the next column. Their title line is drawn centred over its column.
 *
 * The compaction is the part that has to be re-derived rather than copied: their rewrite chain is
 * written against the other server's wording (God Potion, "No Buffs active", "None Found"). The equivalent
 * lines on this server, and what is done with them:
 *
 *   - the server's padding rows (`CLICK`, one per empty slot it pads a section with) are dropped,
 *     the same way blank lines are: they carry nothing, and on this server they are most of the list
 *   - "Active Effects" + "You have 28 active effects. Use /effects to see them!" becomes one line,
 *     "Active Effects: 28" - their rewrite, applied to this server's wording
 *   - "Not active! Obtain booster cookies from the community shop in the hub." becomes "Not Active"
 *   - the advert rows are matched by the user's own Hide Lines list, which now carries this server's
 *     shop address by default
 *
 * How vanilla is stopped: 1.8.9 Forge fires RenderGameOverlayEvent.Pre(PLAYER_LIST) from the middle
 * of GuiIngameForge.renderPlayerList, and cancelling it returns before anything is drawn. The
 * client's own caching (updatePlayerList) has already run by then, so getPlayerName() still returns
 * fresh names. Nothing is hidden from other mods - only from the screen. The list is only cancelled
 * on a frame where there are lines to draw in its place.
 *
 * The header and footer are the two IChatComponent fields on the tab list overlay, read by name
 * (field_175256_i / field_175255_h at runtime, header / footer in a development environment).
 *
 * /se panels prints what each stage of this class has seen.
 */
public class CustomTabList extends Gui {

    public static final String SCALE_KEY = "tab.scale";
    public static final String HIDE_KEY = "tab.hideLines";
    public static final String HEADER_KEY = "tab.showHeader";
    public static final String FOOTER_KEY = "tab.showFooter";
    public static final String TOGGLE_KEY = "tab.toggle";
    /** hide the rows and the banner lines that advertise the server or its store */
    public static final String ADVERTS_KEY = "tab.hideAdverts";
    /** hide fire sale announcements */
    public static final String FIRE_SALE_KEY = "tab.hideFireSale";
    /** draw no panel behind the columns, so the list sits straight on the world */
    public static final String BACKGROUND_KEY = "tab.hideBackground";
    /** how many lines a column holds before the next one starts */
    public static final String COLUMN_LINES_KEY = "tab.maxLines";

    /** The reference layout's own numbers, so the two lists have the same shape */
    private static final int MAX_LINES = 22;
    private static final int LINE_HEIGHT = 9;
    private static final int TAB_PADDING = 3;
    private static final int COLUMN_SPACING = 6;
    /** the panel behind a column, their colour and alpha */
    /** the column box with the big background off, and the one laid over it when on */
    private static final int PANEL_COLOUR = 0x8F262626;
    private static final int COLUMN_OVERLAY_COLOUR = 0x20AAAAAA;
    /** the panel behind the whole list: black at half opacity */
    private static final int WHOLE_PANEL_COLOUR = 0x80000000;

    /** the client's own limit: four columns, and it never shows more than eighty entries */
    private static final int MAX_COLUMNS = 4;
    private static final int MAX_ENTRIES = MAX_LINES * MAX_COLUMNS;

    /** "You have 28 active effects. Use /effects to see them!" - the count is what is worth keeping */
    private static final Pattern ACTIVE_EFFECTS =
            Pattern.compile("(?i)you have (?<count>\\d+) (?:active|non-god) effects?.*");
    /** "Active Effects" - the heading the count goes on. */
    private static final Pattern EFFECTS_HEADING = Pattern.compile("(?i)active effects");
    /**
     * Use \"/effects\" to see them!
     *
     * The reference matches `Use \"/effects\".*`, which is what the sentence looks like whole. This
     * server wraps it after `Use`, putting the rest on its own row - so the row with the opening
     * words is dropped as well, or the tail of the sentence sits under the heading on its own.
     */
    private static final Pattern EFFECTS_USE_COMMAND =
            Pattern.compile("(?i)(?:^|use )\"/effects\".*");
    /** "No effects active. Drink Potions or splash them on the ground to buff yourself!" */
    private static final Pattern NO_EFFECTS_ACTIVE =
            Pattern.compile("(?i)no effects active\\. drink potions or splash them on the|ground to buff yourself!");
    private static final Pattern COOKIE_HEADING = Pattern.compile("(?i)cookie buff");
    /** "You have a God Potion active! 12 Hours" - the timer is shown on the effects heading. */
    private static final Pattern GOD_POTION =
            Pattern.compile("(?i)you have a god potion active! (?<timer>[\\w ]+)");
    /** the title the reference gives the footer's own column */
    private static final String OTHER_TITLE = "\u00a72\u00a7lOther";
    /** a pet at its top level */
    private static final Pattern PET_MAX_LEVEL = Pattern.compile("(?i).*\\[lvl 100][^0-9].*");
    /** how many entries the server sends per column - a property of Minecraft, not of the server */
    private static final int ENTRIES_PER_COLUMN = 20;
    /** "Not active! Obtain booster cookies from the community shop in the hub." - and this server
     * splits it into two rows, so only the opening words can be relied on. */
    private static final Pattern NO_COOKIE =
            Pattern.compile("(?i)not active! obtain booster cookies|shop in the hub\\.");
    /** a value the server has no number for: "Pet Sitter: N/A" */
    private static final Pattern NO_VALUE = Pattern.compile(".*:\\s*N/?A\\s*$", Pattern.CASE_INSENSITIVE);
    /** the line a server sentence is chopped at when it is too wide for one tab row */
    private static final Pattern FRAGMENT = Pattern.compile("^[a-z\"'/\\\\]");
    /** the row the server pads empty slots with */
    private static final String PAD_ROW = "CLICK";
    /** the padding row, and the other wording this server uses for it */
    private static final Pattern PAD_LINE = Pattern.compile("(?i)^click( to play)?!?$");
    /** a row that is one word and nothing else - the shape of a player name */
    private static final Pattern BARE_NAME = Pattern.compile("^[A-Za-z0-9_]{3,16}$");
    /**
     * A heading - the line a group of lines belongs under.
     *
     * This server sends "Server Info", "Account Info", "Players (3)", "Skills:", "Stats:", "Pet:",
     * "Election:", "Event:" and "Active Effects". Blank rows are NOT headings: the server pads its
     * list to a hundred entries and drops a blank row between most lines, so treating a blank as a
     * break made every line a section of its own.
     */
    private static final Pattern HEADING = Pattern.compile(
            "(?i)^(server info|account info|active effects|players \\(\\d+\\)|.{2,24}:)$"
            + "|(?i)^profile:.*");

    /**
     * What an advertisement looks like: the server's store, its website, its discord, and the
     * line the list itself puts at the bottom. Matched without colour codes and without case, so
     * it does not matter how the server dresses them up. This is what the Hide Server Adverts
     * switch uses - the Hide Lines setting is the player's own list and is kept separate, so
     * neither switch can quietly undo the other.
     */
    private static final String[] ADVERT_WORDS = {
            "ranks, boosters", "discord.gg", "discord.com", "buy ranks", "store link",
            "store.", "www.", "http://", "https://", ".net", ".com", ".me", ".shop", ".gg"};

    /** what a fire sale announcement looks like */
    private static final String[] FIRE_SALE_WORDS = {"fire sale", "fire_sale", "firesale"};

    // ---------------------------------------------------------------- diagnostics, read by /se panels
    /** every Pre(PLAYER_LIST) this class was handed */
    public static int framesPreList;
    /** every Post(PLAYER_LIST) this class was handed */
    public static int framesPostList;
    /** every Post(ALL) this class was handed */
    public static int framesPostAll;
    /** every Text event this class was handed, whether or not it drew there */
    public static int framesText;
    /** frames where TAB was actually being held */
    public static int tabFrames;
    /** frames where the client's own list was actually cancelled */
    public static int cancelFrames;
    /** frames where this list was actually drawn */
    public static int drawFrames;
    /** entries read on the last read, and how many of them were dropped as padding or adverts */
    public static int readEntries;
    public static int droppedEntries;
    /** columns and sections the last rebuild produced */
    public static int columnsBuilt;
    public static int sectionsBuilt;
    /** why nothing is on the screen: one short sentence, or "" while the list is drawing */
    public static volatile String why = "not asked yet";

    /** one drawn column: a centred title and the lines under it */
    private static final class Column {
        String title = "";
        final List<String> lines = new ArrayList<String>();
    }

    /** what the last Pre(PLAYER_LIST) worked out, so the draw does not read the client twice */
    private List<Column> prepared;
    private List<String> preparedHeader;
    private int preparedCentre;
    private int preparedTop;

    public static boolean on() {
        try {
            return SweatsEnhancements.cfg != null && SweatsEnhancements.cfg.getBool(Cfg.key("MS063"), false);
        } catch (Throwable t) {
            return false;
        }
    }

    /** true while the list should be on screen: TAB held, or toggled on with press-to-toggle */
    private static volatile boolean toggled;

    private static boolean toggleMode() {
        try {
            return SweatsEnhancements.cfg != null && SweatsEnhancements.cfg.getBool(TOGGLE_KEY, false);
        } catch (Throwable t) {
            return false;
        }
    }

    /** The list is up while the player holds the key, or after a press when press-to-toggle is on. */
    private static boolean showing(Minecraft mc) {
        if (toggled) return true;
        return mc != null && mc.gameSettings != null && mc.gameSettings.keyBindPlayerList != null
                && mc.gameSettings.keyBindPlayerList.isKeyDown();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!on()) {
            prepared = null;
            toggled = false;
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;

        if (toggleMode() && mc.gameSettings != null && mc.gameSettings.keyBindPlayerList != null
                && mc.gameSettings.keyBindPlayerList.isPressed()) {
            toggled = !toggled;
            prepared = null;                       // rebuilt on the next frame either way
            Report.noteOnce(toggled ? "tab.toggle.on" : "tab.toggle.off",
                    toggled ? "Tab list toggled on - press TAB again to close it."
                            : "Tab list toggled off.");
        }

        if (!showing(mc)) {
            // TAB is up: the list goes with it. Without this the list was prepared once and then
            // re-drawn every frame by Post(ALL) for ever, which is the list that would not close.
            prepared = null;
            return;
        }
        tabFrames++;
        if (prepared == null) {
            // Press-to-toggle never fires Pre(PLAYER_LIST) - nothing is held down - so the list is
            // built here as well. In hold mode this is a no-op, the Pre handler has already done it.
            try {
                prepare();
            } catch (Throwable t) {
                Report.fail("tablist.prepare", "Building the tab list", t);
            }
        }
        if (prepared == null || prepared.isEmpty()) {
            if (tabFrames > 40) {
                Report.blockedOnce("tab.empty",
                        "Compact Tab List is on, but there is nothing left to draw when TAB is held"
                        + " (everything was filtered out). The client's own list is left in place.");
            }
            return;
        }
        Report.noteOnce("tab.first",
                "Compact Tab List is drawing " + String.valueOf(containedLines()) + " lines in "
                + String.valueOf(prepared.size()) + " columns, from " + String.valueOf(readEntries)
                + " rows read, " + String.valueOf(droppedEntries) + " dropped as padding or adverts.");
    }

    /** How many lines the prepared columns actually hold, titles included. */
    private int containedLines() {
        int n = 0;
        if (prepared == null) return 0;
        for (Column column : prepared) n += column.lines.size();
        return n;
    }

    @SubscribeEvent
    public void onOverlayPre(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.PLAYER_LIST) return;
        framesPreList++;
        if (!on()) return;
        try {
            prepare();
            if (prepared == null || prepared.isEmpty()) {
                why = "nothing to draw - the client's list is left in place";
                return;
            }
            event.setCanceled(true);
            cancelFrames++;
            why = "";
        } catch (Throwable t) {
            Report.fail("tablist.cancel", "Hiding the client's tab list", t);
            why = "hiding the client's list failed: " + t;
        }
    }

    /**
     * Draws when Post(ALL) never arrives.
     *
     * Post(ALL) is the last event of the frame and is the right place for the list: it comes after
     * the chat and after the client's own player list. If a client never sends it - the counters in
     * /se panels show whether that is what is happening - this draws instead. It only steps in when
     * Post(ALL) has not been seen at all, so one frame is never drawn twice.
     */
    @SubscribeEvent
    public void onOverlayText(RenderGameOverlayEvent.Text event) {
        framesText++;
        if (framesPostAll != 0) return;
        if (framesText < 40) return;
        drawGuarded();
    }

    @SubscribeEvent
    public void onOverlayPost(RenderGameOverlayEvent.Post event) {
        if (event.type == RenderGameOverlayEvent.ElementType.PLAYER_LIST) {
            framesPostList++;
            return;
        }
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        framesPostAll++;
        drawGuarded();
    }

    private void drawGuarded() {
        if (!on()) return;
        try {
            draw();
        } catch (Throwable t) {
            Report.fail("tablist.draw", "Drawing the tab list", t);
            why = "drawing failed: " + t;
        }
    }

    /**
     * Works the whole list out and measures it, without drawing.
     *
     * Called from the Pre(PLAYER_LIST) handler because that is where the decision to hide the
     * client's list has to be made: nothing is cancelled until there is something to put in its
     * place.
     */
    private void prepare() {
        prepared = null;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || mc.getNetHandler() == null) {
            why = "not connected";
            return;
        }
        if (!showing(mc)) {
            why = "TAB is not held";
            return;
        }
        if (mc.gameSettings != null && mc.gameSettings.showDebugInfo) {
            why = "the F3 screen is up";
            return;
        }
        FontRenderer fr = mc.fontRendererObj;
        if (fr == null) {
            why = "no font renderer";
            return;
        }

        List<RawColumn> serverColumns = readColumns(mc);
        // the footer is its own column in the reference's own layout: this server puts the active
        // effects and the cookie buff there, not in the columns above
        RawColumn other = parseFooter(footerLines(mc));
        // the Footer switch governs the footer's other lines: the active effects and the cookie buff
        // are kept either way, because on this server they are the reason the footer exists
        if (other != null && !flag(FOOTER_KEY, true)) other = essentialOnly(other);
        if (other != null) serverColumns.add(other);
        List<Column> columns = combine(serverColumns, columnLines());

        preparedHeader = split(banner(mc, true));
        if (columns.isEmpty() && preparedHeader.isEmpty()) {
            why = "nothing to draw";
            return;
        }
        prepared = columns;
        sectionsBuilt = serverColumns.size();
        columnsBuilt = columns.size();

        ScaledResolution sr = new ScaledResolution(mc);
        preparedCentre = sr.getScaledWidth() / 2;
        preparedTop = 2 + preparedHeader.size() * LINE_HEIGHT + (preparedHeader.isEmpty() ? 0 : 1);
    }

    /**
     * Every row the client is holding, in the client's own order, with the rows that carry nothing
     * left in - but a blank row is kept here, because it is what separates the sections.
     *
     * The order is Minecraft's own sort (spectator last, then team, then name) and has to be kept:
     * it is what groups the server's stat lines into readable blocks.
     */
    /** One column of the server's tab list: its title, and its lines split into sections. */
    private static final class RawColumn {
        String title = "";
        final List<List<String>> sections = new ArrayList<List<String>>();
    }

    /**
     * Reads the tab list the way it is actually laid out.
     *
     * Minecraft sends the tab list in columns of twenty entries, and the first entry of each column
     * is that column's title - "Players (3)", "Server Info", "Account Info". The reference reads it that
     * way (`parseComponentColumns`, stepping 20 at a time), and every centred heading it draws is
     * just the first entry of a column. Flattening the entries into one list and hunting for the
     * headings afterwards, which is what this used to do, cannot find them.
     *
     * Entries the client renders as empty - the server pads with them - are kept as empty lines,
     * because they are what separates one group of lines from the next inside a column.
     */
    /** The client's own tab list: every row in the client's order, and the names of the players. */
    public static final class ClientList {
        public final List<String> rows = new ArrayList<String>();
        public final java.util.Set<String> players = new java.util.HashSet<String>();
        /** set when the client could not be read at all, for the panel to say out loud */
        public String error = "";
    }

    /**
     * Reads the client's tab list the way the client holds it.
     *
     * This is the only place the list is read from the client, and both users of it go through
     * here: the tab list panel, which draws the rows, and the scoreboard, whose elements come
     * partly out of the list. The scoreboard used to take its rows from the separate every-second
     * reader, and when that reader had nothing in it the scoreboard lost every tab list element -
     * Bank, Interest, the SkyBlock level, the election - while the tab list panel, reading exactly
     * these rows, showed them. One read, one truth.
     */
    public static ClientList clientList(Minecraft mc) {
        ClientList out = new ClientList();
        if (mc == null) {
            out.error = "no client";
            return out;
        }
        try {
            List<NetworkPlayerInfo> infos =
                    new ArrayList<NetworkPlayerInfo>(mc.getNetHandler().getPlayerInfoMap());
            Collections.sort(infos, TabListReader.ordering());

            GuiPlayerTabOverlay overlay = mc.ingameGUI == null ? null : mc.ingameGUI.getTabList();
            for (NetworkPlayerInfo info : infos) {
                String name = profileName(info);
                if (isRealPlayerName(name)) out.players.add(name.toLowerCase(java.util.Locale.ROOT));
                try {
                    String text = overlay == null ? null : overlay.getPlayerName(info);
                    if (text != null) out.rows.add(text);
                } catch (Throwable t) {
                    // an entry with no display name is not a row
                }
            }
        } catch (Throwable t) {
            out.error = String.valueOf(t);
        }
        return out;
    }

    private List<RawColumn> readColumns(Minecraft mc) {
        ClientList list = clientList(mc);
        if (!list.error.isEmpty()) {
            Report.fail("tablist.read", "Reading the tab list", new RuntimeException(list.error));
            why = "reading the client's list failed: " + list.error;
            return new ArrayList<RawColumn>();
        }
        return readColumns(list.rows, list.players, selfName(mc));
    }

    /** The name of the player at the keyboard, or null. */
    private static String selfName(Minecraft mc) {
        try {
            return mc != null && mc.thePlayer != null ? mc.thePlayer.getName() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * The reader proper: the client's rows in the client's own order in, the columns to draw out.
     *
     * Nothing in here touches Minecraft, so a dump's rows can be run through the reader that draws.
     */
    static List<RawColumn> readColumns(List<String> displays, java.util.Set<String> realPlayers) {
        return readColumns(displays, realPlayers, null);
    }

    /** The same, with the name of the player at the keyboard, so their own rows can be dropped. */
    static List<RawColumn> readColumns(List<String> displays, java.util.Set<String> realPlayers,
                                       String self) {
        List<RawColumn> out = new ArrayList<RawColumn>();
        try {
            readEntries = 0;
            droppedEntries = 0;
            java.util.Set<String> drawnPlayers = new java.util.HashSet<String>();
            // On a private island the client sends our rank row but no level roster
            // row. Only suppress the rank when a roster row actually represents us.
            boolean selfInRoster = false;
            if (self != null && !self.isEmpty()) {
                for (String display : displays) {
                    if (display == null) continue;
                    String plain = clean(display).trim();
                    if (rosterRow(plain) && self.equalsIgnoreCase(rowName(plain))) {
                        selfInRoster = true;
                        break;
                    }
                }
            }
            String duplicateSelf = selfInRoster ? self : null;
            // A rank row can land in a different column from the level roster row.
            // Scan all columns before deciding it is the only copy of a player.
            java.util.Set<String> allRostered = new java.util.HashSet<String>();
            for (String display : displays) {
                if (display == null) continue;
                String plain = clean(display).trim();
                if (rosterRow(plain)) {
                    String name = rowName(plain);
                    if (!name.isEmpty()) allRostered.add(name);
                }
            }

            for (int entry = 0; entry < displays.size(); entry += ENTRIES_PER_COLUMN) {
                RawColumn column = new RawColumn();
                List<String> section = new ArrayList<String>();
                int end = Math.min(entry + ENTRIES_PER_COLUMN, displays.size());
                int effectsAt = -1;                 // where the "Active Effects" heading sits
                int cookieAt = -1;                  // where the "Cookie Buff" heading sits
                int effectCount = -1;
                boolean skipNext = false;           // the far half of a sentence the server chopped

                for (int i = entry; i < end; i++) {
                    String text = displays.get(i);
                    if (text == null) continue;
                    readEntries++;

                    String clean = clean(text);
                    if (i == entry) {
                        // the first entry of a column is its title, whatever it says - but a
                        // padding row is never a title, and never a line either: it is not data,
                        // and promoting one to a heading is how a `CLICK` ends up drawn across the
                        // top of a column
                        if (junkRow(clean, realPlayers, duplicateSelf)
                                || redundantRank(clean, allRostered, self)) {
                            droppedEntries++;
                            continue;
                        }
                        if (looksLikeTitle(clean)) column.title = text.trim();
                        else section.add(text);
                        continue;
                    }

                    if (clean.isEmpty()) {
                        if (!section.isEmpty()) {
                            column.sections.add(section);
                            section = new ArrayList<String>();
                        }
                        continue;
                    }
                    if (junkRow(clean, realPlayers, duplicateSelf) || npcRow(clean, realPlayers, duplicateSelf)) {
                        droppedEntries++;
                        continue;
                    }
                    // a real player's own entry, drawn with their rank: the same person the roster
                    // row in this same column already lists. See rankRow.
                    if (redundantRank(clean, allRostered, self)) {
                        droppedEntries++;
                        continue;
                    }
                    if (phraseHidden(clean) || advert(clean)) {
                        droppedEntries++;
                        continue;
                    }
                    if (repeatOfDrawn(clean, realPlayers, drawnPlayers)) {
                        droppedEntries++;
                        continue;
                    }

                    // "You have 28 active effects. Use \"/effects\" to see them!" - the count goes
                    // on the heading above it, and the sentence itself is dropped
                    Matcher effects = ACTIVE_EFFECTS.matcher(clean);
                    if (effects.find()) {
                        try {
                            effectCount = Integer.parseInt(effects.group(1));
                        } catch (Throwable t) {
                            effectCount = -1;
                        }
                        droppedEntries++;
                        skipNext = true;
                        continue;
                    }
                    Matcher use = EFFECTS_USE_COMMAND.matcher(clean);
                    if (use.find()) {
                        droppedEntries++;
                        continue;
                    }
                    if (NO_EFFECTS_ACTIVE.matcher(clean).find()) {
                        droppedEntries++;
                        skipNext = true;
                        continue;
                    }
                    if (NO_COOKIE.matcher(clean).find()) {
                        cookieAt = lastIndexOf(section, COOKIE_HEADING, cookieAt);
                        if (cookieAt < 0) {
                            section.add("\u00a7d\u00a7lCookie Buff\u00a78: \u00a7cINACTIVE");
                        } else {
                            section.set(cookieAt, "\u00a7d\u00a7lCookie Buff\u00a78: \u00a7cINACTIVE");
                        }
                        droppedEntries++;
                        skipNext = true;
                        continue;
                    }
                    if (skipNext) {
                        skipNext = false;
                        if (clean.length() < 60 && FRAGMENT.matcher(clean).find()) {
                            droppedEntries++;
                            continue;              // the far half of a sentence already dealt with
                        }
                    }

                    Matcher heading = EFFECTS_HEADING.matcher(clean);
                    if (heading.matches()) {
                        section.add(text);
                        effectsAt = section.size() - 1;
                        continue;
                    }
                    Matcher cookie = COOKIE_HEADING.matcher(clean);
                    if (cookie.matches()) {
                        section.add(text.trim());
                        cookieAt = section.size() - 1;
                        continue;
                    }

                    section.add(text);

                    // a pet at its top level: the reference prints MAX LEVEL under it
                    if (PET_MAX_LEVEL.matcher(clean).find()) {
                        section.add("\u00a7b\u00a7lMAX LEVEL");
                    }
                }

                // the count arrived, so the heading above it becomes "Active Effects: 28"
                if (effectsAt >= 0 && effectCount >= 0 && effectsAt < section.size()) {
                    section.set(effectsAt, "\u00a7a\u00a7lActive Effects: \u00a7e" + effectCount);
                }
                if (!section.isEmpty()) column.sections.add(section);

                // trailing empty lines are trimmed off the end of a column
                List<List<String>> kept = new ArrayList<List<String>>();
                for (List<String> part : column.sections) {
                    if (!part.isEmpty()) kept.add(part);
                }
                column.sections.clear();
                column.sections.addAll(kept);
                if (column.sections.isEmpty() && column.title.isEmpty()) continue;
                out.add(column);
            }
        } catch (Throwable t) {
            Report.fail("tablist.read", "Reading the tab list", t);
            why = "reading the client's list failed: " + t;
        }
        return out;
    }

    /**
     * The tab list's footer text, whatever the Footer setting says.
     *
     * `banner` answers "is this banner wanted"; the footer's own lines still have to be read when
     * the switch is off, because the effects block and the cookie block inside it are kept either
     * way. The advert rule still applies: it is not a switch.
     */
    /** Raw server footer, including dungeon blessing lines even when the tab panel hides its footer. */
    public static String serverFooter(Minecraft mc) {
        try {
            if (mc == null || mc.ingameGUI == null) return "";
            IChatComponent component = bannerComponent(mc.ingameGUI.getTabList(), false);
            return component == null ? "" : component.getFormattedText();
        } catch (Throwable t) { return ""; }
    }

    private static String footerBanner(Minecraft mc) {
        try {
            if (mc.ingameGUI == null) return "";
            IChatComponent component = bannerComponent(mc.ingameGUI.getTabList(), false);
            if (component == null) return "";
            String text = component.getFormattedText();
            if (bannerAdvert(text)) return "";
            return text;
        } catch (Throwable t) {
            return "";
        }
    }

    /**
     * The footer with the switch off: what the server sends that is data rather than furniture.
     *
     * The player's report: turning Footer off took the active effects and the cookie buff with it.
     * Those two are what this server's footer is for, so they are kept either way and the switch
     * governs the rest. A footer with nothing else in it therefore looks the same at either
     * setting, which is the honest answer for this server - and better than a switch that hides
     * the two lines worth reading.
     */
    private static RawColumn essentialOnly(RawColumn column) {
        if (column == null) return null;
        RawColumn out = new RawColumn();
        out.title = column.title;
        for (List<String> section : column.sections) {
            List<String> kept = new ArrayList<String>();
            for (String line : section) {
                if (isFooterData(clean(line))) kept.add(line);
            }
            if (!kept.isEmpty()) out.sections.add(kept);
        }
        return out.sections.isEmpty() ? null : out;
    }

    /**
     * True when a line of the footer is data rather than furniture.
     *
     * The four the parser produces for those two blocks: the effects heading (with the count or
     * with the god potion's timer under it), the god potion line itself, the cookie heading, and
     * the `Not Active` under it. Everything else in the footer is the server's own padding, prose
     * or advertising.
     */
    private static boolean isFooterData(String clean) {
        String text = clean.toLowerCase(java.util.Locale.ROOT);
        return text.startsWith("active effects") || text.contains("cookie buff")
                || text.contains("god potion") || "not active".equals(text);
    }

    /**
     * The footer as the reference reads it: a column of its own, with the server's sentences turned
     * into the short lines the reference shows.
     *
     * Line by line, from the reference footer matcher:
     *
     *   * the advert line is dropped (the store link lives here, not in a row);
     *   * the sentence that carries the effect count is dropped, and the count goes on the heading
     *     above it - the heading is found first, from the whole footer, because the server puts it
     *     above the sentence;
     *   * `Use "/effects" to see them!` and the `No effects active...` sentence are dropped: the
     *     count already says everything they say;
     *   * `Not active! Obtain booster cookies ...` becomes `Not Active`, under the `Cookie Buff`
     *     heading it belongs to;
     *   * a line with no bold in it is indented by one space, which is what makes the reference's
     *     list read as headings with their lines under them;
     *   * a line that is nothing but colour codes becomes an empty line - a separator.
     */
    private RawColumn parseFooter(List<String> lines) {
        if (lines == null || lines.isEmpty()) return null;

        int effectCount = -1;
        String godPotion = null;
        for (String line : lines) {
            String clean = clean(line);
            if (clean.isEmpty()) continue;
            Matcher count = ACTIVE_EFFECTS.matcher(clean);
            if (count.find()) {
                try {
                    effectCount = Integer.parseInt(count.group(1));
                } catch (Throwable t) {
                    effectCount = -1;
                }
                continue;
            }
            Matcher god = GOD_POTION.matcher(clean);
            if (god.find()) godPotion = god.group("timer");
        }

        RawColumn column = new RawColumn();
        column.title = OTHER_TITLE;
        List<String> section = new ArrayList<String>();
        boolean lastWasNoCookie = false;

        for (String line : lines) {
            String clean = clean(line);
            if (clean.isEmpty()) {
                if (!section.isEmpty()) {
                    column.sections.add(section);
                    section = new ArrayList<String>();
                }
                continue;
            }
            if (advert(clean) || footerAdvert(clean)) continue;
            if (phraseHidden(clean)) continue;
            if (junkRow(clean, null)) continue;
            // The footer is prose - "You are currently connected to hub-4" - and never a roster.
            // This server writes the player's own name into it as a row shaped exactly like a
            // player's ("[VIP] fleetum"), which is how the panel's Other column came to carry a
            // name under its heading when the player looked at it in the Hub. A row shaped like a
            // player's is not data here whoever it names, so it goes.
            if (playerRowShape(clean) || rankRow(clean)) continue;

            Matcher count = ACTIVE_EFFECTS.matcher(clean);
            if (count.find()) continue;                       // the count is on the heading
            if (EFFECTS_USE_COMMAND.matcher(clean).find()) continue;
            if (NO_EFFECTS_ACTIVE.matcher(clean).find()) continue;
            if (GOD_POTION.matcher(clean).find()) continue;   // shown on the heading instead

            if (EFFECTS_HEADING.matcher(clean).matches()) {
                if (godPotion != null) {
                    section.add("\u00a7a\u00a7lActive Effects:");
                    section.add(" \u00a7cGod Potion\u00a7r: " + godPotion);
                } else {
                    section.add("\u00a7a\u00a7lActive Effects: \u00a7e" + Math.max(0, effectCount));
                }
                continue;
            }
            if (COOKIE_HEADING.matcher(clean).matches()) {
                section.add(line.trim());
                lastWasNoCookie = false;
                continue;
            }
            if (NO_COOKIE.matcher(clean).find()) {
                if (!lastWasNoCookie) section.add("\u00a77 Not Active");
                lastWasNoCookie = true;
                continue;
            }

            String formatted = line;
            if (!formatted.contains("\u00a7l")) {
                section.add(" " + formatted);                 // the reference indents these
            } else {
                section.add(formatted);
            }
            lastWasNoCookie = false;
        }
        if (!section.isEmpty()) column.sections.add(section);
        while (!column.sections.isEmpty()
                && column.sections.get(column.sections.size() - 1).isEmpty()) {
            column.sections.remove(column.sections.size() - 1);
        }
        return column.sections.isEmpty() ? null : column;
    }

    /** The tab list's footer lines, colour codes intact. */
    private List<String> footerLines(Minecraft mc) {
        try {
            List<String> lines = split(footerBanner(mc));
            List<String> out = new ArrayList<String>();
            // the blank lines are kept: they are what separates the effects block from the cookie
            // block, and the parser reads them as the section breaks they are
            for (String line : lines) out.add(line == null ? "" : line);
            return out;
        } catch (Throwable t) {
            Report.fail("tablist.footer", "Reading the tab list's footer", t);
            return new ArrayList<String>();
        }
    }

    /** Where a heading sits among the lines so far, or -1. */
    private static int lastIndexOf(List<String> lines, Pattern pattern, int fallback) {
        for (int i = lines.size() - 1; i >= 0; i--) {
            if (pattern.matcher(clean(lines.get(i))).matches()) return i;
        }
        return fallback;
    }

    /** A short line that could be a column's title rather than a stat line. */
    private static boolean looksLikeTitle(String clean) {
        if (clean.length() > 24) return false;
        if (clean.contains(":")) return clean.length() <= 24;
        return HEADING.matcher(clean).matches();
    }

    /**
     * Packs the server's columns into the columns to draw.
     *
     * The reference rule (combineColumnsToRender): a heading only comes along when the column it
     * belongs to changes, a section at least half a column tall forces a new column of its own, and
     * otherwise the lines simply flow until the column is full. The blank line between sections is
     * what makes the list read as groups.
     */
    private static List<Column> combine(List<RawColumn> serverColumns, int maxLines) {
        List<Column> out = new ArrayList<Column>();
        Column current = new Column();
        out.add(current);
        String lastTitle = null;

        for (RawColumn serverColumn : serverColumns) {
            for (List<String> section : serverColumn.sections) {
                if (section.isEmpty()) continue;
                boolean needsTitle = !serverColumn.title.equals(lastTitle);
                boolean large = section.size() + (needsTitle ? 1 : 0) >= Math.max(2, maxLines / 2);

                if (current.lines.size() >= maxLines || (large && current.lines.size() > maxLines / 2)) {
                    current = new Column();
                    out.add(current);
                } else if (current.lines.size() > 0
                        && !current.lines.get(current.lines.size() - 1).isEmpty()) {
                    current.lines.add("");
                }

                if (needsTitle && !serverColumn.title.isEmpty()) {
                    lastTitle = serverColumn.title;
                    if (current.title.isEmpty()) {
                        current.title = serverColumn.title;
                    } else {
                        current.lines.add(serverColumn.title);
                    }
                }
                for (String line : section) {
                    if (current.lines.size() >= maxLines) {
                        current = new Column();
                        out.add(current);
                        if (!serverColumn.title.isEmpty()) current.lines.add(serverColumn.title);
                    }
                    current.lines.add(line);
                }
            }
        }
        // The last point before the lines are measured and drawn, so nothing that carries nothing
        // can reach the screen by any path: not a padding line, and not a padding row that some
        // step promoted to a column's heading.
        for (Column column : out) {
            if (!column.title.isEmpty() && paddingRow(clean(column.title))) column.title = "";
            List<String> kept = new ArrayList<String>();
            for (String line : column.lines) {
                if (paddingRow(clean(line))) droppedEntries++;
                else kept.add(line);
            }
            column.lines.clear();
            column.lines.addAll(kept);
        }
        while (out.size() > 1 && out.get(out.size() - 1).lines.isEmpty()
                && out.get(out.size() - 1).title.isEmpty()) {
            out.remove(out.size() - 1);
        }
        return out;
    }

    /** how a link looks inside a line: a scheme in front of a name, or a dotted name */
    private static final Pattern LINK = Pattern.compile(
            "(?i)(https?://|www\\.|[a-z0-9-]+\\.(?:net|com|me|shop|gg|org|io|store))");

    /**
     * True when the footer carries a link, which is where this server puts the line advertising its
     * store. The footer is one line rather than rows of the list, so the switch that governs the
     * rows has nothing to say about it and this test is its own.
     */
    private static boolean footerAdvert(String clean) {
        return LINK.matcher(clean == null ? "" : clean).find();
    }

    /**
     * True when a row carries nothing to draw.
     *
     * Two shapes, both of them padding the server sends to keep its columns square:
     *
     *   * the `CLICK` rows, which are the server's own filler - they have no name behind them at
     *     all, and the client shows them as a yellow `CLICK` with nothing else;
     *   * a row whose whole content is a player's name, with no level in front of it and no label
     *     on it. This server pads its columns with those. A row with anything else on it - a level
     *     in brackets, a `:` label, a sentence - is content and is kept.
     *
     * A bare word is only taken for a name when it reads as one: it carries a digit, it is long
     * enough that it can only be a name, or the client knows it as a player. `None`, `Cookie Buff`
     * and the server's own words are left alone.
     */
    static boolean junkRow(String clean, java.util.Set<String> knownPlayers) {
        return junkRow(clean, knownPlayers, null);
    }

    static boolean junkRow(String clean, java.util.Set<String> knownPlayers, String self) {
        return paddingRow(clean) || bareNameRow(clean, knownPlayers)
                || notARealPlayerRow(clean, knownPlayers, self);
    }

    /** a row that is nothing but capitals - the shape this server lists its NPCs in */
    private static final Pattern SHOUTED_NAME = Pattern.compile("^[A-Z0-9_' -]{2,24}$");

    /**
     * True when a row looks like a name the server lists but is not a player's.
     *
     * The player, in the Dungeon Hub: "i saw my name again in the tablist or npc names in the
     * tablist which shouldnt be there". The NPCs this server puts in the tab list - the shop links
     * standing at the bottom of a column - are written in capitals, two or three words of them:
     * `GATE KEEPER`, `SHOP`. A real player's row in the same list carries their own name, so a row
     * of capitals that names nobody the client holds is furniture and is not drawn. Capital-letter
     * rows that ARE a player's stay: the roster this server writes inside level brackets is in
     * capitals too.
     */
    static boolean npcRow(String clean, java.util.Set<String> knownPlayers, String self) {
        if (clean == null) return false;
        String text = clean.trim();
        if (text.isEmpty() || text.indexOf(':') >= 0) return false;   // a key line is data
        if (!SHOUTED_NAME.matcher(text).matches()) return false;
        if (!Character.isLetter(text.charAt(0))) return false;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isLowerCase(text.charAt(i))) return false; // capitals only
        }
        String lower = text.toLowerCase(java.util.Locale.ROOT);
        String[] words = lower.split("[^a-z0-9_]+");
        if (self != null && !self.isEmpty()) {
            String me = self.toLowerCase(java.util.Locale.ROOT);
            for (String word : words) {
                if (word.equals(me)) return true;                  // the player's own row
            }
        }
        if (knownPlayers == null || knownPlayers.isEmpty()) return false;
        for (String word : words) {
            if (!word.isEmpty() && knownPlayers.contains(word)) return false;  // somebody's row
        }
        return true;
    }

    /** a first bracket that holds a level - "[217] name", "[Lvl 95] Lion" */
    private static final Pattern LEVEL_BRACKET = Pattern.compile("^\\[(?:lvl\\s*)?\\d+\\]");

    /** the text inside a row's first bracket, lower case, or "" when it has none */
    private static String firstBracket(String clean) {
        String text = clean == null ? "" : clean.trim();
        if (!text.startsWith("[")) return "";
        int end = text.indexOf(']');
        return end < 0 ? "" : text.substring(1, end).trim().toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * True when a row is the island's own roster entry for a player: a level in brackets - "[217]
     * name", the shape this server writes the people on your island in.
     *
     * The player's complaint, twice, from both sides: first *"i saw my name again in the tablist"*,
     * and then, when the row was dropped, *"my name not visible in players"* - with their own
     * screenshot showing `Players (6)` above five names. The roster is the one place the player's
     * own name belongs, so this shape is kept even for the player at the keyboard.
     */
    static boolean rosterRow(String clean) {
        return LEVEL_BRACKET.matcher(clean == null ? "" : clean.trim()).find();
    }

    /**
     * True when a row is a pet's line: a bracket that says the level, then the pet's name.
     *
     * This server writes a pet the way it writes a player - `[Lvl 95] Lion`, `[Lvl 100] Baby Yeti`
     * - and a pet's name is not a player the client knows, so the rule that drops invented player
     * rows was dropping the pet line too: the player's own screen had a `Pet:` heading with
     * nothing under it, and *"nothing in pets"* is how they reported it.
     */
    static boolean petRow(String clean) {
        return firstBracket(clean).replace(" ", "").startsWith("lvl");
    }

    /**
     * True when a row is a player's own entry rather than a line of the roster: a rank in
     * brackets, then the name - "[MVP++] aditbro", "[VIP] nidhhdh".
     *
     * Every real player's entry arrives twice in this server's list: once as the island's own
     * roster row (`[217] aditbro`) and once as the client's entry for that player, drawn with their
     * rank (`[MVP++] aditbro`). The second one is the same person said twice, and it lands wherever
     * the sort puts it - in the player's screenshots it turned up between the bank and the
     * election, and above the active effects, which is how they reported it: *"sometimes shows
     * names above active effects"*. The rank rows carry nothing the roster row does not, so they
     * are not drawn.
     */
    /** the name a player-shaped row carries, lower case, or "" when the row carries none */
    static String rowName(String clean) {
        java.util.regex.Matcher m = PLAYER_ROW.matcher(clean == null ? "" : clean.trim());
        if (!m.matches() || m.group(1) == null) return "";
        return m.group(1).toLowerCase(java.util.Locale.ROOT);
    }

    /** When the server supplies a roster, rank-only rows outside it are stray entries.
     * Keep the local player's rank if the roster omits them (private islands do this).
     * On lists with no roster, rank rows are the only player rows and remain visible. */
    private static boolean redundantRank(String clean, java.util.Set<String> roster, String self) {
        if (!rankRow(clean)) return false;
        String name = rowName(clean);
        if (roster.contains(name)) return true;
        return !roster.isEmpty() && (self == null || !name.equalsIgnoreCase(self));
    }

    static boolean rankRow(String clean) {
        String text = clean == null ? "" : clean.trim();
        if (!text.startsWith("[")) return false;
        if (!PLAYER_ROW.matcher(text).matches()) return false;
        return !rosterRow(text) && !petRow(text);
    }

    /**
     * True when a row has the shape of a player's row: a bracket, then a name, then nothing.
     *
     * Used where a player's row can never be data - the footer's own column - so a name the client
     * holds as a player is dropped there too, which `junkRow` deliberately keeps.
     */
    public static boolean playerRowShape(String clean) {
        if (clean == null) return false;
        String text = clean.trim();
        if (!text.startsWith("[")) return false;
        return PLAYER_ROW.matcher(text).matches();
    }

    /**
     * A row that starts with a bracket - "[217] aditbro ?" or "[MVP++] aditbro" - then a name.
     *
     * The name has to be the last word of the row. This server writes a pet the same way it writes
     * a player - "[Lvl 100] Baby Yeti" - and the pet's name is two words, so a rule that only asked
     * for a name after the brackets read the pet line as somebody's row and dropped it, which is
     * why the player's own photograph of the panel has a `Pet:` heading and nothing under it. A row
     * whose name is followed by another word is content, whatever it is.
     */
    private static final Pattern PLAYER_ROW = Pattern.compile(
            "^(?:\\[[^\\]]*\\]\\s*)+([A-Za-z0-9_]{3,16})[^A-Za-z]*$");

    /**
     * True when a row shaped like a player's row is not about a real player.
     *
     * This server writes the people on your island as "[217] aditbro <symbol>" or "[MVP++] aditbro":
     * a bracket, the name, then a symbol. Two kinds of row look exactly like that and neither
     * belongs on the panel - your own row, and rows about NPCs the server lists alongside the
     * players. The player, in the Dungeon Hub: "i saw my name again in the tablist or npc names in
     * the tablist which shouldnt be there". The client's own player list says who is a player, so a
     * row shaped like a player's that names somebody who is not one is not drawn.
     */
    static boolean notARealPlayerRow(String clean, java.util.Set<String> knownPlayers, String self) {
        if (clean == null) return false;
        String text = clean.trim();
        if (!text.startsWith("[")) return false;
        java.util.regex.Matcher m = PLAYER_ROW.matcher(text);
        if (!m.matches()) return false;
        if (petRow(text) || rosterRow(text)) return false;    // the pet, and the island's own roster
        String name = m.group(1) == null ? "" : m.group(1).toLowerCase(java.util.Locale.ROOT);
        if (name.isEmpty()) return false;
        if (self != null && !self.isEmpty() && name.equals(self.toLowerCase(java.util.Locale.ROOT))) {
            return true;                                  // the player's own row
        }
        return knownPlayers != null && !knownPlayers.isEmpty() && !knownPlayers.contains(name);
    }

    /**
     * True when a row is one the server uses to fill a column out: nothing, or a `CLICK`.
     *
     * The test is on the row's **letters**, not on its exact text. The server decorates these rows
     * and pads them, and a non-breaking space or a zero-width character renders exactly like
     * nothing at all while making an exact match fail - which is how a `CLICK` survived every drop
     * that had been written for it. Whatever the decoration, `CLICK` and `CLICK TO PLAY` are
     * padding.
     */
    static boolean paddingRow(String clean) {
        String text = normalise(clean);
        if (text.isEmpty()) return true;
        if (PAD_LINE.matcher(text).matches()) return true;
        StringBuilder letters = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetter(c)) letters.append(Character.toLowerCase(c));
        }
        String word = letters.toString();
        return "click".equals(word) || "clicktoplay".equals(word);
    }

    /**
     * A row's text with the characters that render as nothing taken out, and its spacing collapsed.
     *
     * The server pads its columns with these: `\u00a0` is a non-breaking space, which draws exactly
     * like a space and survives `trim()`, and `\u200b`, `\u200d`, `\u2060` and `\ufeff` draw as
     * nothing whatever. Every rule that reads a row's text goes through here first.
     */
    private static String normalise(String clean) {
        if (clean == null) return "";
        StringBuilder out = new StringBuilder(clean.length());
        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            if (c == '\u00a0' || c == '\u2007' || c == '\u202f') {
                out.append(' ');
            } else if (c == '\u200b' || c == '\u200c' || c == '\u200d' || c == '\u2060'
                    || c == '\ufeff' || c == '\u00ad' || c == '\u001a') {
                continue;
            } else {
                out.append(c);
            }
        }
        return out.toString().replaceAll("\\s+", " ").trim();
    }

    /**
     * True when a row is one word and nothing else and that word reads as a player name.
     *
     * This is the other half of the server's filler: entries whose whole content is somebody's name,
     * with no level and no label on them. A row with anything else on it - a level in brackets, a
     * `:` label, a sentence - is content and is not one of these, so the party's `[216] aditbro` and
     * every stat line are untouched.
     */
    static boolean bareNameRow(String clean, java.util.Set<String> knownPlayers) {
        if (clean == null) return false;
        String text = clean.trim();
        if (!BARE_NAME.matcher(text).matches()) return false;
        if (knownPlayers != null
                && knownPlayers.contains(text.toLowerCase(java.util.Locale.ROOT))) return true;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isDigit(text.charAt(i))) return true;
        }
        return text.length() >= 8;
    }

    /**
     * True when a row is an advertisement and the player asked for adverts to go.
     *
     * The fire sale list is separate because it is a separate switch: a player may well want the
     * store links gone and the fire sales kept.
     */
    private static boolean advert(String clean) {
        String lower = clean.toLowerCase(java.util.Locale.ROOT);
        if (flag(FIRE_SALE_KEY, false)) {
            for (String word : FIRE_SALE_WORDS) {
                if (lower.contains(word)) return true;
            }
        }
        if (!flag(ADVERTS_KEY, false)) return false;
        for (String word : ADVERT_WORDS) {
            if (lower.contains(word)) return true;
        }
        return false;
    }

    /** A settings switch, read with a default so a missing key is never a failure. */
    private static boolean flag(String key, boolean def) {
        try {
            return SweatsEnhancements.cfg == null ? def : SweatsEnhancements.cfg.getBool(key, def);
        } catch (Throwable t) {
            return def;
        }
    }

    private static int number(String key, int def) {
        try {
            return SweatsEnhancements.cfg == null ? def : SweatsEnhancements.cfg.getInt(key, def);
        } catch (Throwable t) {
            return def;
        }
    }

    /** True when a row contains one of the phrases in the Hide Lines setting. */
    private static boolean phraseHidden(String clean) {
        if (SweatsEnhancements.cfg == null) return false;
        String list = SweatsEnhancements.cfg.get(HIDE_KEY, "");
        if (list == null || list.trim().isEmpty()) return false;
        String lower = clean.toLowerCase();
        for (String piece : list.split(",")) {
            String needle = piece.trim().toLowerCase();
            if (!needle.isEmpty() && lower.contains(needle)) return true;
        }
        return false;
    }

    /**
     * Splits the rows on blank lines, and rewrites the blocks that are worth rewriting.
     *
     * The first row of a section is its title: it is drawn centred over the column and brings
     * it along when the section runs on into the next column, and that is what the packer below does.
     */
    /** How many lines a column holds, which is the Lines Per Column setting or the default. */
    private static int columnLines() {
        int lines = number(COLUMN_LINES_KEY, MAX_LINES);
        if (lines < 5) lines = 5;
        if (lines > 40) lines = 40;
        return lines;
    }

    private void draw() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;
        if (!showing(mc)) {
            prepared = null;
            return;
        }
        if (prepared == null) return;

        FontRenderer fr = mc.fontRendererObj;
        if (fr == null) return;

        List<Column> columns = prepared;
        List<String> headerLines = preparedHeader == null ? new ArrayList<String>() : preparedHeader;
        int centre = preparedCentre;
        int lineHeight = LINE_HEIGHT;

        int percent = SweatsEnhancements.cfg == null ? 100 : SweatsEnhancements.cfg.getInt(SCALE_KEY, 100);
        // 0.9.1's generator wrote the 1.0 default into the file as the whole number 1, which reads
        // back as one percent. A slider that far down is that old value, not a choice.
        if (percent <= 1) percent = 100;
        if (percent < 25) percent = 25;
        if (percent > 200) percent = 200;
        float scale = percent / 100.0F;

        GL11.glPushMatrix();
        try {
            if (scale != 1.0F) {
                // scale from the top centre: the list grows and shrinks towards the middle of the screen
                GL11.glTranslatef(centre, 0.0F, 0.0F);
                GL11.glScalef(scale, scale, 1.0F);
                GL11.glTranslatef(-centre, 0.0F, 0.0F);
            }

            // The reference layout: the list starts 10px from the top of the screen, the header is
            // centred on the whole list, the columns sit under it, and the footer is inside the
            // panel at the bottom.
            int y = 10;
            boolean hideBackground = flag(BACKGROUND_KEY, false);

            int totalWidth = 0 - COLUMN_SPACING;
            int maxLines = 0;
            for (Column column : columns) {
                maxLines = Math.max(maxLines,
                        column.lines.size() + (column.title.isEmpty() ? 0 : 1));
                totalWidth += columnWidth(fr, column) + COLUMN_SPACING;
            }
            if (columns.isEmpty()) totalWidth = 0;
            int headerHeight = headerLines.size() * lineHeight;
            int totalHeight = headerHeight + maxLines * lineHeight;

            int x = centre - totalWidth / 2;

            if (!hideBackground) {
                // one panel behind everything, the same rectangle the reference draws
                drawRect(x - COLUMN_SPACING, y - TAB_PADDING,
                        centre + totalWidth / 2 + COLUMN_SPACING, y + totalHeight + TAB_PADDING,
                        WHOLE_PANEL_COLOUR);
            }

            int middleY = y;
            for (String line : headerLines) {
                fr.drawStringWithShadow(line, centre - fr.getStringWidth(line) / 2, middleY, 0xFFFFFF);
                middleY += lineHeight;
            }

            int middleX = x;
            for (Column column : columns) {
                int width = columnWidth(fr, column);
                int maxWidth = width - TAB_PADDING * 2;
                int lines = column.lines.size() + (column.title.isEmpty() ? 0 : 1);

                // the column's own box, with the reference numbers: the text sits inside it
                drawRect(middleX - TAB_PADDING + 1, middleY - TAB_PADDING + 1,
                        middleX + maxWidth + TAB_PADDING - 2,
                        middleY + lines * lineHeight + TAB_PADDING - 2,
                        hideBackground ? PANEL_COLOUR : COLUMN_OVERLAY_COLOUR);

                int lineY = middleY;
                if (!column.title.isEmpty()) {
                    // the column's own heading, centred over it - the first entry of the column
                    fr.drawStringWithShadow(column.title,
                            middleX + maxWidth / 2 - fr.getStringWidth(column.title) / 2,
                            lineY, 0xFFFFFF);
                    lineY += lineHeight;
                }
                for (String line : column.lines) {
                    int drawX = middleX;
                    if (HEADING.matcher(clean(line)).matches()) {
                        // a title is centred inside its column, like every other title line
                        drawX = middleX + maxWidth / 2 - fr.getStringWidth(line) / 2;
                    }
                    fr.drawStringWithShadow(line, drawX, lineY, 0xFFFFFF);
                    lineY += lineHeight;
                }
                middleX += width + COLUMN_SPACING;
            }

            // there is no footer to draw separately: it is a column like any other, so it sits
            // inside a box with the rest of the list, which is what the reference shows
        } finally {
            GL11.glPopMatrix();
        }

        drawFrames++;
        why = "";
    }

    /** The width of a column: its widest line, with the padding on both sides. */
    private static int columnWidth(FontRenderer fr, Column column) {
        int width = column.title.isEmpty() ? 0 : fr.getStringWidth(column.title);
        for (String line : column.lines) {
            width = Math.max(width, fr.getStringWidth(line));
        }
        return width + TAB_PADDING * 2;
    }

    private static int columnHeight(Column column) {
        int lines = column.lines.size() + (column.title.isEmpty() ? 0 : 1);
        return lines * LINE_HEIGHT + TAB_PADDING * 2;
    }

    // ---------------------------------------------------------------- header and footer

    private static String banner(Minecraft mc, boolean header) {
        if (SweatsEnhancements.cfg != null && !SweatsEnhancements.cfg.getBool(header ? HEADER_KEY : FOOTER_KEY, true)) return "";
        try {
            if (mc.ingameGUI == null) return "";
            IChatComponent component = bannerComponent(mc.ingameGUI.getTabList(), header);
            if (component == null) return "";
            String text = component.getFormattedText();
            if (bannerAdvert(text)) return "";
            return text;
        } catch (Throwable t) {
            return "";
        }
    }

    /**
     * True when a whole banner line is an advertisement.
     *
     * On this server the store advert is not a row in the list - it is one line of the footer, so
     * a switch that only looked at rows would do nothing here. Every line of the banner is looked
     * at, and the banner only goes when all of its lines are adverts, so a footer that mixes a
     * real line with an advert keeps the real line.
     */
    private static boolean bannerAdvert(String text) {
        if (SweatsEnhancements.cfg == null) return false;
        if (!flag(ADVERTS_KEY, false)) return false;
        if (text == null || text.trim().isEmpty()) return false;
        boolean any = false;
        for (String line : text.split("\n")) {
            String clean = EnumChatFormatting.getTextWithoutFormattingCodes(line);
            if (clean == null) continue;
            String lower = clean.trim().toLowerCase(java.util.Locale.ROOT);
            if (lower.isEmpty()) continue;
            any = true;
            boolean hit = false;
            for (String word : ADVERT_WORDS) {
                if (lower.contains(word)) {
                    hit = true;
                    break;
                }
            }
            if (!hit) return false;
        }
        return any;
    }

    /**
     * Picks the header or the footer off the tab list overlay.
     *
     * Both are private IChatComponent fields with no accessor, so they are read by name. The names
     * accepted are the ones that exist at runtime after reobfuscation and the readable ones that
     * exist in a development environment - picking the wrong one of the two would swap the header
     * and the footer, so the names are used and the positional fallback only runs if neither
     * matches.
     */
    private static IChatComponent bannerComponent(GuiPlayerTabOverlay overlay, boolean header) {
        if (overlay == null) return null;
        String wanted = header ? "field_175256_i" : "field_175255_h";
        String readable = header ? "header" : "footer";
        IChatComponent fallback = null;
        IChatComponent first = null;
        IChatComponent second = null;
        int seen = 0;

        for (Field field : GuiPlayerTabOverlay.class.getDeclaredFields()) {
            if (field.getType() != IChatComponent.class) continue;
            seen++;
            try {
                field.setAccessible(true);
                IChatComponent value = (IChatComponent) field.get(overlay);
                if (field.getName().equals(wanted) || field.getName().equals(readable)) return value;
                if (seen == 1) first = value;
                else if (seen == 2) second = value;
            } catch (Throwable t) {
                // a field that will not open is simply not used
            }
        }
        if (seen < 2) return null;
        // declaration order in this client: footer first, then header
        fallback = header ? second : first;
        return fallback;
    }

    private static List<String> split(String text) {
        List<String> out = new ArrayList<String>();
        if (text == null || text.isEmpty()) return out;
        for (String piece : text.split("\n")) {
            out.add(piece);
        }
        return out;
    }

    /** The line without its colour codes, for the rules that read the text itself. */
    /** "aditbro", "CRAFTY_IKALIXX" - a name the client is holding as a real player. */
    private static boolean isRealPlayerName(String name) {
        return name != null && name.matches("[A-Za-z0-9_]{3,16}");
    }

    /** The name the client holds for an entry, or "" when it holds none. */
    private static String profileName(NetworkPlayerInfo info) {
        try {
            return info.getGameProfile() == null ? "" : info.getGameProfile().getName();
        } catch (Throwable t) {
            return "";
        }
    }

    /**
     * True when this row is another writing of a player the list has already drawn.
     *
     * The players a row is about are the real names that appear in it as whole words, so
     * "[216] aditbro >" and "[VIP+] aditbro" and "aditbro" are all about aditbro. A row about a
     * player already drawn is dropped; otherwise the player is marked drawn and the row is kept.
     * The names are checked as whole words, so one player's name inside another's is not a match.
     */
    public static boolean repeatOfDrawn(String clean, java.util.Set<String> realPlayers,
                                 java.util.Set<String> drawnPlayers) {
        if (realPlayers.isEmpty()) return false;
        String lower = clean.toLowerCase(java.util.Locale.ROOT);
        java.util.List<String> about = new java.util.ArrayList<String>();
        for (String name : realPlayers) {
            if (name.isEmpty()) continue;
            if (java.util.regex.Pattern.compile("(?<![a-z0-9_])" + java.util.regex.Pattern.quote(name)
                    + "(?![a-z0-9_])").matcher(lower).find()) {
                about.add(name);
            }
        }
        if (about.isEmpty()) return false;
        if (drawnPlayers.containsAll(about)) return true;
        drawnPlayers.addAll(about);
        return false;
    }

    /** The same cleaning the panel's own rows get, for the scoreboard's copy of them. */
    public static String cleanRow(String raw) {
        return clean(raw);
    }

    private static String clean(String raw) {
        String plain = EnumChatFormatting.getTextWithoutFormattingCodes(raw);
        return plain == null ? "" : normalise(plain);
    }

    /** The settings the list is actually drawing with, for /se panels. */
    public static String probeSettings() {
        try {
            int percent = SweatsEnhancements.cfg == null ? 100 : SweatsEnhancements.cfg.getInt(SCALE_KEY, 100);
            if (percent <= 1) percent = 100;
            if (percent < 25) percent = 25;
            if (percent > 200) percent = 200;
            String hide = SweatsEnhancements.cfg == null ? "" : SweatsEnhancements.cfg.get(HIDE_KEY, "");
            return "scale " + percent + "%, " + (toggleMode() ? "press TAB to toggle" : "hold TAB")
                    + ", " + columnLines() + " lines per column, background "
                    + (flag(BACKGROUND_KEY, false) ? "off" : "on")
                    + ", adverts " + flag(ADVERTS_KEY, false)
                    + ", fire sales " + flag(FIRE_SALE_KEY, false)
                    + ", hide lines: " + (hide == null || hide.trim().isEmpty() ? "(none)" : hide);
        } catch (Throwable t) {
            return "settings unreadable: " + t;
        }
    }

    /** Clears the counters behind /se panels, so a report starts from zero. */
    public static void resetDiagnostics() {
        framesPreList = 0;
        framesPostList = 0;
        framesPostAll = 0;
        framesText = 0;
        tabFrames = 0;
        cancelFrames = 0;
        drawFrames = 0;
        readEntries = 0;
        droppedEntries = 0;
        columnsBuilt = 0;
        sectionsBuilt = 0;
        why = "not asked yet";
    }
}
