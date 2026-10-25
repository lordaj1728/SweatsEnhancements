package com.sweatsenhancements.feature;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.chat.Report;
import com.sweatsenhancements.config.Cfg;
import com.sweatsenhancements.core.PanelSlots;
import com.sweatsenhancements.core.RichText;
import com.sweatsenhancements.core.SidebarClaims;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import org.lwjgl.opengl.GL11;

/**
 * Custom Scoreboard - draws the sidebar ourselves instead of letting the client draw it.
 *
 * Shape and settings follow the reference layout: the
 * vanilla panel is replaced, the server's lines are kept, the red score numbers are gone by
 * default, a title and a footer can be set, and the whole thing sits in a cleaner panel with a
 * configurable background. No code was taken from it - the layout here is the client's own.
 *
 * HOW THE VANILLA SIDEBAR IS HIDDEN, since it matters and it is not obvious:
 *
 *   1.8.9 Forge fires no event for the sidebar. The client draws it in GuiIngameForge.renderGameOverlay
 *   straight after the Pre(ALL) event, guarded only by "is there an objective in a display slot".
 *   So the sidebar cannot be cancelled - it can only be made not to exist. That is what this class
 *   does: it reads the objective the server put in the sidebar slot, keeps a reference to it, and
 *   clears the slot. The objective still holds all its lines, so the sidebar can be redrawn from
 *   it; it is only no longer displayed. The slot is cleared again every frame as well as every tick,
 *   because the server re-sends the display slot whenever it refreshes the sidebar.
 *
 *   The slots are 1 (the sidebar) and 3..18 (the colour-coded sidebars the client picks between
 *   when the player's team has a chat colour). Slot 0 is the tab list's objective and is left alone.
 *
 * NOTHING IS HIDDEN THAT CANNOT BE REPLACED. The slot is only cleared on a frame where this class
 * has lines to draw. A sidebar whose every line is empty once the colour codes are stripped - the
 * Garden's is twelve lines of nothing but colour codes, with the values sitting in the score
 * numbers - is left exactly as the client drew it, because an empty panel in its place would be
 * worse than the rows it replaced. In that case a line is said in chat explaining why.
 *
 * Every failure is reported through Report instead of being swallowed, so a panel that does nothing
 * can be asked why: /se panels prints what each stage of this class has seen.
 */
public class CustomScoreboard extends Gui {

    public static final String TITLE_KEY = "scoreboard.title";
    public static final String NUMBERS_KEY = "scoreboard.showNumbers";
    public static final String MAX_LINES_KEY = "scoreboard.maxLines";
    public static final String HIDE_KEY = "scoreboard.hideLines";
    public static final String FOOTER_KEY = "scoreboard.footer";
    public static final String OPACITY_KEY = "scoreboard.opacity";
    public static final String SCALE_KEY = "scoreboard.scale";
    public static final String ORDER_KEY = "scoreboard.appearance";
    /** whether the client's own sidebar is taken off the screen at all */
    public static final String HIDE_VANILLA_KEY = "scoreboard.hideVanilla";
    /** skip drawing while TAB is held / while chat is open */
    public static final String HIDE_WHILE_TAB_KEY = "scoreboard.hideWhileTab";
    public static final String HIDE_WHILE_CHAT_KEY = "scoreboard.hideWhileChat";
    /** the shadow behind the text, the background's colour and how dark it is */
    public static final String SHADOW_KEY = "scoreboard.textShadow";
    public static final String BACKGROUND_KEY = "scoreboard.background";
    /** how far apart the lines sit, and where the panel sits on the screen */
    public static final String SPACING_KEY = "scoreboard.lineSpacing";
    public static final String VERTICAL_KEY = "scoreboard.verticalAlign";
    public static final String HORIZONTAL_KEY = "scoreboard.horizontalAlign";
    public static final String TEXT_ALIGN_KEY = "scoreboard.defaultAlign";
    public static final String FREE_X_KEY = "scoreboard.freeX";
    public static final String FREE_Y_KEY = "scoreboard.freeY";
    /** title and footer */
    public static final String SERVER_TITLE_KEY = "scoreboard.useServerTitle";
    public static final String TITLE_CUSTOM_KEY = "scoreboard.titleCustom";
    public static final String TITLE_ANYWHERE_KEY = "scoreboard.titleAnywhere";
    public static final String TITLE_ALIGN_KEY = "scoreboard.titleAlign";
    public static final String FOOTER_CUSTOM_KEY = "scoreboard.footerCustom";
    public static final String FOOTER_TEXT_KEY = "scoreboard.footerText";
    public static final String FOOTER_ALIGN_KEY = "scoreboard.footerAlign";
    /** the reference's Margin, which the panel keeps from the screen edge it is anchored to */
    public static final String MARGIN_KEY = "scoreboard.margin";
    /** the reference's Background Options -> Enabled */
    public static final String BACKGROUND_ON_KEY = "scoreboard.backgroundEnabled";
    /** the reference's Information Filtering -> Hide empty lines at top/bottom */
    public static final String HIDE_EDGES_KEY = "scoreboard.hideEmptyEdges";
    /** the reference's Information Filtering -> Hide consecutive empty lines */
    public static final String HIDE_CONSECUTIVE_KEY = "scoreboard.hideConsecutiveEmpty";

    /** the background's shape: padding around the text, corner rounding, and the outline */
    public static final String BORDER_KEY = "scoreboard.borderSize";
    public static final String ROUND_KEY = "scoreboard.round";
    public static final String OUTLINE_KEY = "scoreboard.outline";
    public static final String OUTLINE_SIZE_KEY = "scoreboard.outlineThickness";
    public static final String OUTLINE_TOP_KEY = "scoreboard.outlineTop";
    public static final String OUTLINE_BOTTOM_KEY = "scoreboard.outlineBottom";

    /** the warning about lines this mod does not recognise, and the list itself */
    public static final String UNKNOWN_WARNING_KEY = "scoreboard.unknownLines";
    // the "show the unknown lines in the panel" switch is gone with the section it drew: the panel
    // already draws a line it cannot place, so the section only ever repeated it

    /** the client's own limit, and the count it keeps when a sidebar is longer */
    /** how many lines the panel draws when the setting has never been written: the client's own
     * scoreboard holds fifteen, but this panel also carries the tab list's elements, and fifteen
     * cut the election in half on the player's screen */
    private static final int VANILLA_MAX_LINES = 25;

    /** the display slot the sidebar lives in, and the two either side of it that are not sidebars */
    private static final int SIDEBAR_SLOT = 1;
    private static final int BELOW_NAME_SLOT = 2;
    private static final int FIRST_TEAM_SLOT = 3;
    private static final int LAST_TEAM_SLOT = 18;

    private static final SimpleDateFormat CLOCK = new SimpleDateFormat("HH:mm:ss");

