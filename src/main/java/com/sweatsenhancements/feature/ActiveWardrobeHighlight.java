package com.sweatsenhancements.feature;

import java.util.Locale;
import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/** Marks only the equipped armor-set button inside the Wardrobe chest. */
public final class ActiveWardrobeHighlight {
    public static boolean isEquippedLabel(String name) {
        String plain = EnumChatFormatting.getTextWithoutFormattingCodes(name);
        return plain != null && plain.toLowerCase(Locale.ROOT).contains("equipped");
    }

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        try {
            if (event == null || !(event.gui instanceof GuiChest) || SweatsEnhancements.cfg == null
                    || !SweatsEnhancements.cfg.getBool(Cfg.key("MS221"), false)) return;
            GuiChest gui = (GuiChest) event.gui;
            if (!(gui.inventorySlots instanceof ContainerChest)) return;
            ContainerChest chest = (ContainerChest) gui.inventorySlots;
            String title = chest.getLowerChestInventory().getDisplayName().getUnformattedText();
            if (title == null || !title.toLowerCase(Locale.ROOT).startsWith("wardrobe")) return;
            int size = chest.getLowerChestInventory().getSizeInventory();
            if (size < 9 || size > 54 || size % 9 != 0) return;
            int left = (gui.width - 176) / 2;
            int top = (gui.height - (114 + size / 9 * 18)) / 2;
            boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            try {
            for (int i = 0; i < size; i++) {
                Slot slot = chest.getSlot(i);
                ItemStack item = slot.getStack();
                if (item == null || !isEquippedLabel(item.getDisplayName())) continue;
                int x = left + slot.xDisplayPosition;
                int y = top + slot.yDisplayPosition;
                Gui.drawRect(x, y, x + 16, y + 2, 0xEE37FF37);
                Gui.drawRect(x, y + 14, x + 16, y + 16, 0xEE37FF37);
                Gui.drawRect(x, y, x + 2, y + 16, 0xEE37FF37);
                Gui.drawRect(x + 14, y, x + 16, y + 16, 0xEE37FF37);
            }
            } finally {
                if (depth) GL11.glEnable(GL11.GL_DEPTH_TEST);
            }
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] active wardrobe highlight failed: " + t);
        }
    }
}
