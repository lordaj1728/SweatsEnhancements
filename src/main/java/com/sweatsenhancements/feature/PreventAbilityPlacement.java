package com.sweatsenhancements.feature;

import java.util.Locale;
import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Cancel only known accidental placement cases, not every right-click ability. */
public final class PreventAbilityPlacement {
    public static boolean shouldCancel(String display, boolean plantSurface, boolean hopperItem) {
        if (display == null) return false;
        String plain = EnumChatFormatting.getTextWithoutFormattingCodes(display);
        if (plain == null) return false;
        String name = plain.toLowerCase(Locale.ROOT);
        if (name.contains("superboom tnt") || name.contains("infinityboom tnt")) return true;
        if (hopperItem && name.contains("weird tuba")) return true;
        return plantSurface && (name.contains("flower of truth")
                || name.contains("bouquet of lies") || name.contains("spirit sceptre"));
    }

    @SubscribeEvent
    public void onInteract(PlayerInteractEvent event) {
        try {
            if (event == null || event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK
                    || event.world == null || !event.world.isRemote || SweatsEnhancements.cfg == null
                    || !SweatsEnhancements.cfg.getBool(Cfg.key("MS214"), false)) return;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null || mc.thePlayer != event.entityPlayer) return;
            ItemStack held = event.entityPlayer.getHeldItem();
            BlockPos pos = event.pos;
            if (held == null || pos == null) return;
            Block block = event.world.getBlockState(pos).getBlock();
            boolean plantSurface = block == Blocks.grass || block == Blocks.dirt
                    || block == Blocks.flower_pot || block == Blocks.tallgrass
                    || block == Blocks.double_plant;
            boolean hopperItem = held.getItem() == Item.getItemFromBlock(Blocks.hopper);
            if (shouldCancel(held.getDisplayName(), plantSurface, hopperItem)) event.setCanceled(true);
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] ability placement guard failed: " + t);
        }
    }
}
