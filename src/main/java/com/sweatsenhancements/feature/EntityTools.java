package com.sweatsenhancements.feature;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Entity hiding: damage splashes and dead entities.
 *
 * Both are render-side only - nothing is removed from the world, so nothing here can
 * desync you from the server.
 *
 * "Hide Useless Armor Stands" used to live here and was removed: it caught Hoppity's Hunt eggs,
 * which are nameless armour stands like any other. A rule that cannot tell an egg from a floating
 * NPC label is not worth keeping.
 */
public class EntityTools {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderLiving(RenderLivingEvent.Pre event) {
        try {
            if (event == null || event.entity == null) return;

            // MS007 - damage splashes. SkyBlock floats damage numbers over the mob as armour stands
            // whose whole name is the number, sometimes with a colour code in front of it and
            // sometimes with a k/m suffix. The test is "has a digit and no other letters", which
            // leaves pet names, NPC labels and holograms alone.
            if (SweatsEnhancements.cfg.getBool(Cfg.key("MS007"), true) && event.entity instanceof EntityArmorStand) {
                EntityArmorStand stand = (EntityArmorStand) event.entity;
                if (stand.hasCustomName()
                        && isDamageNumber(EnumChatFormatting.getTextWithoutFormattingCodes(stand.getName()))) {
                    event.setCanceled(true);
                    return;
                }
            }

            // MS027 - dead entities
            if (SweatsEnhancements.cfg.getBool(Cfg.key("MS027"), true) && event.entity instanceof EntityLivingBase) {
                EntityLivingBase living = (EntityLivingBase) event.entity;
                if (living.getHealth() <= 0.0F && !living.isEntityAlive()) {
                    event.setCanceled(true);
                    return;
                }
            }

        } catch (Throwable t) {
            // a render hook that throws would spam the log every frame
        }
    }

    /**
     * True when a floating name is a damage number rather than a name.
     *
     * Damage numbers arrive as digits with separators, sometimes signed, sometimes with a
     * magnitude letter stuck on the end (12k, 1.2m). Anything containing another letter is a name
     * - a pet, an NPC, a hologram - and is left alone.
     */
    private static boolean isDamageNumber(String raw) {
        if (raw == null) return false;
        String s = raw.trim();
        if (s.isEmpty()) return false;

        // drop a magnitude suffix: 12k, 1.2m, 340b
        char last = s.charAt(s.length() - 1);
        if (s.length() > 1
                && (last == 'k' || last == 'K' || last == 'm' || last == 'M' || last == 'b' || last == 'B')) {
            s = s.substring(0, s.length() - 1).trim();
        }

        boolean digit = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                digit = true;
            } else if (Character.isLetter(c)) {
                return false;
            }
        }
        return digit;
    }
}
