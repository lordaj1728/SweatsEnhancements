package com.sweatsenhancements.feature;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Hold the sprint input while enabled; vanilla handles movement and hunger restrictions. */
public final class AlwaysSprint {
    private boolean heldBySweatsEnhancements;
    private int heldCode;

    public static boolean shouldHold(boolean enabled, boolean inWorld, int keyCode) {
        return enabled && inWorld && keyCode != 0;
    }

    /** Respect the player's physical sprint key when releasing SweatsEnhancements's synthetic press. */
    private static boolean physicallyDown(int code) {
        if (code > 0 && code < Keyboard.KEYBOARD_SIZE) return Keyboard.isKeyDown(code);
        if (code <= -100 && code > -120) return Mouse.isButtonDown(code + 100);
        return false;
    }

    private void release() {
        if (!heldBySweatsEnhancements) return;
        KeyBinding.setKeyBindState(heldCode, physicallyDown(heldCode));
        heldBySweatsEnhancements = false;
        heldCode = 0;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            boolean enabled = SweatsEnhancements.cfg != null && SweatsEnhancements.cfg.getBool(Cfg.key("MS204"), false);
            boolean inWorld = mc != null && mc.theWorld != null && mc.thePlayer != null;
            int code = mc == null || mc.gameSettings == null ? 0
                    : mc.gameSettings.keyBindSprint.getKeyCode();
            if (!shouldHold(enabled, inWorld, code)) {
                release();
                return;
            }
            if (heldBySweatsEnhancements && heldCode != code) release(); // key was rebound
            // Supply sprint input, not a forced player sprint state. Minecraft handles whether
            // sneaking, using an item, hunger, collisions, etc. permit sprinting.
            if (!mc.gameSettings.keyBindSprint.isKeyDown()) {
                KeyBinding.setKeyBindState(code, true);
                heldBySweatsEnhancements = true;
                heldCode = code;
            }
        } catch (Throwable t) {
            try { release(); } catch (Throwable ignored) { }
            System.out.println("[SweatsEnhancements] always sprint input failed: " + t);
        }
    }
}