    // ---------------------------------------------------------------- diagnostics, read by /se panels
    /** every Pre(ALL) this class was handed */
    public static int framesPre;
    /** every Post(ALL) this class was handed */
    public static int framesPost;
    /** every Text event this class was handed, whether or not it drew there */
    public static int framesText;
    /** frames where the vanilla sidebar was actually taken out of its display slot */
    public static int hidFrames;
    /** frames where the panel was actually drawn */
    public static int drawFrames;
    /** ticks the switch has been on, so a report is not sent before the server has sent a sidebar */
    public static int ticksOn;
    /**
     * Frames the client's own sidebar has been kept off the screen without this panel drawing one.
     *
     * It has to stay at zero. A panel that hides the client's sidebar and then fails to draw its own
     * leaves the player with no sidebar at all, which looks exactly like the feature deleting it -
     * so after a second of that, the client's sidebar is put back and the reason is said out loud.
     */
    public static int hiddenWithoutDrawing;
    /** the rows the panel built on the last read, with the element each one came from */
    private static volatile List<PanelLines.Row> lastBuilt = new ArrayList<PanelLines.Row>();
    /** where the tab list's rows came from on the last read, for /se dump */
    public static volatile String tabSource = "not read yet";
    /** lines the server sent that no element claimed, on the last read */
    public static volatile int unknownCount;
    /** the first few of those, for the warning and for /se panels */
    public static volatile List<String> unknownSample = new ArrayList<String>();
    /** why nothing is on the screen: one short sentence, or "" while the panel is drawing */
    public static volatile String why = "not asked yet";

    /** one line of the sidebar: what it says, and the number the server scored it */
    private static final class Row {
        final String text;
        final int points;

        Row(String text, int points) {
            this.text = text;
            this.points = points;
        }
    }

    /**
     * The objective the server is using for the sidebar, REMEMBERED after its display slot is
     * cleared.
     *
     * It is not re-read from the scoreboard every frame, because emptying the display slot is
     * exactly what this feature does. A scan that trusted the slots found nothing on the next
     * frame, concluded the server had no sidebar, and dropped its own reference to the one it was
     * hiding - so it could neither draw it nor put it back, and the player was left with no
     * sidebar from the client and none from this panel. That is the state the first in-game test
     * of the finished panels reported.
     */
    private ScoreObjective objective;
    /** which display slot it came from, so it can be put back when this feature is switched off */
    private int objectiveSlot = 1;
    /** true while the empty display slots are this feature's own doing rather than the server's */
    private boolean hidByUs;
    /** whether the switch was on last tick, so the tick it goes off can be noticed */
    private boolean wasOn;
    /**
     * Ticks left in which the switch going off is still allowed to put the client's sidebar back.
     *
     * Bounded on purpose: the server is allowed to take its sidebar away whenever it likes, and
     * after these five seconds the server's word is final again.
     */
    private int repairTicks;
    /** where the panel was drawn last, so /se panels can say where it is */
    public static volatile int boxLeft;
    public static volatile int boxTop;
    public static volatile int boxWidth;
    public static volatile int boxHeight;
    /** where the first line of text starts inside that box, and how tall the text block is */
    public static volatile int boxFirstY;
    public static volatile int boxBodyHeight;
    private static volatile int boxBorder = 5;
    private static volatile int boxSpacing = 10;
    private volatile String title = "";
    private volatile String objectiveName = "";
    /** the server's lines in the server's own order, before anything was claimed or dropped */
    private volatile List<Row> raw = new ArrayList<Row>();
    /** Read-only bridge for clients that need the actual server objective while SweatsEnhancements hides slot 1.
     * Never puts it back on screen. A stale objective from another world is rejected. */
    public ScoreObjective retainedServerSidebar(Scoreboard board) {
        ScoreObjective saved = objective;
        return board != null && saved != null && board.getObjective(saved.getName()) == saved
                ? saved : null;
    }

    /** Unmodified server sidebar, independent of user-hidden rows and display-slot suppression. */
    public List<String> serverSidebarLines() {
        List<String> lines = new ArrayList<String>();
        lines.add(title);
        for (Row row : raw) lines.add(row.text);
        return lines;
    }

    private volatile int rowsRead;
    private volatile int rowsDrawn;
    private volatile List<Row> rows = new ArrayList<Row>();

