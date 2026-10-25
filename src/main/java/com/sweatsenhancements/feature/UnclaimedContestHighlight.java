package com.sweatsenhancements.feature;

import java.util.Locale;
import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/** Colour claimable slots green and claimed slots red, without touching the container. */
public final class UnclaimedContestHighlight {
    private int lastWindow = -1;

    public static boolean isContestMenu(String title) {
        return title != null && title.trim().toLowerCase(Locale.ROOT).startsWith("your contests");
    }

    /** 1 = claimable, 2 = claimed, 0 = unrelated. */
    public static int rewardState(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) return 0;
        NBTTagCompound root = stack.getTagCompound();
        if (root == null || !root.hasKey("display", 10)) return 0;
        return rewardStateLore(root.getCompoundTag("display").getTagList("Lore", 8));
    }

    public static int rewardStateLore(NBTTagList lore) {
        if (lore == null) return 0;
        for (int i = 0; i < lore.tagCount(); i++) {
            String text = EnumChatFormatting.getTextWithoutFormattingCodes(lore.getStringTagAt(i));
            if (text == null) continue;
            String line = text.toLowerCase(Locale.ROOT).trim();
            if (line.contains("click to claim reward")) return 1;
            if (line.contains("rewards claimed") || line.contains("reward claimed")
                    || line.contains("already claimed") || line.contains("claimed rewards")) return 2;
        }
        return 0;
    }

    public static boolean isUnclaimed(ItemStack stack) {
        return rewardState(stack) == 1;
    }

    @SubscribeEvent
    public void onBackground(GuiScreenEvent.BackgroundDrawnEvent event) {
        if (event != null) render(event.gui, false);
    }

    @SubscribeEvent
    public void onForeground(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (event != null) render(event.gui, true);
    }

    private void render(net.minecraft.client.gui.GuiScreen screen, boolean outline) {
        try {
            if (!(screen instanceof GuiChest) || SweatsEnhancements.cfg == null
                    || !SweatsEnhancements.cfg.getBool(Cfg.key("MS216"), false)) return;
            GuiChest gui = (GuiChest) screen;
            if (!(gui.inventorySlots instanceof ContainerChest)) return;
            ContainerChest chest = (ContainerChest) gui.inventorySlots;
            if (!isContestMenu(chest.getLowerChestInventory().getDisplayName().getUnformattedText())) return;
            int size = chest.getLowerChestInventory().getSizeInventory();
            if (size <= 0 || size % 9 != 0 || size > 54) return;
            int left = (gui.width - 176) / 2;
            int top = (gui.height - (114 + 18 * (size / 9))) / 2;
            int[] counts = new int[3];
            boolean wasDepthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            if (outline) GL11.glDisable(GL11.GL_DEPTH_TEST);
            try {
                for (int i = 0; i < size; i++) {
                    Slot slot = chest.getSlot(i);
                    int state = rewardState(slot.getStack());
                    if (state == 0) continue;
                    counts[state]++;
                    int x = left + slot.xDisplayPosition;
                    int y = top + slot.yDisplayPosition;
                    int color = state == 1 ? 0xDD37FF37 : 0xDDFF3737;
                    if (!outline) {
                        Gui.drawRect(x, y, x + 16, y + 16, state == 1 ? 0x9937FF37 : 0x99FF3737);
                    } else {
                        Gui.drawRect(x, y, x + 16, y + 2, color);
                        Gui.drawRect(x, y + 14, x + 16, y + 16, color);
                        Gui.drawRect(x, y, x + 2, y + 16, color);
                        Gui.drawRect(x + 14, y, x + 16, y + 16, color);
                    }
                }
            } finally {
                if (outline && wasDepthEnabled) GL11.glEnable(GL11.GL_DEPTH_TEST);
                else if (outline) GL11.glDisable(GL11.GL_DEPTH_TEST);
            }
            if (outline && lastWindow != chest.windowId) {
                System.out.println("[SweatsEnhancements] contest slots: claimable=" + counts[1] + ", claimed=" + counts[2]
                        + ", menu slots=" + size);
                lastWindow = chest.windowId;
            }
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] contest highlight failed: " + t);
        }
    }
}
