package com.sweatsenhancements.feature;

import java.util.ArrayList;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Optional client-side visual filter; never sends block or entity changes to the server. */
public final class HideFallingBlocks {
    private int ticks;

    public static boolean hidesType(Class<?> type) {
        return type != null && EntityFallingBlock.class.isAssignableFrom(type);
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent event) {
        try {
            if (event == null || event.world == null || !event.world.isRemote || event.entity == null
                    || SweatsEnhancements.cfg == null || !SweatsEnhancements.cfg.getBool(Cfg.key("MS205"), false)) return;
            if (hidesType(event.entity.getClass())) event.setCanceled(true);
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] falling-block spawn filter failed: " + t);
        }
    }

    /** Hide ones already spawned when the toggle is switched on, client side only. */
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        try {
            if (++ticks < 5) return;
            ticks = 0;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.theWorld == null || !mc.theWorld.isRemote || SweatsEnhancements.cfg == null
                    || !SweatsEnhancements.cfg.getBool(Cfg.key("MS205"), false)) return;
            World world = mc.theWorld;
            for (Entity entity : new ArrayList<Entity>(world.loadedEntityList)) {
                if (entity != null && hidesType(entity.getClass())) world.removeEntity(entity);
            }
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] falling-block sweep failed: " + t);
        }
    }
}
