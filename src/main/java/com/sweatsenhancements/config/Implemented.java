package com.sweatsenhancements.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * The features that are actually built and wired up right now.
 *
 * Everything else in Features.java is listed in the config GUI but marked "planned", so the
 * GUI never claims something works when it does not. Add an id here when its code lands.
 *
 * Removed at the user's request: CH008 (0.6.3), then CH012 and MS009/MS010 (0.7.0), then CH030
 * (0.7.1 - Fakepixel has no prototype lobby, loot boxes were never a nuisance, and it was not
 * catching the lobby join/leave messages it was written for), then MS112 "Hide Useless Armor
 * Stands" (0.8.0 - it hid Hoppity's Hunt eggs, which are nameless armour stands like every other
 * hologram). CH012 was a
 * misport - the reference sack feature shows its own message, which Fakepixel never sends, so there
 * was nothing to hide. The potion readout was dropped because the player already has one.
 *
 * CH008 (Compact Potion Effect Messages) was removed in 0.6.3. It had been listed here since
 * 0.3.0 without any code behind it - a switch that did nothing, which is exactly what this file
 * is supposed to prevent. The user's call to drop it, and the count is honest again.
 */
public final class Implemented {

    private Implemented() {
    }

    public static final Set<String> IDS = new HashSet<String>(Arrays.asList(
            // --- chat: filters and rewrites, all instant to verify in game
            "CH033",   // Welcome message when entering SkyBlock
            "CH014",   // Hide fire sale reminder messages
            "CH031",   // Hiding Empty messages
            "CH034",   // Powder Mining messages hider
            "CH200",   // Copy Chat Message
            "CH201",   // Watchdog/info messages
            "CH202",   // Friend join/leave messages
            "CH203",   // Zombie rare drops
            "CH204",   // Selling ranks messages
            "CH205",   // Game invites
            "CH206",   // Server announcements

            // --- misc: HUD elements
            "MS028",   // TPS Display
            "MS200",   // Fullbright, reversible brightness
            "MS201",   // Current Pet HUD
            "MS003",   // Hide ground XP bottles and orbs
            "MS204",   // Always Sprint
            "MS205",   // Hide Falling Blocks
            "MS206",   // Mute Teleport Item Sound
            "MS207",   // Disable Rain Rendering
            "MS208",   // Wardrobe Shortcut
            "MS209",   // Equipment Shortcut
            "MS210",   // Wardrobe Slot Shortcuts
            "MS211",   // Warp Shortcuts
            "MS212",   // Jerry-chine sounds
            "MS213",   // SkyBlock menu middle-click
            "MS214",   // Ability item placement protection
            "MS215",   // Zombie rare-drop chime
            "MS216",   // Unclaimed contests
            "MS217",   // Flaming Fists cosmetic
            "MS218",   // SweatsEnhancements-only internal item ID tooltip
            "MS219",   // Stacking counter tooltip
            "MS220",   // Admin-item tooltip heuristic and reason
            "MS221",   // Active Wardrobe Slot highlight
            "MS222",   // Protect unique item identity
            "MS223",   // Smooth confirmed teleport view
            "MS224",   // Close only Secret Chest
            "MS225",   // Cyan block selection outline
            "MS226",   // Local CPS HUD
            "MS227",   // Disable hurt camera shake
            "MS228",   // Blank decorative pane tooltips

            // --- misc: the two on-screen panels, drawn by SweatsEnhancements instead of by the client
            "MS063",   // Compact Tab List
            "MS085",   // Custom Scoreboard

            // --- misc: entity rendering
            "MS027",   // Hide dead entities
            "MS007",   // Hide damage splashes (numbered armour stands)
            // --- fishing
            "FI030",   // Fishing bobber timer
            "SL053", "SL054", "SL055", // independently opt-in slayer essentials
            "SL056", "SL057", "SL058", "SL059", "SL060", "SL061", "SL062",
            "SL063", "SL064", "SL065", "SL066", "SL067", "SL068", "SL069", "SL070", // NEF Slayer family
            "DU001",   // Dungeon run timer, clear progress and completion
            "DU002",   // Manual requeue of last observed floor and mode
            "DU003",   // Restore GUI cursor position
            "DU004",   // Label clicked dungeon chests and levers
            "DU005",   // Dungeon blessing HUD from tab footer
            "DU006", "DU007", "DU008", // starred mobs, dropped keys, wither doors
            "DU009", "DU010", "DU011", // blood mobs, elapsed timer, ready alert
            "DU012", "DU013", // fel and bat dungeon boxes
            "DU014", "DU016", "DU020", // F6 timer, map HUD, score HUD
            "DU017", "DU018", "DU019", "DU021", "DU022", "DU023", "DU024", "DU025", "DU026", "DU027", "DU028", "DU030", "DU031", "DU032", "DU033", "DU034", "DU035", "DU036" // dungeon QoL
    ));

    /**
     * Features that work but should start switched off, because they change how the game looks
     * rather than cleaning something up. The menu shows these off; the player turns them on.
     */
    public static final Set<String> DEFAULT_OFF = new HashSet<String>(Arrays.asList(
            "DU001",   // independent dungeon HUD, opt in
            "DU003", "DU004", "DU005", // opt-in dungeon visual features
            "DU006", "DU007", "DU008", "DU009", "DU010", "DU011", "DU012", "DU013", "DU014", "DU016", "DU020", "DU017", "DU018", "DU019", "DU021", "DU022", "DU023", "DU024", "DU025", "DU026", "DU027", "DU028", "DU030", "DU031", "DU032", "DU033", "DU034", "DU035", "DU036",
            "MS063",   // replaces the client's tab list
            "MS085",   // replaces the client's scoreboard
            "MS200",   // changes brightness while enabled
            "MS201",   // new HUD element, opt in
            "MS003",   // hide dropped XP, opt in
            "MS204",   // always sprint, opt in
            "MS205",   // falling-block visual filter, opt in
            "MS206",   // teleport item sound, opt in
            "MS207",   // rain visual change, opt in
            "MS208",   // wardrobe shortcut, opt in
            "MS209",   // equipment shortcut, opt in
            "MS210",   // wardrobe slot keys, opt in
            "MS211",   // warp keys, opt in
            "CH200",   // copy chat buttons, opt in
            "CH201",   // chat filters, opt in
            "CH202",
            "CH203",
            "CH204",
            "CH205",
            "CH206",  // server announcements, opt in
            "MS212",  // sound and interaction tweaks, opt in
            "MS213",
            "MS214",
            "MS215",
            "MS216", // next visual/item-data batch, opt in
            "MS217",
            "MS218",
            "MS219",
            "MS220",
            "MS221", // active wardrobe slot, opt in
            "MS222", // exact-item protection, opt in
            "MS223", // visual-only teleport smoothing, opt in
            "MS224", // exact Secret Chest auto-close
            "MS225", // block outline
            "MS226", // local CPS HUD
            "MS227", // no hurt camera
            "MS228", // hide blank decorative pane tooltips
            "SL053", "SL054", "SL055", "SL056", "SL057", "SL058", "SL059",
            "SL060", "SL061", "SL062", "SL063", "SL064", "SL065", "SL066", "SL067", "SL068", "SL069", "SL070" // opt-in slayer aids
    ));
}
