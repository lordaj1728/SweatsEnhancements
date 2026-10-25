package com.sweatsenhancements.feature;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;


/** Read-only pass floor label: restricted to a chest with multiple server-tagged pass items. */
public final class DungeonPassFloorTip {
    public static String floorLabel(String internalId) {
        if (internalId == null) return null;
        String prefix;
        if (internalId.startsWith("MASTER_CATACOMBS_PASS_")) prefix = "MASTER_CATACOMBS_PASS_";
        else if (internalId.startsWith("CATACOMBS_PASS_")) prefix = "CATACOMBS_PASS_";
        else return null;
        String suffix = internalId.substring(prefix.length());
        if (!suffix.matches("[0-9]{1,2}")) return null;
        int floor = Integer.parseInt(suffix) - 3;
        if (floor == 0 && !prefix.startsWith("MASTER")) return "E";
        return floor >= 1 && floor <= 7 ? (prefix.startsWith("MASTER") ? "M" : "F") + floor : null;
    }

    public static String itemLabel(ItemStack stack) {
        return floorLabel(internalId(stack));
    }

    /** Accept a pass selection only when the SERVER-provided chest contains at least two
     * independently tagged floor passes. The probe dump labels the player inventory, not the
     * open chest's title; requiring a guessed menu title hid all legitimate passes. */
    public static boolean isPassSelection(String[] ids) {
        if (ids == null) return false;
        int matched = 0;
        for (String id : ids) {
            if (floorLabel(id) != null && ++matched >= 2) return true;
        }
        return false;
    }

    private static String internalId(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) return null;
        NBTTagCompound tag = stack.getTagCompound();
        if (!tag.hasKey("ExtraAttributes", 10)) return null;
        NBTTagCompound extra = tag.getCompoundTag("ExtraAttributes");
        return extra.hasKey("id", 8) ? extra.getString("id") : null;
    }

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (event == null || !(event.gui instanceof GuiChest) || SweatsEnhancements.cfg == null
                || !SweatsEnhancements.cfg.getBool(Cfg.key("DU026"), false)) return;
        GuiChest gui = (GuiChest) event.gui;
        if (!(gui.inventorySlots instanceof ContainerChest)) return;
        ContainerChest chest = (ContainerChest) gui.inventorySlots;
        int size = chest.getLowerChestInventory().getSizeInventory();
        if (size <= 0 || size > 54 || size % 9 != 0) return;
        String[] ids = new String[size];
        for (int i = 0; i < size; i++) ids[i] = internalId(chest.getSlot(i).getStack());
        if (!isPassSelection(ids)) return;
        int left = (gui.width - 176) / 2;
        int top = (gui.height - (114 + 18 * size / 9)) / 2;
        // Post renders after vanilla tooltips. Suppress pass labels while any occupied
        // chest slot is hovered, so no label can be drawn over that tooltip.
        for (int i = 0; i < size; i++) {
            Slot hovered = chest.getSlot(i);
            if (hovered.getStack() == null || (SweatsEnhancements.cfg.getBool(Cfg.key("MS228"), false)
                    && BlankGlassTooltips.isBlankPane(hovered.getStack()))) continue;
            int hx = left + hovered.xDisplayPosition, hy = top + hovered.yDisplayPosition;
            if (event.mouseX >= hx && event.mouseX < hx + 16
                    && event.mouseY >= hy && event.mouseY < hy + 16) return;
        }
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        try {
            for (int i = 0; i < size; i++) {
                Slot slot = chest.getSlot(i);
                String label = itemLabel(slot.getStack());
                if (label == null) continue;
                int x = left + slot.xDisplayPosition;
                int y = top + slot.yDisplayPosition;
                Gui.drawRect(x, y + 9, x + 16, y + 17, 0xCC101824);
                SweatsEnhancements.mc.fontRendererObj.drawStringWithShadow(label, x + 1, y + 9, 0xFF70F4FF);
            }
        } finally {
            GL11.glPopAttrib();
        }
    }
}
