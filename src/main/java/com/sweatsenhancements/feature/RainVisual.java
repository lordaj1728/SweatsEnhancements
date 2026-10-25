package com.sweatsenhancements.feature;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Client-side weather visuals only: leave server weather and gameplay untouched. */
public final class RainVisual {
    private WorldClient hiddenWorld;
    private float previousStrength;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            WorldClient world = mc == null ? null : mc.theWorld;
            boolean enabled = SweatsEnhancements.cfg != null && SweatsEnhancements.cfg.getBool(Cfg.key("MS207"), false);
            if (hiddenWorld != null && (hiddenWorld != world || !enabled)) {
                // On disabling in the same world, restore the strength seen before suppression.
                // Never mutate a world after a disconnect/dimension switch.
                if (hiddenWorld == world) world.setRainStrength(previousStrength);
                hiddenWorld = null;
            }
            if (!enabled || world == null) return;
            if (hiddenWorld != world) {
                hiddenWorld = world;
                previousStrength = world.getRainStrength(1.0F);
            }
            world.setRainStrength(0.0F);
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] rain visual filter failed: " + t);
        }
    }
}
