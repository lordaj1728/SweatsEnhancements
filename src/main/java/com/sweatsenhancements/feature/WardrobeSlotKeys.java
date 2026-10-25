package com.sweatsenhancements.feature;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

/** Select slots 1-9 only in a recognised Wardrobe chest with a complete slot row. */
public final class WardrobeSlotKeys {
    public static boolean isWardrobe(String title, int lowerSize) {
        return title != null && title.trim().toLowerCase(java.util.Locale.ROOT).startsWith("wardrobe")
                && lowerSize >= 45;
    }

    public static int chestSlotFor(int index) {
        return index >= 0 && index < 9 ? 36 + index : -1;
    }

    /** -1 means no unique binding; an ambiguous assignment must never click a slot. */
    public static int slotForKey(int pressed, int[] bindings) {
        if (pressed <= Keyboard.KEY_NONE || bindings == null || bindings.length != 9) return -1;
        int match = -1;
        for (int i = 0; i < bindings.length; i++) {
            if (bindings[i] == pressed) {
                if (match != -1) return -1;
                match = i;
            }
        }
        return match;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onKey(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        try {
            if (event == null || !(event.gui instanceof GuiChest) || SweatsEnhancements.cfg == null
                    || !SweatsEnhancements.cfg.getBool(Cfg.key("MS210"), false)
                    || !Keyboard.getEventKeyState() || Keyboard.isRepeatEvent()) return;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null || mc.playerController == null || mc.theWorld == null) return;
            GuiChest chest = (GuiChest) event.gui;
            if (!(chest.inventorySlots instanceof ContainerChest)) return;
            ContainerChest container = (ContainerChest) chest.inventorySlots;
            String title = container.getLowerChestInventory().getDisplayName().getUnformattedText();
            if (!isWardrobe(title, container.getLowerChestInventory().getSizeInventory())) return;
            int[] keys = new int[9];
            for (int i = 0; i < 9; i++)
                keys[i] = SweatsEnhancements.cfg.getInt("shortcuts.wardrobeSlotKey" + (i + 1), Keyboard.KEY_1 + i);
            int slot = slotForKey(Keyboard.getEventKey(), keys);
            if (slot < 0) return;
            // Consume the number key so vanilla's hotbar-swap action cannot run as well.
            event.setCanceled(true);
            mc.playerController.windowClick(container.windowId, chestSlotFor(slot), 0, 0, mc.thePlayer);
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] wardrobe slot shortcut failed: " + t);
        }
    }
}
