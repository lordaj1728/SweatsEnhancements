package com.sweatsenhancements.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The names of the scoreboard's elements, and which of the server's lines each one claims.
 *
 * This is the list behind the Appearance setting: every element has a name the player can see,
 * order and switch off, and a pattern that finds it in whatever the server sent. Two rules, both
 * learned from the way this server writes its panels:
 *
 *   1. A slot that finds nothing is simply absent. Nothing is invented and no gap is left where a
 *      line "should" have been - which is why every element here works on a line the server
 *      actually sent, and why an element the server never writes costs nothing here.
 *   2. Nothing is ever dropped for being unrecognised. A sidebar line no slot claims keeps its
 *      place in EXTRA. The only lines this class discards are blank ones.
 *
 * The elements a tab list can fill are listed separately, because on this server the tab list is
 * where the useful numbers live: Bank, Interest, Gems, SkyBlock level and the stat block are tab
 * lines, not sidebar lines. That is why "Select Tablist Lines" exists as its own setting.
 *
 * No Minecraft imports: plain text in, slot names out.
 */
public final class PanelSlots {

    /** the slot that keeps every sidebar line nothing else claimed */
    public static final String EXTRA = "EXTRA";

    /**
     * Every element this panel draws, in the order that reads best by default: the top of the panel
     * says when and where you are, then your money, then your progression, then what is happening
     * around you. The order is the player's to change - this is only where the list starts.
     *
     * The player cut this list down themselves: *"remove the area,server,player,motes,cold,soulflow,
     * flintarrow,skills,stats,pet,party,active effect,website and extra from every list as either
     * they do not do anything,not in the game or i dont need them in there"*. Everything they named
     * is gone from here rather than only from the config page, because this list is what the panel
     * will draw: a row that used to land in one of those slots - a skills block, the pet, the
     * island's roster, the effects list - is now not drawn at all, wherever the server puts it.
     */
    public static final List<String> ALL = Collections.unmodifiableList(Arrays.asList(
            "SERVER_DATE", "TIME", "SEASON", "ISLAND", "LOCATION", "DUNGEON_SIDEBAR", "PROFILE", "SB_LEVEL",
            "PURSE", "BANK", "INTEREST", "BITS", "GEMS", "COPPER", "HEAT", "NORTH_STARS",
            "POWDER", "FORGES", "COOKIE", "POWER", "TUNING", "ELECTIONS", "EVENT", "SLAYER"));

    /**
     * The blank line, which the reference has as a draggable entry too.
     *
     * It is not in the default order: this server sends its own blank lines between its blocks and
     * they are kept, so adding more by default would double them. It is in the list, so a player who
     * wants a gap where the server has none can drag one in.
     */
    public static final String EMPTY_LINE = "EMPTY_LINE";

    /** the elements that are read out of the tab list rather than out of the sidebar */
    public static final List<String> TAB_ELEMENTS = Collections.unmodifiableList(Arrays.asList(
            "PROFILE", "SB_LEVEL", "BANK", "INTEREST", "GEMS", "POWER", "ELECTIONS", "EVENT",
            "POWDER", "FORGES", "FAIRY_SOULS", "COPPER", "HEAT", "NORTH_STARS"));

    /**
     * What is ticked out of the box.
     *
     * This server keeps the figures a player actually watches - the bank, interest, gems, the
     * SkyBlock level, the skill and stat blocks, the pet, the election - in the tab list rather
     * than in the sidebar, so without these the panel would show the sidebar's handful of lines and
     * nothing else. They are ordered the way the panel draws them (see ALL above), and each one can
     * be unticked in Select Tablist Lines.
     */
    public static final List<String> DEFAULT_TAB_LINES = Collections.unmodifiableList(Arrays.asList(
            "BANK", "INTEREST", "GEMS", "POWER", "SB_LEVEL", "PROFILE", "POWDER", "FORGES",
            "EVENT", "ELECTIONS"));

    /**
     * The currencies the Chunky Stats list offers, in the order the add menu shows them.
     *
     * The list starts empty: the panel draws one currency a line until the player puts some in it,
     * which is what the panel looked like before the list existed.
     */
    public static final List<String> DEFAULT_CHUNKED = Collections.emptyList();

    /** the default Appearance order, which is the order this panel draws in */
    public static final List<String> DEFAULT_APPEARANCE = ALL;

