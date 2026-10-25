package com.sweatsenhancements.feature;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

/** A reversible brightness adjustment. Never changes the player's saved options file. */
public final class Fullbright {
    private boolean applied;
    private float before;

    /** Pure state transition for the offline test; preserves the player's chosen gamma. */
    public float update(boolean enabled, boolean inWorld, float current) {
        if (!enabled || !inWorld) {
            if (!applied) return current;
            applied = false;
            return before;
        }
        if (!applied) {
            before = current;
            applied = true;
        }
        return 1000.0F;
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.gameSettings != null) {
                mc.gameSettings.gammaSetting = update(false, false, mc.gameSettings.gammaSetting);
            }
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] brightness restore failed: " + t);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.gameSettings == null) return;
            boolean enabled = SweatsEnhancements.cfg != null && SweatsEnhancements.cfg.getBool(Cfg.key("MS200"), false);
            mc.gameSettings.gammaSetting = update(enabled, mc.theWorld != null,
                    mc.gameSettings.gammaSetting);
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] brightness update failed: " + t);
        }
    }
}
