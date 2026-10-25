package com.sweatsenhancements.feature;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

/** Remap menu left-click to button 2, only on identified server-menu slots. */
public final class SkyblockMenuMiddleClick {
    private static final Set<String> MENUS = new HashSet<String>(Arrays.asList(
            "skyblock menu", "chronomatron", "experiment over", "ultrasequencer",
            "superpairs", "coop bank account", "booster cookie", "pets", "storage",
            "your museum", "museum", "settings", "autopet", "quest log", "trades", "profiles"));

    public static boolean isSupportedMenu(String title) {
        if (title == null) return false;
        return MENUS.contains(title.trim().toLowerCase(Locale.ROOT));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        try {
            if (event == null || !(event.gui instanceof GuiChest) || SweatsEnhancements.cfg == null
                    || !SweatsEnhancements.cfg.getBool(Cfg.key("MS213"), false)
                    || Mouse.getEventButton() != 0 || !Mouse.getEventButtonState()) return;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null || mc.playerController == null) return;
            GuiChest gui = (GuiChest) event.gui;
            if (!(gui.inventorySlots instanceof ContainerChest)) return;
            ContainerChest chest = (ContainerChest) gui.inventorySlots;
            if (!isSupportedMenu(chest.getLowerChestInventory().getDisplayName().getUnformattedText())) return;
            Slot slot = gui.getSlotUnderMouse();
            if (slot == null || slot.slotNumber < 0
                    || slot.slotNumber >= chest.getLowerChestInventory().getSizeInventory()
                    || !slot.getHasStack()) return;
            event.setCanceled(true);
            mc.playerController.windowClick(chest.windowId, slot.slotNumber, 2, 0, mc.thePlayer);
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] menu middle-click failed: " + t);
        }
    }
}