    public static boolean on() {
        try {
            return SweatsEnhancements.cfg != null && SweatsEnhancements.cfg.getBool(Cfg.key("MS085"), false);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * What the rows were built from: the sidebar's lines, the tab list's content version, how many
     * players there are, and the settings version. Equal means nothing the panel reads has moved,
     * so the rebuild can be skipped.
     *
     * Kept as a plain function of its inputs rather than inlined, so the offline tests can show it
     * changing when - and only when - one of the four really changes.
     */
    public static int rowFingerprint(List<PanelLines.Raw> sidebar, int tabVersion, int players,
                                     int cfgVersion) {
        int fingerprint = 17;
        if (sidebar != null) {
            for (PanelLines.Raw line : sidebar) {
                fingerprint = fingerprint * 31
                        + (line == null || line.text == null ? 0 : line.text.hashCode());
            }
        }
        fingerprint = fingerprint * 31 + tabVersion;
        fingerprint = fingerprint * 31 + players;
        fingerprint = fingerprint * 31 + cfgVersion;
        return fingerprint;
    }

    /** the inputs the rows were last built from, so an unchanged sidebar does not rebuild them */
    private int lastFingerprint;
    /** how many rebuilds the fingerprint has saved, for /se panels */
    public static volatile int buildsSkipped;

    /** set once a world has loaded, so the "both switched off" hint is said once per session */
    private static boolean joined;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        joinHint();
        if (!on()) {
            if (wasOn) {
                // the switch has just gone off: put the client's sidebar back, and stay willing to
                // do it again for a moment in case the world is still settling around us
                restore();
                repairTicks = 100;
            }
            wasOn = false;
            ticksOn = 0;
        } else {
            wasOn = true;
            ticksOn++;
        }
        // Read what the server is sending whether or not the panel is switched on: /se sidebar
        // shows what the slots claimed, and it has to work even while nothing is being replaced.
        try {
            scan(true);
        } catch (Throwable t) {
            Report.fail("scoreboard.read", "Reading the sidebar", t);
        }
        if (!on() && repairTicks > 0) {
            repairTicks--;
            try {
                repair();
            } catch (Throwable t) {
                Report.fail("scoreboard.restore", "Putting the client's sidebar back", t);
            }
        }
        if (on()) {
            try {
                hide();
            } catch (Throwable t) {
                Report.fail("scoreboard.hide", "Hiding the client's sidebar", t);
            }
            announce();
        }
    }

    /**
     * The first time a world is loaded, and only if both panels are switched off, one line saying
     * how to switch them on. Temporary - it is here because "the panel does nothing" and "the panel
     * is switched off" are impossible to tell apart from the outside, which is exactly what went
     * wrong in 0.9.0. Remove it once both panels are confirmed working.
     */
    private static void joinHint() {
        if (joined) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;
        joined = true;
        if (on() || CustomTabList.on()) return;
        Report.warnOnce("panels.off",
                "Custom Scoreboard and Compact Tab List are both switched off. /se panels on switches"
                + " both on, /se menu opens the settings.");
    }

    /**
     * The one line that tells the player which of the three states this panel is in.
     *
     * Only said once, and only after three seconds of the switch being on, so that the second
     * after a server change is not mistaken for "the server has no sidebar".
     */
    private void announce() {
        if (ticksOn < 60) return;
        if (objective == null) {
            Report.blockedOnce("scoreboard.noobjective",
                    "Custom Scoreboard is on, but this server has no sidebar up right now.");
        } else if (!hasSomethingToShow()) {
            Report.blockedOnce("scoreboard.empty",
                    "Custom Scoreboard is on, but every line of this sidebar is empty once the colour"
                    + " codes are stripped (the values live in the score numbers). The client's own"
                    + " sidebar is left in place. Turning on Score Numbers shows this one.");
        } else {
            Report.noteOnce("scoreboard.first",
                    "Custom Scoreboard is drawing " + String.valueOf(rowsDrawn) + " of "
                    + String.valueOf(rowsRead) + " sidebar lines, in your slot order."
                    + " /se sidebar shows the slots.");
        }
    }

    /**
     * Puts the sidebar back in its display slot, for when the switch is turned off.
     *
     * Without this the sidebar would stay hidden until the server next refreshes it, which can be
     * minutes - the switch has to look like it did something.
     */
    private void restore() {
        hidByUs = false;
        if (objective == null) return;
        putBack();
        objective = null;
        // the name stays: repair() finds the sidebar by name for a moment after the switch goes
        // off, so "off puts the original back" does not depend on a reference surviving a frame
        rows = new ArrayList<Row>();
        why = "switched off - the client's sidebar is back";
    }

    /**
     * Puts the client's own sidebar back by name.
     *
     * Called for five seconds after the switch goes off. Nothing happens unless the objective we
     * were hiding is still in the scoreboard and is on no display slot at all - that is the state
     * this feature can leave behind if anything goes wrong, and it is the state the player has no
     * way to get out of from the outside.
     */
    private void repair() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null) return;
        Scoreboard board = mc.theWorld.getScoreboard();
        if (board == null || objectiveName.isEmpty()) return;
        ScoreObjective obj = board.getObjective(objectiveName);
        if (obj == null) return;
        for (int slot = SIDEBAR_SLOT; slot <= LAST_TEAM_SLOT; slot++) {
            if (slot == BELOW_NAME_SLOT) continue;
            if (board.getObjectiveInDisplaySlot(slot) == obj) return;   // already on the screen
        }
        board.setObjectiveInDisplaySlot(objectiveSlot, obj);
        why = "the client's sidebar was put back in display slot " + objectiveSlot;
    }

    /** Puts the objective back in its slot but keeps hold of it, for the empty-line case. */
    private void putBack() {
        if (objective == null) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.theWorld != null) {
                Scoreboard board = mc.theWorld.getScoreboard();
                if (board != null && board.getObjective(objective.getName()) != null) {
                    board.setObjectiveInDisplaySlot(objectiveSlot, objective);
                }
            }
        } catch (Throwable t) {
            // the scoreboard moved on; the next sidebar update brings it back by itself
        }
    }

    /** Runs before the client draws the overlay, so the sidebar never reaches the screen. */
    @SubscribeEvent
    public void onOverlayPre(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL
                && event.type != RenderGameOverlayEvent.ElementType.HOTBAR) return;
        if (event.type == RenderGameOverlayEvent.ElementType.ALL) framesPre++;
        // Some overlay paths skip ALL. HOTBAR is an early second chance to
        // clear a sidebar the server restored after the last tick.
        if (on()) {
            try {
                scan(false);
                hide();
            } catch (Throwable t) {
                Report.fail("scoreboard.hide", "Hiding the client's sidebar", t);
            }
            return;
        }
        try {
            scan(false);
        } catch (Throwable t) {
            Report.fail("scoreboard.read", "Reading the sidebar", t);
        }
    }

    /**
     * Draws when Post(ALL) never arrives.
     *
     * Post(ALL) is the last event of the frame and is therefore the right place for a panel that
     * belongs on top of everything: it is fired at the very end of GuiIngameForge.renderGameOverlay
     * on this Forge version, after the chat and the player list. If a client turns out not to send
     * it - the counters say so - this takes over instead of leaving the panel invisible. It only
     * ever steps in when Post(ALL) has not been seen at all, so a frame can never be drawn twice:
     * the panel has a translucent background, and drawing it twice would stack the alpha.
     */
    @SubscribeEvent
    public void onOverlayText(RenderGameOverlayEvent.Text event) {
        framesText++;
        if (on()) {
            try { scan(false); hide(); }
            catch (Throwable t) { Report.fail("scoreboard.hide", "Hiding the client sidebar", t); }
        }
        if (framesPost != 0) return;
        if (framesText < 40) return;
        if (!on()) return;
        drawGuarded();
    }

    @SubscribeEvent
    public void onOverlayPost(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        framesPost++;
        if (!on()) return;
        drawGuarded();
    }

    private void drawGuarded() {
        try {
            draw();
        } catch (Throwable t) {
            Report.fail("scoreboard.draw", "Drawing the scoreboard", t);
            why = "drawing failed: " + t;
        }
    }

    /**
     * Finds the sidebar objective, remembers it, and reads it.
     *
     * Called every frame as well as every tick: the server re-sends the display slot whenever it
     * updates the sidebar, and a sidebar that flickers back in for a frame looks broken. Clearing
     * the slot is a separate step - see hide() - so that reading the sidebar and replacing it are
     * not the same decision.
     */
    private void scan(boolean readLines) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null) {
            why = "not in a world";
            return;
        }
        Scoreboard board = mc.theWorld.getScoreboard();
        if (board == null) {
            why = "this world has no scoreboard";
            return;
        }

        // Slot 1 is the sidebar. Slot 2 is the health shown under every player's name, slot 0 is
        // the tab list's objective, and neither of those is a sidebar - taking the last non-empty
        // slot, which is what this did, drew the below-name objective in the sidebar's place.
        // Slots 3..18 are the colour-coded sidebars a player on a coloured team can be given, so
        // they are the only fallback. Slots 0 and 2 are never touched, here or in hide().
        ScoreObjective found = board.getObjectiveInDisplaySlot(SIDEBAR_SLOT);
        int foundSlot = SIDEBAR_SLOT;
        if (found == null) {
            for (int slot = FIRST_TEAM_SLOT; slot <= LAST_TEAM_SLOT; slot++) {
                ScoreObjective shown = board.getObjectiveInDisplaySlot(slot);
                if (shown != null) {
                    found = shown;
                    foundSlot = slot;
                    break;
                }
            }
        }
        if (found != null) {
            // the server has the sidebar up: this is the one, whatever was held before
            objective = found;
            objectiveSlot = foundSlot;
            objectiveName = objective.getName();
            title = objective.getDisplayName() == null ? "" : objective.getDisplayName();
            hidByUs = false;
        } else if (objective != null && board.getObjective(objective.getName()) != objective) {
            // A world change may replace the scoreboard while our former slot was hidden.
            // Do not draw the previous island's title or rows in a lobby.
            objective = null;
            objectiveName = "";
            title = "";
            hidByUs = false;
        }
        if (readLines && objective != null) read(mc, board);

        if (objective == null) {
            why = "the server has no sidebar objective";
            return;
        }
        if (!hasSomethingToShow()) {
            // Never leave the client's sidebar hidden with nothing of ours to put in its place.
            if (on()) putBack();
            why = "this sidebar has no lines left after the filters";
            return;
        }
        if (on()) why = "";
    }

    /** Takes the objective out of its display slot, and only when there is a line to draw. */
    private void hide() {
        if (!skyBlockTitle(title)) {
            if (hidByUs) putBack();
            hidByUs = false;
            return;
        }
        if (!hasSomethingToShow()) return;
        if (hiddenWithoutDrawing > 120) {
            // a second of hiding with nothing drawn: give the client its sidebar back and stop
            putBack();
            hidByUs = false;
            why = "the panel has not managed to draw for a second, so the client's sidebar is back";
            Report.blockedOnce("scoreboard.nodraw",
                    "Custom Scoreboard is hiding the client's sidebar but has not drawn its own for"
                    + " a second, so the client's one is back in place. /se panels says why, and"
                    + " /se panels off switches the panel off.");
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null) return;
        Scoreboard board = mc.theWorld.getScoreboard();
        if (board == null) return;

        int cleared = 0;
        for (int slot = SIDEBAR_SLOT; slot <= LAST_TEAM_SLOT; slot++) {
            if (slot == BELOW_NAME_SLOT) continue;   // the health under every name is not ours to hide
            if (board.getObjectiveInDisplaySlot(slot) != null) {
                board.setObjectiveInDisplaySlot(slot, null);
                cleared++;
            }
        }
        if (cleared > 0) {
            hidFrames++;
            hiddenWithoutDrawing++;
            hidByUs = true;
        }
    }

    /** True when this class has lines (or a title) worth putting on the screen. */
    private boolean hasSomethingToShow() {
        if (objective == null) return false;
        if (!title.isEmpty()) return true;
        // the custom title is the one the player typed, so it is asked for through the same
        // method the drawing uses - a panel that draws nothing must never be the reason the
        // client's own sidebar is hidden
        if (!heading().isEmpty()) return true;
        return !rows.isEmpty();
    }

    /** The order of elements the player asked for, or the default when the config says nothing. */
    private static List<String> order() {
        return PanelLines.list(ORDER_KEY, PanelSlots.DEFAULT_APPEARANCE);
    }

    /** A settings switch, read with a default so a missing key is never a failure. */
    private static boolean flag(String key, boolean def) {
        try {
            return SweatsEnhancements.cfg == null ? def : SweatsEnhancements.cfg.getBool(key, def);
        } catch (Throwable t) {
            return def;
        }
    }

    private static String setting(String key, String def) {
        try {
            String v = SweatsEnhancements.cfg == null ? null : SweatsEnhancements.cfg.get(key, def);
            return v == null ? def : v;
        } catch (Throwable t) {
            return def;
        }
    }

    private static int setting(String key, int def) {
        try {
            return SweatsEnhancements.cfg == null ? def : SweatsEnhancements.cfg.getInt(key, def);
        } catch (Throwable t) {
            return def;
        }
    }

    /** The row holding this text, taking each row only once so repeated lines stay separate. */
    private static Row take(List<Row> pool, String text) {
        for (int i = 0; i < pool.size(); i++) {
            Row row = pool.get(i);
            if (row.text.equals(text)) {
                pool.remove(i);
                return row;
            }
        }
        return new Row(text, 0);
    }

    private static String configuredTitle() {
        try {
            String set = SweatsEnhancements.cfg == null ? "" : SweatsEnhancements.cfg.get(TITLE_KEY, "");
            return set == null ? "" : set.trim();
        } catch (Throwable t) {
            return "";
        }
    }

    /**
     * Copies the sidebar into this class, in an order the player chose.
     *
     * The claiming itself lives in PanelLines, which is pure text work and is shared with the
     * 1.21.11 port: every line the server sent is claimed by an element, the elements are drawn in
     * the Appearance order, and a line nothing claimed keeps its place in EXTRA. What is left here
     * is the part that needs the client: reading the objective, applying the player's own hide
     * list, and taking the measurements the panel is drawn with.
     */
    private void read(Minecraft mc, Scoreboard board) {
        ScoreObjective obj = objective;
        if (obj == null) {
            title = "";
            objectiveName = "";
            rowsRead = 0;
            rows = new ArrayList<Row>();
            return;
        }

        title = obj.getDisplayName() == null ? "" : obj.getDisplayName();
        objectiveName = obj.getName() == null ? "" : obj.getName();

        List<Row> found = new ArrayList<Row>();
        try {
            for (Score score : new ArrayList<Score>(board.getSortedScores(obj))) {
                String name = score.getPlayerName();
                // the client hides these itself: entries starting with '#' are used as spacers
                if (name == null || name.startsWith("#")) continue;
                String text = ScorePlayerTeam.formatPlayerName(board.getPlayersTeam(name), name);
                found.add(new Row(text, score.getScorePoints()));
            }
        } catch (Throwable t) {
            // one bad line must not cost the other fourteen
        }

        raw = found;
        rowsRead = found.size();

        // The player's own hide list runs first, so a line that is switched off does not hold a
        // place in the arrangement either.
        List<PanelLines.Raw> sidebar = new ArrayList<PanelLines.Raw>();
        for (Row row : found) {
            if (hidden(row.text)) continue;
            String clean = EnumChatFormatting.getTextWithoutFormattingCodes(row.text);
            sidebar.add(new PanelLines.Raw(row.text, row.points, clean == null || clean.trim().isEmpty()));
        }

        // The tab list is where this server keeps the numbers the sidebar does not show: Bank,
        // Interest, Gems, the SkyBlock level, the stat block. Read on the tick by TabListReader.
        List<String> tabRaw = new ArrayList<String>();
        List<String> tabClean = new ArrayList<String>();
        java.util.Set<String> players = new java.util.HashSet<String>();
        // what the tab list looked like, as one number, so the rebuild below happens when it moves
        // whichever way the rows were read
        int tabStamp = 0;
        try {
            // The client's own list first, the same call the tab list panel uses, so the two
            // panels can never be showing two different lists. The every-second reader is the
            // fallback: its rows are a snapshot a tick old, and its names are every entry the
            // client holds - the server's invented rows included - while the client call filters
            // them. Reading it first is how the scoreboard came to be fed an empty list while the
            // tab list carried everything.
            CustomTabList.ClientList client = mc == null ? null : CustomTabList.clientList(mc);
            if (client != null && !client.rows.isEmpty()) {
                for (String row : client.rows) {
                    tabRaw.add(row);
                    tabClean.add(CustomTabList.cleanRow(row));
                }
                players = client.players;
                for (String row : tabClean) {
                    tabStamp = tabStamp * 31 + (row == null ? 0 : row.hashCode());
                }
                tabSource = "the client's tab list, read here"
                        + (client.error.isEmpty() ? "" : " (failed: " + client.error + ")");
            } else if (SweatsEnhancements.tabList != null && !SweatsEnhancements.tabList.lines().isEmpty()) {
                for (TabListReader.Line line : SweatsEnhancements.tabList.lines()) {
                    tabRaw.add(line.raw);
                    tabClean.add(line.clean);
                }
                players = SweatsEnhancements.tabList.playerNames();
                tabStamp = SweatsEnhancements.tabList.version();
                tabSource = "the every-second reader — " + SweatsEnhancements.tabList.statusLine();
            }
            if (tabRaw.isEmpty()) {
                tabSource = "nothing yet — the client held no rows"
                        + (SweatsEnhancements.tabList == null ? "" : "; reader: " + SweatsEnhancements.tabList.statusLine());
            }
        } catch (Throwable t) {
            Report.fail("scoreboard.tab", "Reading the tab list for the scoreboard", t);
        }

        // Nothing here is cheap: claiming the elements, the rewrites and the ordering run over
        // every sidebar line and every one of the tab list's rows, and this used to happen twenty
        // times a second whether or not a character had changed. The rows are built when one of the
        // inputs has actually moved - the sidebar's own lines, the tab list's content, the players,
        // or a setting - and kept otherwise.
        int fingerprint = rowFingerprint(sidebar, tabStamp,
                players.size(), SweatsEnhancements.cfg == null ? 0 : SweatsEnhancements.cfg.version());
        if (fingerprint == lastFingerprint && !rows.isEmpty()) {
            buildsSkipped++;
            return;
        }
        lastFingerprint = fingerprint;

        List<PanelLines.Row> built = PanelLines.build(sidebar, tabRaw, tabClean, players);
        lastBuilt = built;

        // What the server sent that no element claimed. Worth saying out loud once: a line this
        // mod does not know is a line the mod cannot format, and the player is the only one who
        // can report it.
        List<String> unknown = new ArrayList<String>();
        for (PanelLines.Row row : built) {
            if (!PanelSlots.EXTRA.equals(row.slot)) continue;
            String clean = EnumChatFormatting.getTextWithoutFormattingCodes(row.text);
            if (clean != null && !clean.trim().isEmpty()) unknown.add(clean.trim());
        }
        unknownCount = unknown.size();
        unknownSample = unknown;
        if (!unknown.isEmpty() && flag(UNKNOWN_WARNING_KEY, true) && ticksOn > 60) {
            StringBuilder sample = new StringBuilder();
            for (int i = 0; i < unknown.size() && i < 3; i++) {
                if (sample.length() > 0) sample.append(" \u00a78| \u00a7f");
                sample.append(unknown.get(i));
            }
            Report.noteOnce("scoreboard.unknown",
                    "Custom Scoreboard does not recognise " + String.valueOf(unknown.size())
                    + " of this sidebar's lines, which are drawn at the end: \u00a77" + sample);
        }

        int max = setting(MAX_LINES_KEY, VANILLA_MAX_LINES);
        if (max < 1) max = 1;

        rows = new ArrayList<Row>();
        for (PanelLines.Row row : built) {
            if (rows.size() >= max) break;
            rows.add(new Row(row.text, row.points));
        }

        // The section that listed the unrecognised lines at the end of the panel is gone. It drew
        // every one of them a second time - a line the panel cannot place is already drawn where the
        // server put it - and the player saw exactly that: the slayer block, then the same two lines
        // again under the unknown heading. The chat warning above still names them, which is what it
        // is for.
        rowsDrawn = rows.size();
    }

    /**
     * Draws the panel.
     *
     * The layout settings from the settings page all land here: the scale, the space between the
     * lines, where the panel sits (vertical and horizontal alignment, or the free move position),
     * which text alignment the lines use inside it, whether the text has a shadow, and the
     * background's colour and darkness. The title and the footer have their own alignments and
     * their own text, written in the mini-markup RichText understands.
     */
    private void draw() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.gameSettings == null) return;
        if (mc.gameSettings.showDebugInfo) {
            why = "the F3 screen is up";
            return;
        }
        if (mc.fontRendererObj == null) return;
        if (objective == null) {
            why = "the server has no sidebar objective";
            return;
        }
        if (!skyBlockTitle(title)) {
            why = "waiting for SkyBlock";
            return;
        }
        if (flag(HIDE_WHILE_CHAT_KEY, false) && mc.currentScreen instanceof GuiChat) {
            why = "the chat is open, and Hide While Chat Open is on";
            return;
        }
        if (flag(HIDE_WHILE_TAB_KEY, false) && mc.gameSettings.keyBindPlayerList != null
                && mc.gameSettings.keyBindPlayerList.isKeyDown()) {
            why = "the tab list is open, and Hide While Tablist Open is on";
            return;
        }

        FontRenderer fr = mc.fontRendererObj;
        ScaledResolution sr = new ScaledResolution(mc);

        List<Row> visible = rows;
        String heading = heading();
        if (visible.isEmpty() && heading.isEmpty()) return;

        boolean shadow = flag(SHADOW_KEY, true);
        boolean numbers = flag(NUMBERS_KEY, false);
        String footerText = footer();

        int spacing = setting(SPACING_KEY, 10);
        if (spacing < 9) spacing = 9;
        if (spacing > 20) spacing = 20;

        int textAlign = alignment(TEXT_ALIGN_KEY, 0);      // 0 start, 1 centre, 2 end
        int width = fr.getStringWidth(heading);
        int body = 0;
        int padding = 0;
        for (Row row : visible) {
            int w = fr.getStringWidth(row.text);
            if (numbers) w += fr.getStringWidth(" " + row.points) + 2;
            body = Math.max(body, w);
        }
        width = Math.max(width, body);
        if (!footerText.isEmpty()) width = Math.max(width, fr.getStringWidth(footerText));

        // "Background Border Size": how far the background reaches past the text.
        int border = setting(BORDER_KEY, 5);
        if (border < 0) border = 0;
        if (border > 20) border = 20;
        int panel = width + border * 2;
        int contentLeft = border;
        int screenWidth = sr.getScaledWidth();
        int screenHeight = sr.getScaledHeight();

        // the reference's Margin: "space between the border of your screen and the scoreboard",
        // which is the distance the panel keeps from the edge it is anchored to
        int margin = setting(MARGIN_KEY, 0);
        if (margin < 0) margin = 0;
        if (margin > 50) margin = 50;
        int edge = 3 + margin;

        int left;
        String horizontal = setting(HORIZONTAL_KEY, "Right");
        if ("Left".equalsIgnoreCase(horizontal)) left = edge;
        else if ("Center".equalsIgnoreCase(horizontal)) left = screenWidth / 2 - panel / 2;
        else if ("Free Move".equalsIgnoreCase(horizontal)) {
            left = screenWidth / 2 - panel / 2 + setting(FREE_X_KEY, 0);
        } else left = screenWidth - edge - panel;
        // A stored free-move position can be anything, and a panel parked past the edge draws
        // nothing while still counting as drawn - so the client's sidebar is gone and the player
        // has neither. Whatever the settings say, the panel is pulled back into the screen.
        if (left < 3) left = 3;
        if (left + panel > screenWidth - 3) left = Math.max(3, screenWidth - 3 - panel);

        int lines = visible.size() + (footerText.isEmpty() ? 0 : 1);
        int bodyHeight = lines * spacing;
        int firstY;
        String vertical = setting(VERTICAL_KEY, "Center");
        if ("Top".equalsIgnoreCase(vertical)) firstY = 4 + margin;
        else if ("Bottom".equalsIgnoreCase(vertical)) firstY = screenHeight - 4 - margin - bodyHeight;
        else if ("Free Move".equalsIgnoreCase(vertical)) {
            firstY = screenHeight / 2 - bodyHeight / 2 + setting(FREE_Y_KEY, 0);
        } else firstY = screenHeight / 2 - bodyHeight / 2 + bodyHeight / 3;
        if (firstY < 3) firstY = 3;
        if (firstY + bodyHeight > screenHeight - 3) {
            firstY = Math.max(3, screenHeight - 3 - bodyHeight);
        }

        int headroom = heading.isEmpty() ? 0 : spacing;
        // the box reaches past the text by the border size: the padding is inside the background,
        // which is what makes the reference's panel look like a box around the lines rather than a
        // rectangle that happens to be behind them
        int top = firstY - headroom - border;
        int bottom = firstY + bodyHeight + border;
        int height = bottom - top;

        int alpha = setting(OPACITY_KEY, 110);
        if (alpha < 0) alpha = 0;
        if (alpha > 255) alpha = 255;
        int background = colourOf(setting(BACKGROUND_KEY, "000000"));

        int percent = setting(SCALE_KEY, 100);
        // 0.9.1's generator wrote the 1.0 default into the file as the whole number 1, which reads
        // back as one percent, and a panel drawn at a quarter size at the screen edge is a panel the
        // player reports as "vanished". A value that small is that old default, not a choice.
        if (percent <= 1) percent = 100;
        if (percent < 25) percent = 25;
        if (percent > 200) percent = 200;
        float scale = percent / 100.0F;

        GL11.glPushMatrix();
        try {
            if (scale != 1.0F) {
                // shrink around the panel's own top corner, so it stays where the player put it
                GL11.glTranslatef(left, top, 0.0F);
                GL11.glScalef(scale, scale, 1.0F);
                GL11.glTranslatef(-left, -top, 0.0F);
            }

            int radius = setting(ROUND_KEY, 10);
            if (radius < 0) radius = 0;
            if (radius > 30) radius = 30;
            if (flag(OUTLINE_KEY, false)) {
                int thickness = setting(OUTLINE_SIZE_KEY, 5);
                if (thickness < 1) thickness = 1;
                if (thickness > 15) thickness = 15;
                outlineRect(left - thickness, top - thickness, left + panel + thickness,
                        bottom + thickness, thickness,
                        colourOf(setting(OUTLINE_TOP_KEY, "AF59FF")),
                        colourOf(setting(OUTLINE_BOTTOM_KEY, "7FEDFF")));
            }
            boolean drawBackground = flag(BACKGROUND_ON_KEY, true);
            if (drawBackground && alpha > 0) {
                roundedRect(left, top, left + panel, bottom, radius, (alpha << 24) | background);
            }

            int textLeft = left + contentLeft;
            int y = firstY;
            if (!heading.isEmpty()) {
                draw(fr, heading, align(fr, heading, left, panel, alignment(TITLE_ALIGN_KEY, 1)), top + 1, shadow);
                y = firstY;
            }
            for (Row row : visible) {
                if (row.text.isEmpty()) {
                    y += spacing;
                    continue;
                }
                int x = align(fr, row.text, textLeft, panel - contentLeft - padding, textAlign);
                draw(fr, row.text, x, y, shadow);
                if (numbers) {
                    String points = "\u00a7c" + row.points;
                    draw(fr, points, left + panel - 2 - fr.getStringWidth(points), y, shadow);
                }
                y += spacing;
            }
            if (!footerText.isEmpty()) {
                draw(fr, footerText, align(fr, footerText, left, panel, alignment(FOOTER_ALIGN_KEY, 1)), y, shadow);
            }
        } finally {
            GL11.glPopMatrix();
        }

        boxLeft = left;
        boxTop = top;
        // Editor hitbox is the visible (scaled) box, not its unscaled layout size.
        boxWidth = Math.max(1, Math.round(panel * scale));
        boxHeight = Math.max(1, Math.round(height * scale));
        boxFirstY = firstY;
        boxBodyHeight = bodyHeight;
        boxBorder = border;
        boxSpacing = spacing;
        drawFrames++;
        rowsDrawn = visible.size();
        hiddenWithoutDrawing = 0;
        why = "";
    }

    /**
     * A filled rectangle with rounded corners, drawn a row at a time.
     *
     * The reference draws its background as a rounded rectangle of the same shape: the corners are
     * cut by a circle of `radius`, so the top and bottom rows are inset the most.
     */
    static void roundedRect(int x1, int y1, int x2, int y2, int radius, int colour) {
        if (radius <= 1 || x2 - x1 <= radius * 2 || y2 - y1 <= radius * 2) {
            drawRect(x1, y1, x2, y2, colour);
            return;
        }
        for (int y = y1; y < y2; y++) {
            int inset;
            if (y == y1 + radius) {
                drawRect(x1, y, x2, y2 - radius, colour);
                y = y2 - radius - 1;
                continue;
            }
            if (y < y1 + radius) inset = cornerInset(y1 + radius - 1 - y, radius);
            else inset = cornerInset(y - (y2 - radius), radius);
            drawRect(x1 + inset, y, x2 - inset, y + 1, colour);
        }
    }

    /** How far a row is pulled in at the corner, `dy` rows into it. */
    private static int cornerInset(int dy, int radius) {
        int d = radius - (int) Math.sqrt(Math.max(0, radius * radius - dy * dy));
        return Math.max(0, d);
    }

    /**
     * An outline of `thickness` rings around a box, blended from the top colour to the bottom one
     * down its height and fading outwards - the reference's "Background Outline" with its blur.
     */
    static void outlineRect(int x1, int y1, int x2, int y2, int thickness, int top, int bottom) {
        int height = Math.max(1, y2 - y1);
        for (int i = 0; i < thickness; i++) {
            int alpha = 255 - (int) (190.0F * i / Math.max(1, thickness));
            for (int y = y1 + i; y < y2 - i; y++) {
                int colour = blend(top, bottom, (y - y1) / (float) height);
                drawRect(x1 + i, y, x1 + i + 1, y + 1, (alpha << 24) | colour);
                drawRect(x2 - i - 1, y, x2 - i, y + 1, (alpha << 24) | colour);
            }
            drawRect(x1 + i, y1 + i, x2 - i, y1 + i + 1, (alpha << 24) | blend(top, bottom, 0.0F));
            drawRect(x1 + i, y2 - i - 1, x2 - i, y2 - i, (alpha << 24) | blend(top, bottom, 1.0F));
        }
    }

    /** One colour part way towards another. */
    private static int blend(int from, int to, float t) {
        if (t < 0.0F) t = 0.0F;
        if (t > 1.0F) t = 1.0F;
        int r = (int) (((from >> 16) & 0xFF) + ((((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t));
        int g = (int) (((from >> 8) & 0xFF) + ((((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t));
        int b = (int) ((from & 0xFF) + (((to & 0xFF) - (from & 0xFF)) * t));
        return (r << 16) | (g << 8) | b;
    }

    /** One line of text, with or without the shadow the settings page asks for. */
    private static void draw(FontRenderer fr, String text, int x, int y, boolean shadow) {
        if (shadow) fr.drawStringWithShadow(text, x, y, 0xFFFFFF);
        else fr.drawString(text, x, y, 0xFFFFFF);
    }

    /** Where a line sits inside the panel for the alignment the player chose. */
    private static int align(FontRenderer fr, String text, int left, int width, int textAlign) {
        if (textAlign == 1) return left + width / 2 - fr.getStringWidth(text) / 2;
        if (textAlign == 2) return left + width - fr.getStringWidth(text) - 2;
        return left;
    }

    private static int panelPadding(int textAlign, int extra) {
        return textAlign == 0 ? 2 + extra : extra;
    }

    /** "START" / "CENTER" / "END" / "Left" / "Right" as an index. */
    private static int alignment(String key, int def) {
        String value = setting(key, "");
        if (value.isEmpty()) return def;
        String lower = value.toLowerCase(java.util.Locale.ROOT);
        if (lower.startsWith("c")) return 1;
        if (lower.startsWith("e") || lower.startsWith("r")) return 2;
        if (lower.startsWith("s") || lower.startsWith("l")) return 0;
        return def;
    }

    /** The heading over the panel: the custom text if it is on, otherwise the server's own title. */
    private String heading() {
        if (flag(TITLE_CUSTOM_KEY, false)) {
            if (flag(TITLE_ANYWHERE_KEY, false) || inSkyBlock()) {
                return RichText.render(configTitle());
            }
        }
        if (!flag(SERVER_TITLE_KEY, true)) return "";
        return RichText.strip(title);
    }

    /** The line under the panel: the custom footer, or the clock and tick rate. */
    private static String footer() {
        if (flag(FOOTER_CUSTOM_KEY, false)) {
            String custom = RichText.render(setting(FOOTER_TEXT_KEY, ""));
            if (!custom.isEmpty()) return custom;
        }
        if (!flag(FOOTER_KEY, true)) return "";
        return "\u00a77" + CLOCK.format(new Date());
    }

    private static String configTitle() {
        return setting(TITLE_KEY, "");
    }

    /** The server calls its lobby sidebar FAKEPIXEL and its game sidebar SKYBLOCK. */
    public static boolean skyBlockTitle(String heading) {
        String plain = RichText.strip(heading == null ? "" : heading);
        return plain != null && plain.toLowerCase(java.util.Locale.ROOT).contains("skyblock");
    }

    private static boolean inSkyBlock() {
        return SweatsEnhancements.scoreboard != null && skyBlockTitle(SweatsEnhancements.scoreboard.title);
    }

    /** A hex colour out of the config, with the usual fallback rather than a crash. */
    private static int colourOf(String hex) {
        try {
            String clean = hex == null ? "" : hex.trim().replace("#", "").replace("0x", "");
            if (clean.length() != 6) return 0x000000;
            return (int) Long.parseLong(clean, 16);
        } catch (Throwable t) {
            return 0x000000;
        }
    }

    /**
     * True when a line should not be drawn: blank, or containing one of the hidden phrases.
     *
     * Blank lines are always dropped - a sidebar full of gaps is the thing this feature exists to
     * clean up. The phrases are a comma-separated list, matched without colour codes and without
     * case, and the default list is empty because the server's own lines are worth keeping.
     */
    private boolean hidden(String raw) {
        String clean = EnumChatFormatting.getTextWithoutFormattingCodes(raw);
        if (clean == null || clean.trim().isEmpty()) return true;
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

    /** Clears the counters behind /se panels, so a report starts from zero. */
    public static void resetDiagnostics() {
        framesPre = 0;
        framesPost = 0;
        framesText = 0;
        hidFrames = 0;
        drawFrames = 0;
        ticksOn = 0;
        hiddenWithoutDrawing = 0;
        unknownCount = 0;
        lastBuilt = new ArrayList<PanelLines.Row>();
        unknownSample = new ArrayList<String>();
        why = "not asked yet";
    }

    // ---------------------------------------------------------------- for the probe dump and /se panels

    /** The title of the sidebar as this class sees it, so /se dump still shows one. */
    public static String probeTitle() {
        return SweatsEnhancements.scoreboard == null ? "" : SweatsEnhancements.scoreboard.title;
    }

    /** The sidebar's lines, formatted, in the order they are drawn. */
    public static List<String> probeLines() {
        List<String> out = new ArrayList<String>();
        if (SweatsEnhancements.scoreboard == null) return out;
        for (Row row : SweatsEnhancements.scoreboard.rows) {
            out.add("   [" + row.points + "] " + row.text);
        }
        return out;
    }

    /**
     * Every line the panel builds, with the element it came from: "SLOT  text".
     *
     * This is what /se elements prints, and it is the answer to "what is the panel actually
     * showing" - an element with no data behind it simply does not appear in this list, so a
     * missing line can be told from a line that was drawn wrong.
     */
    public static List<String> probeElements() {
        List<String> out = new ArrayList<String>();
        for (PanelLines.Row row : lastBuilt) {
            out.add(row.slot + "\u0001" + EnumChatFormatting.getTextWithoutFormattingCodes(row.text));
        }
        return out;
    }

    /**
     * The text of the lines the panel is drawing, for the HUD editor's preview.
     *
     * The editor draws a scoreboard-shaped rectangle and needs to know how big it is; the row text
     * is what makes the preview recognisable, so the element the player grabs looks like the panel
     * rather than like a blank box.
     */
    public static List<String> previewLines() {
        List<String> out = new ArrayList<String>();
        List<Row> rows = SweatsEnhancements.scoreboard == null ? null : SweatsEnhancements.scoreboard.rows;
        if (rows != null) {
            for (Row row : rows) {
                String text = EnumChatFormatting.getTextWithoutFormattingCodes(row.text);
                out.add(text == null ? "" : text);
            }
        }
        if (out.isEmpty()) {
            String heading = EnumChatFormatting.getTextWithoutFormattingCodes(probeHeading());
            if (heading != null && !heading.trim().isEmpty()) out.add(heading);
            out.add("Purse: 0");
            out.add("Bank: 0");
            out.add("Gems: 0");
        }
        return out;
    }

    /** The panel's own title, with its colour codes kept, for the editor's preview. */
    private static String probeHeading() {
        String heading = "SkyBlock";
        try {
            String custom = configTitle();
            if (!custom.trim().isEmpty()) heading = custom;
        } catch (Throwable t) {
            // a preview is not worth a stack trace
        }
        return heading;
    }

    /**
     * Moves the panel to a screen position, the way the HUD editor's drag does.
     *
     * The panel's position is stored as an offset from the middle of the screen - the same two
     * numbers the Free Move alignment and Free X/Free Y settings use - and the editor works in
     * screen pixels, so the two are converted here rather than in the editor. The target is the
     * panel's box, not its first line: the box is what the player sees and grabs.
     */
    public static void moveTo(int left, int top) {
        try {
            if (SweatsEnhancements.cfg == null) return;
            int screenWidth = screenWidth();
            int screenHeight = screenHeight();
            int width = boxWidth > 0 ? boxWidth : 130;
            int body = boxBodyHeight > 0 ? boxBodyHeight : 120;
            // the offsets the panel's own maths would apply right now
            int logicalWidth = Math.round(width * 100f / HudLayout.scalePercent(HudLayout.byKey("scoreboard")));
            int freeX = boxLeft - (screenWidth / 2 - logicalWidth / 2);
            int freeY = boxFirstY - (screenHeight / 2 - boxBodyHeight / 2);
            // then the move the player just made, measured on the box
            freeX += left - boxLeft;
            freeY += top - boxTop;
            if (boxWidth <= 0) freeX = left - (screenWidth / 2 - width / 2);
            if (boxBodyHeight <= 0) freeY = top - (screenHeight / 2 - body / 2);
            SweatsEnhancements.cfg.set(HORIZONTAL_KEY, "Free Move");
            SweatsEnhancements.cfg.set(VERTICAL_KEY, "Free Move");
            SweatsEnhancements.cfg.setInt(FREE_X_KEY, freeX);
            SweatsEnhancements.cfg.setInt(FREE_Y_KEY, freeY);
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] could not move the scoreboard: " + t);
        }
    }

    /** Puts the panel back where it draws by default: against the right edge, upright centre. */
    public static void resetPosition() {
        try {
            if (SweatsEnhancements.cfg == null) return;
            SweatsEnhancements.cfg.set(HORIZONTAL_KEY, "Right");
            SweatsEnhancements.cfg.set(VERTICAL_KEY, "Center");
            SweatsEnhancements.cfg.remove(FREE_X_KEY);
            SweatsEnhancements.cfg.remove(FREE_Y_KEY);
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] could not reset the scoreboard's position: " + t);
        }
    }

    /** The screen size the panel is drawing into, or a plain 1920x1080 before the first frame. */
    private static int screenWidth() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null) return 1920;
            return new net.minecraft.client.gui.ScaledResolution(mc).getScaledWidth();
        } catch (Throwable t) {
            return 1920;
        }
    }

    private static int screenHeight() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null) return 1080;
            return new net.minecraft.client.gui.ScaledResolution(mc).getScaledHeight();
        } catch (Throwable t) {
            return 1080;
        }
    }

    /** How many lines the panel built, elements and all. */
    public static int probeBuilt() {
        return lastBuilt.size();
    }

    /** The server's lines in the server's own order, before anything was claimed. */
    public static List<String> probeRaw() {
        List<String> out = new ArrayList<String>();
        if (SweatsEnhancements.scoreboard == null) return out;
        for (Row row : SweatsEnhancements.scoreboard.raw) {
            out.add(row.text);
        }
        return out;
    }

    /** What one slot claimed, for /se sidebar. */
    public static List<String> probeSlot(String slot) {
        return SidebarClaims.slotLines(probeRaw(), slot);
    }

    /** The slot names in the order they are drawn, for /se sidebar. */
    public static List<String> probeOrder() {
        return order();
    }

    /** The number of lines the server sent. */
    public static int probeRawCount() {
        return probeRaw().size();
    }

    /** The settings the panel is actually drawing with, for /se panels. */
    public static String probeSettings() {
        try {
            int percent = setting(SCALE_KEY, 100);
            if (percent <= 1) percent = 100;
            if (percent < 25) percent = 25;
            if (percent > 200) percent = 200;
            return "scale " + percent + "%, background " + setting(OPACITY_KEY, 110)
                    + ", max " + setting(MAX_LINES_KEY, VANILLA_MAX_LINES) + " lines"
                    + ", spacing " + setting(SPACING_KEY, 10) + "px"
                    + ", " + setting(VERTICAL_KEY, "Center") + "/" + setting(HORIZONTAL_KEY, "Right")
                    + " aligned, shadow " + flag(SHADOW_KEY, true)
                    + ", client sidebar " + (flag(HIDE_VANILLA_KEY, true) ? "replaced" : "kept")
                    + ", custom lines " + flag("scoreboard.customLines", false)
                    + ", unknown lines " + unknownCount
                    + ", drew " + drawFrames + " frames"
                    + ", hid " + hidFrames + " frames"
                    + ", panel at " + boxLeft + "," + boxTop + " " + boxWidth + "x" + boxHeight;
        } catch (Throwable t) {
            return "settings unreadable: " + t;
        }
    }
    /** The objective name as this class sees it, for /se panels. */
    public static String probeObjective() {
        return SweatsEnhancements.scoreboard == null ? "" : SweatsEnhancements.scoreboard.objectiveName;
    }

    /** Which display slot the sidebar was read from, for /se panels. */
    public static int probeSlot() {
        return SweatsEnhancements.scoreboard == null ? SIDEBAR_SLOT : SweatsEnhancements.scoreboard.objectiveSlot;
    }

    /** How many lines were read last, for /se panels. */
    public static int probeRead() {
        return SweatsEnhancements.scoreboard == null ? 0 : SweatsEnhancements.scoreboard.rowsRead;
    }

    /** How many lines were drawn last, for /se panels. */
    public static int probeDrawn() {
        return SweatsEnhancements.scoreboard == null ? 0 : SweatsEnhancements.scoreboard.rowsDrawn;
    }
}
