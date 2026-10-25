package com.sweatsenhancements.feature;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/** Config-only keybinds. No new entries are registered in Minecraft Controls. */
public final class ConfigShortcuts {
    private static final String[] KEYS = {"wardrobeKey", "equipmentKey", "warpIslandKey",
            "warpHubKey", "warpDungeonHubKey"};
    private static final int[] DEFAULT_KEYS = {Keyboard.KEY_NONE, Keyboard.KEY_NONE,
            Keyboard.KEY_F7, Keyboard.KEY_F8, Keyboard.KEY_F9};
    private static final String[] IDS = {"MS208", "MS209", "MS211", "MS211", "MS211"};
    private static final String[] COMMANDS = {"/wardrobe", "/equipment", "/warp is",
            "/warp hub", "/warp dh"};
    private final boolean[] held = new boolean[KEYS.length];

    public static boolean shouldSend(boolean down, boolean held, boolean enabled, boolean inGame) {
        return down && !held && enabled && inGame;
    }

    /** Do not send multiple commands when two enabled shortcuts share one key. */
    public static boolean unique(int index, int[] keys, boolean[] enabled) {
        if (index < 0 || index >= keys.length || !enabled[index] || keys[index] <= Keyboard.KEY_NONE)
            return false;
        for (int i = 0; i < keys.length; i++) {
            if (i != index && enabled[i] && keys[i] == keys[index]) return false;
        }
        return true;
    }

    public static boolean down(int key) {
        return key > Keyboard.KEY_NONE && key < Keyboard.KEYBOARD_SIZE
                && Keyboard.isCreated() && Keyboard.isKeyDown(key);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || SweatsEnhancements.cfg == null) {
                for (int i = 0; i < held.length; i++) held[i] = false;
                return;
            }
            int[] keys = new int[KEYS.length];
            boolean[] enabled = new boolean[KEYS.length];
            boolean[] pressed = new boolean[KEYS.length];
            for (int i = 0; i < KEYS.length; i++) {
                keys[i] = SweatsEnhancements.cfg.getInt("shortcuts." + KEYS[i], DEFAULT_KEYS[i]);
                enabled[i] = SweatsEnhancements.cfg.getBool(Cfg.key(IDS[i]), false);
                pressed[i] = down(keys[i]);
            }
            boolean inGame = mc.currentScreen == null && mc.theWorld != null && mc.thePlayer != null;
            for (int i = 0; i < KEYS.length; i++) {
                if (shouldSend(pressed[i], held[i], enabled[i], inGame && unique(i, keys, enabled)))
                    mc.thePlayer.sendChatMessage(COMMANDS[i]);
                // Keep tracking even while in a GUI so closing it with a key held cannot fire it.
                held[i] = pressed[i];
            }
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] config shortcuts failed: " + t);
        }
    }
}
