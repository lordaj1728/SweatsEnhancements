package com.sweatsenhancements.feature;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.gui.HudEditor;
import com.sweatsenhancements.gui.MoulMenu;
import com.sweatsenhancements.probe.Probe;
import net.minecraft.client.gui.GuiChat;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/** All SweatsEnhancements core shortcuts are configured inside /sweatsenhancements, never registered in Minecraft Controls. */
public final class CoreShortcuts {
    private static final String[] KEYS = {"shortcuts.menuKey", "shortcuts.hudKey", "shortcuts.dumpKey"};
    private static final int[] DEFAULTS = {Keyboard.KEY_P, Keyboard.KEY_H, Keyboard.KEY_I};
    private final boolean[] held = new boolean[3];

    private static int key(int i) { return SweatsEnhancements.cfg == null ? DEFAULTS[i] : SweatsEnhancements.cfg.getInt(KEYS[i], DEFAULTS[i]); }
    private static boolean valid(int key) { return key > Keyboard.KEY_NONE && key < Keyboard.KEYBOARD_SIZE; }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        try {
            int[] keys = {key(0), key(1), key(2)};
            for (int i = 0; i < held.length; i++) {
                boolean down = valid(keys[i]) && Keyboard.isCreated() && Keyboard.isKeyDown(keys[i]);
                if (down && !held[i] && SweatsEnhancements.mc != null && SweatsEnhancements.mc.thePlayer != null
                        && SweatsEnhancements.mc.currentScreen == null && ConfigShortcuts.unique(i, keys,
                        new boolean[] {true, true, true})) {
                    if (i == 0) MoulMenu.open();
                    else if (i == 1) SweatsEnhancements.mc.displayGuiScreen(new HudEditor());
                    else Probe.dump(SweatsEnhancements.mc.thePlayer);
                }
                held[i] = down;
            }
        } catch (Throwable t) { System.out.println("[SweatsEnhancements] core shortcut failed: " + t); }
    }

    /** Probe also works inside non-chat GUI to capture item NBT. */
    @SubscribeEvent
    public void onGuiKey(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        try {
            int key = key(2);
            if (event == null || event.gui == null || event.gui instanceof GuiChat
                    || !valid(key) || SweatsEnhancements.mc == null || SweatsEnhancements.mc.thePlayer == null
                    || !Keyboard.getEventKeyState() || Keyboard.isRepeatEvent()
                    || Keyboard.getEventKey() != key || key == key(0) || key == key(1)) return;
            Probe.dump(SweatsEnhancements.mc.thePlayer);
            event.setCanceled(true);
        } catch (Throwable t) { System.out.println("[SweatsEnhancements] GUI probe shortcut failed: " + t); }
    }
}
