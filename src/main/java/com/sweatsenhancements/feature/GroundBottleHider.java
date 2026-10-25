package com.sweatsenhancements.feature;

import java.util.ArrayList;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Client-only ground-item filter. Never removes items from the server. */
public final class GroundBottleHider {
    private int ticks;

    public static boolean isExperienceBottle(ItemStack stack) {
        return stack != null && stack.getItem() == Items.experience_bottle;
    }

    private static boolean shouldHide(Entity entity) {
        return entity instanceof EntityXPOrb || (entity instanceof EntityItem
                && isExperienceBottle(((EntityItem) entity).getEntityItem()));
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent event) {
        try {
            if (event == null || event.world == null || !event.world.isRemote
                    || SweatsEnhancements.cfg == null || !SweatsEnhancements.cfg.getBool(Cfg.key("MS003"), false)) return;
            if (shouldHide(event.entity)) event.setCanceled(true);
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] bottle spawn filter failed: " + t);
        }
    }

    /** Also catches bottles that were already on the ground when the switch was enabled.
     * A client-only world removal leaves the server item and its pickup handling alone.
     */
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        try {
            if (++ticks < 5) return;
            ticks = 0;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.theWorld == null || !mc.theWorld.isRemote
                    || SweatsEnhancements.cfg == null || !SweatsEnhancements.cfg.getBool(Cfg.key("MS003"), false)) return;
            World world = mc.theWorld;
            for (Entity entity : new ArrayList<Entity>(world.loadedEntityList)) {
                if (shouldHide(entity)) world.removeEntity(entity);
            }
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] bottle sweep failed: " + t);
        }
    }
}
