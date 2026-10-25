package com.sweatsenhancements.feature;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import net.minecraft.block.Block;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Blank filler panes only; named panes and real inventory items retain tooltips. */
public final class BlankGlassTooltips {
    public static boolean blankName(String name) {
        if (name == null) return false;
        String clean = EnumChatFormatting.getTextWithoutFormattingCodes(name);
        return clean != null && clean.trim().isEmpty();
    }
    public static boolean isBlankPane(ItemStack stack) {
        if (stack == null || !stack.hasDisplayName() || !blankName(stack.getDisplayName())) return false;
        Block block = Block.getBlockFromItem(stack.getItem());
        return block == Blocks.stained_glass_pane || block == Blocks.glass_pane;
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTooltip(ItemTooltipEvent event) {
        if (event == null || event.toolTip == null || event.itemStack == null || SweatsEnhancements.cfg == null
                || !SweatsEnhancements.cfg.getBool(Cfg.key("MS228"), false) || SweatsEnhancements.mc == null
                || !(SweatsEnhancements.mc.currentScreen instanceof GuiChest)) return;
        if (isBlankPane(event.itemStack)) event.toolTip.clear();
    }
}