    /**
     * The order events are shown in when more than one is up. This server names its own events,
     * so this list is a preference, not a whitelist: an event that is not on it is still shown,
     * after the ones that are.
     */
    public static final List<String> DEFAULT_EVENTS_PRIORITY = Collections.unmodifiableList(Arrays.asList(
            "Farming Contest", "Mining Fiesta", "Fishing Festival", "Mythological Ritual",
            "Spooky Festival", "Season of Jerry", "New Year Celebration", "Traveling Zoo",
            "Hoppity's Hunt", "Better Together", "Election Booth Opens"));

    /** "09/22/26 hub-4", "09/23/26 hub-3" - the date and server id line the sidebar opens with */
    private static final Pattern SERVER_DATE = Pattern.compile("\\s*\\d{2}/\\d{2}/\\d{2}.*");

    /** "Late Spring 11th", "Summer 30th", "Early Autumn 3rd" */
    private static final Pattern SEASON = Pattern.compile(
            "(Early|Late|Mid)?\\s*(Spring|Summer|Autumn|Winter)\\s+\\d{1,2}(st|nd|rd|th)?.*",
            Pattern.CASE_INSENSITIVE);

    /** "4:00pm", "7:10pm" - whatever sun or moon symbol follows does not matter */
    private static final Pattern TIME = Pattern.compile("\\d{1,2}:\\d{2}\\s*(am|pm).*", Pattern.CASE_INSENSITIVE);

    /** the area line is marked with this symbol - every probe dump from this server carries it */
    private static final String LOCATION_SYMBOL = "\u23e3";

    /**
     * The island line's marker, whatever character the server sends for it.
     *
     * The line above the area line is a symbol and a place name (`\u32d6 Skyblock Hub` on the player's own
     * screen). The symbol is the server's to choose and this class cannot know it in advance, so the
     * line is recognised by its *shape*: a symbol, then a letter, then a name. The ranges are the ones
     * a symbol can come from - the technical, geometric, dingbat, arrow, enclosed and private-use
     * blocks, the last because a server with a resource pack draws its own glyph there.
     */
    private static boolean islandMarker(char c) {
        return (c >= '\u2300' && c <= '\u23ff')
                || (c >= '\u25a0' && c <= '\u25ff')
                || (c >= '\u2600' && c <= '\u27bf')
                || (c >= '\u2b00' && c <= '\u2bff')
                || (c >= '\u3200' && c <= '\u32ff')
                || (c >= '\u2190' && c <= '\u21ff')
                || (c >= '\ue000' && c <= '\uf8ff');
    }

    /**
     * True when a sidebar line is the island line: a symbol, then a name.
     *
     * The name has to start with a letter, which is what keeps the server's other marked lines out -
     * `\u263d 7:10pm` is a time and is claimed as one before this is asked, and a `\u270c (9/20)` count is not
     * a name. The area line is claimed first, by its own symbol, so it can never be taken for this.
     */
    private static boolean looksLikeIsland(String text) {
        if (text.isEmpty()) return false;
        char first = text.charAt(0);
        if (first < 128) return false;
        if (!islandMarker(first)) return false;
        int i = 1;
        while (i < text.length() && (text.charAt(i) == ' ' || text.charAt(i) == '\u00a0')) i++;
        return i < text.length() && Character.isLetter(text.charAt(i));
    }

    /** the header a tab list puts above a block of lines; "Skills:" is one, "Speed: 369" is not */
    private static final Pattern TAB_HEADER = Pattern.compile("^([A-Za-z][A-Za-z '\\-/]{2,24}):$");

    /**
     * A heading the server writes without a colon - "Account Info", "Server Info", "Player Stats".
     *
     * Two or more words, each starting with a capital, letters and spaces only. A player's name is
     * either one word or carries a rank in brackets, so neither can be taken for a heading. Without
     * this, "Account Info" was read as a line of the block above it and the Area, Server, Bank and
     * SkyBlock level lines that follow it never became elements of their own.
     */
    // One capitalised word is a heading too: the Dwarven Mines' blocks are written "Powders" and
    // "Forges", with no colon and nothing after them, and the two-word rule read them as data - so
    // the powder block and the forge list were never opened as blocks at all.
    private static final Pattern BARE_HEADER = Pattern.compile("^([A-Z][a-z]+(?: [A-Z][a-z]+)*)$");

    /** "Players (3)", "Players (12)" - the party block's header */
    private static final Pattern TAB_PLAYERS = Pattern.compile("(?i)^players\\s*\\(\\d+\\).*");

