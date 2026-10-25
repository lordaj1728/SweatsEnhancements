package com.sweatsenhancements.feature;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/** Optional config-GUI key; toggles a held or hovered item, for UUID-bearing items only. */
public final class SafeguardKey {
    public static final String KEY = "shortcuts.safeguardKey";
    private boolean held;

    public static boolean allowed(int key, int dropKey) {
        return key > Keyboard.KEY_NONE && key < Keyboard.KEYBOARD_SIZE && key != dropKey;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || SweatsEnhancements.cfg == null) { held = false; return; }
            int key = SweatsEnhancements.cfg.getInt(KEY, Keyboard.KEY_NONE);
            if (!allowed(key, mc.gameSettings.keyBindDrop.getKeyCode()) || !Keyboard.isCreated()) {
                held = false;
                return;
            }
            boolean down = Keyboard.isKeyDown(key);
            if (down && !held && mc.theWorld != null && mc.thePlayer != null
                    && (mc.currentScreen == null || mc.currentScreen instanceof GuiContainer)) {
                ItemStack item;
                if (mc.currentScreen instanceof GuiContainer) {
                    GuiContainer gui = (GuiContainer) mc.currentScreen;
                    Slot slot = gui.getSlotUnderMouse();
                    item = slot != null && slot.getHasStack() ? slot.getStack()
                            : mc.thePlayer.inventory.getItemStack(); // item carried on the cursor
                } else item = mc.thePlayer.getHeldItem();
                if (item == null) { held = down; return; }
                Boolean on = ProtectedItems.toggle(item);
                String text = on == null ? "§cThis item has no server UUID and cannot be safeguarded."
                        : on ? "§aSafeguarding this UUID item wherever you move it."
                        : "§eSafeguard removed from this item.";
                mc.thePlayer.addChatMessage(new ChatComponentText("§b[SweatsEnhancements] §r" + text));
            }
            held = down; // never fire just because a GUI closed while the key was held
        } catch (Throwable t) { System.out.println("[SweatsEnhancements] safeguard key failed: " + t); }
    }
}
