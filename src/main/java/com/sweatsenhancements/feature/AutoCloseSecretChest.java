package com.sweatsenhancements.feature;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Exact-title-only convenience. Delay the close until the next client tick, not mid-GUI draw. */
public final class AutoCloseSecretChest {
    private int pendingWindow = -1;

    public static boolean shouldClose(String title) { return "Secret Chest".equals(title); }

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.BackgroundDrawnEvent event) {
        try {
            if (event == null || !(event.gui instanceof GuiChest) || SweatsEnhancements.cfg == null
                    || !SweatsEnhancements.cfg.getBool(Cfg.key("MS224"), false)) return;
            GuiChest gui = (GuiChest) event.gui;
            if (!(gui.inventorySlots instanceof ContainerChest)) return;
            ContainerChest chest = (ContainerChest) gui.inventorySlots;
            if (shouldClose(chest.getLowerChestInventory().getDisplayName().getUnformattedText()))
                pendingWindow = chest.windowId;
        } catch (Throwable t) { System.out.println("[SweatsEnhancements] auto-close chest detection failed: " + t); }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || pendingWindow < 0) return;
        try {
            int pending = pendingWindow;
            pendingWindow = -1;
            if (SweatsEnhancements.cfg == null || !SweatsEnhancements.cfg.getBool(Cfg.key("MS224"), false)
                    || SweatsEnhancements.mc == null || SweatsEnhancements.mc.thePlayer == null || !(SweatsEnhancements.mc.currentScreen instanceof GuiChest)) return;
            GuiChest gui = (GuiChest) SweatsEnhancements.mc.currentScreen;
            if (!(gui.inventorySlots instanceof ContainerChest)) return;
            ContainerChest chest = (ContainerChest) gui.inventorySlots;
            if (chest.windowId != pending || !shouldClose(chest.getLowerChestInventory()
                    .getDisplayName().getUnformattedText())) return;
            // closeScreen can grab the mouse; doing it after GUI drawing avoids a mid-frame focus change.
            SweatsEnhancements.mc.thePlayer.closeScreen();
            System.out.println("[SweatsEnhancements] closed Secret Chest (no camera rotation requested)");
        } catch (Throwable t) { System.out.println("[SweatsEnhancements] auto-close chest failed: " + t); }
    }
}