    private PanelSlots() {
    }

    /**
     * Which element a sidebar line belongs to, or null when nothing claims it.
     *
     * Matched against the line with its colour codes taken out, so the server may colour any of
     * this however it likes. Deliberately matched on wording and symbols rather than on a list of
     * island names: a list of names is exactly what makes another server's client do nothing here.
     */
    public static String slotOf(String cleanLine) {
        if (cleanLine == null) return null;
        String text = cleanLine.trim();
        if (text.isEmpty()) return null;

        if (text.indexOf(LOCATION_SYMBOL) >= 0) return "LOCATION";
        if (looksLikeIsland(text)) return "ISLAND";
        if (SERVER_DATE.matcher(text).matches()) return "SERVER_DATE";
        if (TIME.matcher(text).matches()) return "TIME";
        if (SEASON.matcher(text).matches()) return "SEASON";

        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.startsWith("purse:") || lower.startsWith("piggy:")) return "PURSE";
        if (lower.startsWith("bank:")) return "BANK";
        if (lower.startsWith("bits:")) return "BITS";
        if (lower.startsWith("gems:")) return "GEMS";
        if (lower.startsWith("copper:")) return "COPPER";
        if (lower.startsWith("motes:")) return "MOTES";
        if (lower.startsWith("heat:")) return "HEAT";
        if (lower.startsWith("cold:")) return "COLD";
        if (lower.startsWith("soulflow:")) return "SOULFLOW";
        if (lower.startsWith("north stars:") || lower.startsWith("north star:")) return "NORTH_STARS";
        if (lower.startsWith("powder")) return "POWDER";
        if (lower.startsWith("event:")) return "EVENT";
        if (lower.startsWith("slayer quest")) return "SLAYER";
        if (lower.startsWith("cookie") || lower.contains("booster cookie")) return "COOKIE";
        // this server calls the row after the quest it answers - "Fetchur: Flint and Steel x1" - so
        // the row is claimed by either wording and can never fall through to Everything Else
        if (lower.startsWith("flint arrow") || lower.startsWith("fetchur:")) return "FLINT_ARROW";
        if (lower.startsWith("power:")) return "POWER";
        if (lower.startsWith("tuning:")) return "TUNING";
        if (lower.contains("fakepixel") || lower.startsWith("www.") || lower.startsWith("mc.")) {
            return "WEBSITE";
        }
        return null;
    }

    /**
     * The element a tab list key belongs to: "sb level" becomes SB_LEVEL, "area" becomes AREA.
     * Keys this class has never heard of still work - they simply keep their own name.
     */
    public static String slotOfTabKey(String key) {
        if (key == null) return null;
        String name = key.trim().toLowerCase(Locale.ROOT);
        if (name.isEmpty()) return null;
        // The election is the one block whose keys are not element names. This server writes the
        // countdown as "Election: 2d 19m 39s", the winner as "Current Mayor: Paul", the minister as
        // "Minister: X" and the candidates as "<name>: |||||| (98.3%)". Every one of those used to
        // come back as an element of its own - ELECTION, SCORPIUS, CURRENT_MAYOR - none of which is
        // ever selected, so the whole block was read and then dropped. The player: "mayor stats in
        // sb doesnt work ... its in tablist but not scoreboard". The keys that mean the election
        // now say so.
        if (name.equals("election") || name.equals("elections")) return "ELECTIONS";
        if (name.contains("mayor") || name.contains("minister") || name.equals("winner")) {
            return "ELECTIONS";
        }
        if (name.equals("powder") || name.equals("powders")) return "POWDER";
        if (name.equals("forge") || name.equals("forges")) return "FORGES";
        // The Dwarven Mines' powder block: the figures in it are the powder ones, written with the
        // name of the thing that yields or boosts them - "Mithril Powder: 58,608", "Titanium Miner:
        // 73.3%", "2x Mithril Powder Collector: 6%", "Upper Mines Mithril: 88.3%", "Raffle: 0%".
        // Read as element names of their own they are MITHRIL_POWDER, TITANIUM_MINER and the rest,
        // none of which is offered, so the whole block was read and then dropped, and the panel
        // showed nothing at all in the Dwarven Mines.
        if (name.contains("powder") || name.contains("mithril") || name.contains("titanium")
                || name.contains("glacite") || name.equals("raffle")) {
            return "POWDER";
        }
        return name.toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }

    /** "|||||||| (98.3%)" - the shape this server writes a mayor candidate's share in */
    private static final Pattern ELECTION_BARS =
            Pattern.compile("\\|{2,}\\s*\\(\\s*\\d+(?:\\.\\d+)?\\s*%\\s*\\)");

    /**
     * True when a tab line's value is an election bar.
     *
     * The candidate's own name is in the key, and the names change every election: this is a fact
     * about the shape of the line, not about a list of mayors, so any candidate is claimed whatever
     * they are called - and a line that only says "Marina:" with no bar behind it is left out, the
     * same way every other line with no data is.
     */
    public static boolean electionValue(String value) {
        return value != null && ELECTION_BARS.matcher(value).find();
    }

    /** The name a slot is shown under in the settings list, e.g. LOCATION -> "Location". */
    public static String labelOf(String slot) {
        if (slot == null) return "";
        if (EXTRA.equals(slot)) return "Everything Else";
        StringBuilder out = new StringBuilder();
        for (String word : slot.split("_")) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(word.charAt(0)).append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return out.toString();
    }

    /** True when the element can be filled from the tab list. */
    public static boolean fromTab(String slot) {
        return TAB_ELEMENTS.contains(slot);
    }

    /** True when the element can be filled from the sidebar. */
    public static boolean fromSidebar(String slot) {
        switch (slot) {
            case "SERVER_DATE":
            case "TIME":
            case "SEASON":
            case "ISLAND":
            case "LOCATION":
            case "DUNGEON_SIDEBAR":
            case "PURSE":
            case "BANK":
            case "BITS":
            case "GEMS":
            case "COPPER":
            case "MOTES":
            case "HEAT":
            case "COLD":
            case "SOULFLOW":
            case "NORTH_STARS":
            case "POWDER":
            case "COOKIE":
            case "FLINT_ARROW":
            case "POWER":
            case "TUNING":
            case "EVENT":
            case "SLAYER":
            case "WEBSITE":
            case EXTRA:
                return true;
            default:
                return false;
        }
    }

    /**
     * True when the tab list line is a block header - "Skills:", "Stats:" - rather than a value.
     *
     * This is the whole of the block reader: the server writes a heading, then the lines under it,
     * and the heading is the only thing that says what the block is. Nothing is keyed to a block
     * this class recognises, so a block this server adds later still comes out as its own element.
     */
    public static String tabHeaderOf(String cleanLine) {
        if (cleanLine == null) return null;
        String text = cleanLine.trim();
        if (text.isEmpty()) return null;
        if (TAB_PLAYERS.matcher(text).find()) return "PARTY";
        if (BARE_HEADER.matcher(text).matches()) {
            // a heading this class does not know still ends the block above it; the lines under it
            // are read one by one, which is how Area, Server, Bank and the rest become elements
            return slotOfTabKey(text.toLowerCase(Locale.ROOT));
        }
        java.util.regex.Matcher m = TAB_HEADER.matcher(text);
        if (!m.matches()) return null;
        String name = m.group(1).trim().toLowerCase(Locale.ROOT);
        if (name.equals("skills")) return "SKILLS";
        if (name.equals("stats")) return "STATS";
        if (name.equals("pet")) return "PET";
        if (name.equals("election") || name.equals("elections")) return "ELECTIONS";
        if (name.equals("event") || name.equals("events")) return "EVENT";
        if (name.equals("active effects")) return "ACTIVE_EFFECTS";
        if (name.equals("powder") || name.equals("powders")) return "POWDER";
        if (name.equals("forge") || name.equals("forges")) return "FORGES";
        // a candidate's heading with no bar behind it ("Marina:") is not data: the bars rule in
        // slotOfTabKey claims the candidates that do carry their share, whatever they are called
        if (name.equals("current mayor") || name.equals("mayor") || name.equals("minister")) {
            return "ELECTIONS";
        }
        // a block this class does not know still gets a name of its own
        return slotOfTabKey(name);
    }

    /** The header line a block is drawn under: "Skills:", "Players (3)". */
    public static String blockTitle(String cleanLine) {
        return cleanLine == null ? "" : cleanLine.trim();
    }

    /** Every element name, for the settings lists and for the error message when one is unknown. */
    public static List<String> all() {
        List<String> out = new ArrayList<String>(ALL);
        return out;
    }
}
