/* NEF config/features/Slayer.java, LGPL-3.0, f80cddf6; package/class and defaults adapted.
 * Nested @Category annotations removed: SweatsEnhancements exposes each group as a root menu category. */
package com.sweatsenhancements.feature.nef.exact;

import com.google.gson.annotations.Expose;

import io.github.notenoughupdates.moulconfig.annotations.*;

public class SlayerSettings {

    // Top-Level Options
    @Expose
    @ConfigOption(name = "Faster Maddox Calling", desc = "Method for faster Maddox calling.")
    @ConfigEditorDropdown(values = {"Auto Open", "Semi Auto", "Disabled"})
    public int slayerMaddoxCalling = 2;

    @Expose
    public SlayerOverlaySettings slayerOverlaySettings = new SlayerOverlaySettings();

    @Expose
    public MinibossSettings minibossSettings = new MinibossSettings();

    @Expose
    public BossSettings bossSettings = new BossSettings();

    @Expose
    public BeaconSettings beaconSettings = new BeaconSettings();

    @Expose
    public InfernoDemonlord infernoDemonlord = new InfernoDemonlord();

    public static class SlayerOverlaySettings {

            @Expose
            @ConfigOption(name = "Slayer Overlay", desc = "Enable/Disable slayers overlay")
            @ConfigEditorBoolean
            public boolean slayerOverlay = false;

            @Expose
            @ConfigOption(name = "Position", desc = "Edit slayer overlay position.")
            @ConfigEditorButton(runnableId = 7, buttonText = "Edit")
            public String editSlayerOverlayPos = "";

            @Expose
            @ConfigOption(name = "Hide on Tab", desc = "Hide the slayer overlay when the tab list is open.")
            @ConfigEditorBoolean
            public boolean slayerOverlayHideOnTab = true;

            @Expose
            @ConfigOption(name = "Hide on Chat", desc = "Hide the slayer overlay when the chat is open.")
            @ConfigEditorBoolean
            public boolean slayerOverlayHideOnChat = true;

            @Expose
            @ConfigOption(name = "Slayer Overlay Scale", desc = "Scale of the slayer overlay text.")
            @ConfigEditorSlider(minValue = 0.5f, maxValue = 5.0f, minStep = 0.1f)
            public float slayerOverlayScale = 1.0f;

            @Expose
            @ConfigOption(name = "Slayer Overlay Background Color", desc = "Background color of the slayer overlay.")
            @ConfigEditorColour
            public String slayerOverlayBackgroundColor = "0:150:0:0:0";

            @Expose
            public Position slayerOverlayPos = new Position(0, 0, false, false);
    }

    public static class MinibossSettings {

            @Expose
            @ConfigOption(name = "Slayer Minibosses Display", desc = "Draw a box around slayer minibosses.")
            @ConfigEditorBoolean
            public boolean slayerMinibosses = false;

            @Expose
            @ConfigOption(name = "Miniboss Spawn Title", desc = "Show a title when a miniboss spawns.")
            @ConfigEditorBoolean
            public boolean slayerMinibossTitle = false;

            @Expose
            @ConfigOption(name = "Miniboss Sound Notification", desc = "Play a sound when a miniboss spawns.")
            @ConfigEditorBoolean
            public boolean slayerMinibossSound = false;

            @Expose
            @ConfigOption(name = "Slayer Minibosses Color", desc = "Color of slayer minibosses.")
            @ConfigEditorColour
            public String slayerColor = "0:92:154:255:255";
    }

    public static class BossSettings {

            @Expose
            @ConfigOption(name = "Slayer Bosses Display", desc = "Draw a box around slayer bosses.")
            @ConfigEditorBoolean
            public boolean slayerBosses = false;

            @Expose
            @ConfigOption(name = "Slayer Bosses Outline", desc = "Draw an outline around slayer bosses.")
            @ConfigEditorBoolean
            public boolean slayerBossesOutline = false;

            @Expose
            @ConfigOption(name = "Slayer Boss Health Display", desc = "Displays the slayer health on screen.")
            @ConfigEditorBoolean
            public boolean slayerBossHP = false;

            @Expose
            @ConfigOption(name = "Edit Slayer HP Overlay Position", desc = "Adjust the slayer hp overlay position visually")
            @ConfigEditorButton(runnableId = 8, buttonText = "Edit Position")
            public String editSlayerOverlayPositionButton = "";

            @Expose
            @ConfigOption(name = "Slayer Bosses Color", desc = "Color of slayer bosses.")
            @ConfigEditorColour
            public String slayerBossColor = "0:92:154:255:255";

            @Expose
            @ConfigOption(name = "Slayer Boss Time", desc = "Show slayer boss time.")
            @ConfigEditorBoolean
            public boolean slayerBossTimer = false;

            @Expose
            public Position slayerBossHPPos = new Position(10, 10, false, true);
    }

    public static class BeaconSettings {

            @Expose
            @ConfigOption(name = "Show Beacon Waypoint", desc = "Show waypoint for beacon.")
            @ConfigEditorBoolean
            public boolean slayerShowBeaconPath = false;

            @Expose
            @ConfigOption(name = "Beacon Color", desc = "Color of beacon waypoint.")
            @ConfigEditorColour
            public String slayerBeaconColor = "0:128:0:128:255";

            @Expose
            @ConfigOption(name = "Show Beacon Tracer", desc = "Traces a line to the beacon.")
            @ConfigEditorBoolean
            public boolean showTracerToBeacon = false;

            @Expose
            @ConfigOption(name = "Beacon notifier", desc = "Shows a message middle screen when beacon is detected.")
            @ConfigEditorBoolean
            public boolean notifyBeaconInScreen = false;
    }

    public static class InfernoDemonlord {

            @Expose
            @ConfigOption(name = "Display Blaze Pillar Title", desc = "Display title when a blaze pillar is nearby.")
            @ConfigEditorBoolean
            public boolean slayerFirePillarDisplay = false;

            @Expose
            @ConfigOption(name = "Filter Hellion Shield Spam", desc = "Hide the 'Hellion Shield' and 'Strike using attunement' messages when hitting with the wrong attunement.")
            @ConfigEditorBoolean
            public boolean slayerHellionShieldFilter = false;

    }

    @Expose
    public BlazeAttunementSettings blazeAttunementSettings = new BlazeAttunementSettings();

    public static class BlazeAttunementSettings {

            @Expose
            @ConfigOption(name = "Show Outline", desc = "Draw a coloured outline around attunement minions.")
            @ConfigEditorBoolean
            public boolean slayerBlazeAttunementOutline = false;

            @Expose
            @ConfigOption(name = "Show Full Attunement", desc = "Fill the attunement minion model with its attunement colour.")
            @ConfigEditorBoolean
            public boolean slayerBlazeAttunementFill = false;
    }
}
