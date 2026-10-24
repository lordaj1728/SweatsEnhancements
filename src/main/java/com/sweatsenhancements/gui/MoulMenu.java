package com.sweatsenhancements.gui;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;

import io.github.notenoughupdates.moulconfig.gui.GuiScreenElementWrapper;
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor;
import io.github.notenoughupdates.moulconfig.processor.ConfigProcessorDriver;
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

/**
 * Opens the config screen.
 *
 * The screen is drawn by MoulConfig, a config screen library published separately, which is why the
 * menu looks the way it does. The switches
 * are read from and written to SweatsEnhancements's own config file; MoulConfig's own storage is unused.
 *
 * Two safety nets, because this screen is the mod's front door:
 *  - if building it throws, the built-in ConfigMenu opens instead and the error goes to the log;
 *  - while it is open, the switches are mirrored back into the config every half second, so a
 *    toggle cannot be lost if the close hook is missed.
 */
public final class MoulMenu {

    /** config key for the wide-layout setting, written by the General category */
    private static final String WIDE_KEY = "gui.wide";

    private static SweatsConfig config;
    private static int ticksOpen;
    /**
     * The built screen, kept between openings.
     *
     * Building it means constructing an editor for every one of the ~600 options, which is a
     * one-time cost - MoulConfig skips the options that are off screen when it draws, and the
     * earlier per-second hitch was this mod's own doing, not the library's. It is built on the first
     * open and reused after that; only the layout setting forces a rebuild, because the layout is
     * chosen while the editor is being constructed.
     */
    private static MoulConfigEditor<SweatsConfig> editor;
    private static Boolean editorWide;
    /** how many times the screen has actually been built, for /se panels */
    public static volatile int builds;

    /**
     * What the menu did the last time it was opened, for /se panels.
     *
     * The screen falls back to the built-in menu if the MoulConfig screen cannot be built, and the
     * built-in one does not carry the newer switches - so a panel that "does nothing" can just as
     * easily be a switch that was never on screen. This says which one happened.
     */
    public static volatile String menuState = "not opened yet";

    private MoulMenu() {
    }

    public static boolean isOpen() {
        return Minecraft.getMinecraft().currentScreen instanceof MoulScreen;
    }

    public static void open() {
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (config == null) config = new SweatsConfig();
            config.loadFromCfg();

            boolean wide = SweatsEnhancements.cfg.getBool(WIDE_KEY, false);
            if (editor != null && editorWide != null && editorWide.booleanValue() == wide) {
                menuState = "MoulConfig, reusing the screen built earlier (" + builds + " build(s))";
                ticksOpen = 0;
                mc.displayGuiScreen(new MoulScreen(editor));
                return;
            }

            MoulConfigProcessor<SweatsConfig> processor = MoulConfigProcessor.withDefaults(config);
            new ConfigProcessorDriver(processor).processConfig(config);
            // the identifiers of every category the library accepted, so /se panels can show
            // whether the scoreboard and tab list categories actually made it into the menu
            StringBuilder ids = new StringBuilder();
            for (String id : processor.getAllCategories().keySet()) {
                if (ids.length() > 0) ids.append(", ");
                ids.append(id);
            }
            menuState = "MoulConfig, " + processor.getAllCategories().size() + " categories built"
                    + " (" + builds + " build(s)): " + ids;
            editor = new MoulConfigEditor<SweatsConfig>(processor);
            // Layout is a setting rather than a fixed choice. Narrow (the library's default)
            // stacks the description under the name; wide spreads the name and description across
            // a panel half as wide again, with the switch under the name. Narrow is the default
            // because it fits smaller screens, and the wide panel is not kind on a phone.
            // Changing it takes effect next time the screen is opened - the layout is chosen while
            // the editor is being built.
            editor.setWide(Boolean.valueOf(wide));
            editorWide = Boolean.valueOf(wide);
            builds++;

            ticksOpen = 0;
            mc.displayGuiScreen(new MoulScreen(editor));
        } catch (Throwable t) {
            // Loud on purpose: if this ever fires in a real game, the cause has to be visible
            // without digging through the log, because the menu is the only way in.
            System.out.println("[SweatsEnhancements] !! the MoulConfig screen could not be built, using the built-in menu");
            t.printStackTrace();
            menuState = "the built-in fallback menu - the MoulConfig screen failed with: " + t;
            report("MoulConfig screen failed: " + t);
            try {
                mc.displayGuiScreen(new ConfigMenu());
            } catch (Throwable t2) {
                System.out.println("[SweatsEnhancements] the built-in menu failed too: " + t2);
            }
        }
    }

    /** Says something in chat if there is a chat to say it in. */
    private static void report(String message) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.thePlayer != null) {
                mc.thePlayer.addChatComponentMessage(new ChatComponentText(
                        "\u00a7c[SweatsEnhancements] \u00a7f" + message));
            }
        } catch (Throwable ignored) {
            // chat is a nicety, never a requirement
        }
    }

    /**
     * Called from the client tick; keeps SweatsEnhancements's config in step with the switches on screen.
     *
     * The mirror is cheap now - it only writes values that actually changed, and writing a value no
     * longer writes the file - so the pass itself can stay. What is gone is the disk work: the file
     * is written by the throttled flush on the tick, not here, and not twice a second.
     */
    public static void tick() {
        try {
            if (config == null || !isOpen()) return;
            if (++ticksOpen % 10 != 0) return;
            config.saveToCfg();
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] menu tick failed: " + t);
        }
    }

    private static class MoulScreen extends GuiScreenElementWrapper {

        MoulScreen(MoulConfigEditor<SweatsConfig> editor) {
            super(editor);
        }

        @Override
        public void onGuiClosed() {
            try {
                if (config != null) config.saveToCfg();
            } catch (Throwable t) {
                System.out.println("[SweatsEnhancements] could not save the menu changes: " + t);
            }
            super.onGuiClosed();
        }
    }
}
