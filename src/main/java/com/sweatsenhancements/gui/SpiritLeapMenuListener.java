package com.sweatsenhancements.gui;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import com.sweatsenhancements.feature.DungeonRunHud;
import com.sweatsenhancements.thirdparty.skyvelor.feature.DungeonContext;
import com.sweatsenhancements.thirdparty.skyvelor.feature.LeapUse;
import com.sweatsenhancements.thirdparty.skyvelor.feature.SpiritLeap;
import com.sweatsenhancements.thirdparty.skyvelor.gui.LeapScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** SweatsEnhancements opt-in adapter for Sky VeLor's original LeapScreen and SpiritLeap menu detection. */
public final class SpiritLeapMenuListener {
    private static boolean enabled(){return SweatsEnhancements.cfg!=null && SweatsEnhancements.cfg.getBool(Cfg.key("DU036"),false);}
    @SubscribeEvent public void onUse(PlayerInteractEvent event){
        if(SweatsEnhancements.mc==null||event.entityPlayer!=SweatsEnhancements.mc.thePlayer)return;
        if(event.action!=PlayerInteractEvent.Action.RIGHT_CLICK_AIR
                &&event.action!=PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK)return;
        ItemStack held=event.entityPlayer.getHeldItem();
        if(held!=null&&LeapUse.isLeapItemName(held.getDisplayName()))LeapUse.markUsed();
    }
    @SubscribeEvent public void onGuiOpen(GuiOpenEvent event){
        if(!enabled()||!DungeonRunHud.isInDungeon()||!(event.gui instanceof GuiChest))return;
        GuiChest chest=(GuiChest)event.gui;
        if(SpiritLeap.isLeapMenu(chest.inventorySlots))event.gui=new LeapScreen(chest.inventorySlots);
    }
    @SubscribeEvent public void onTick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END||SweatsEnhancements.mc==null)return;
        DungeonContext.tick(SweatsEnhancements.mc);
        // A server can populate the heads after opening the chest; retry without guessing a menu.
        if(!enabled()||!DungeonRunHud.isInDungeon()||!(SweatsEnhancements.mc.currentScreen instanceof GuiChest)
                ||SweatsEnhancements.mc.thePlayer==null||!(SweatsEnhancements.mc.thePlayer.openContainer instanceof ContainerChest))return;
        ContainerChest chest=(ContainerChest)SweatsEnhancements.mc.thePlayer.openContainer;
        if(SpiritLeap.isLeapMenu(chest))SweatsEnhancements.mc.displayGuiScreen(new LeapScreen(chest));
    }
}
