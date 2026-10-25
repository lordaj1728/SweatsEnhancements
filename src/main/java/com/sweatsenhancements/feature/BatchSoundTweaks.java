package com.sweatsenhancements.feature;

import java.util.Locale;
import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Sound-only filters; never intercepts outgoing packets or changes server gameplay. */
public final class BatchSoundTweaks {
    private long lastZombieDeath;

    private static String normalize(String sound) {
        if (sound == null) return "";
        String name = sound.toLowerCase(Locale.ROOT);
        return name.startsWith("minecraft:") ? name.substring(10) : name;
    }

    public static boolean isJerrySound(String sound, String heldDisplay) {
        if (heldDisplay == null) return false;
        String plain = EnumChatFormatting.getTextWithoutFormattingCodes(heldDisplay);
        if (plain == null || !plain.toLowerCase(Locale.ROOT).contains("jerry-chine gun")) return false;
        String name = normalize(sound);
        return "mob.villager.yes".equals(name) || "mob.villager.haggle".equals(name);
    }

    public static boolean isDropChime(String sound, float volume, long sinceDeathMs) {
        return "note.pling".equals(normalize(sound)) && sinceDeathMs >= 0 && sinceDeathMs < 850
                && (Math.abs(volume - 0.4f) < 0.015f || Math.abs(volume - 0.8f) < 0.015f);
    }

    @SubscribeEvent
    public void onSound(PlaySoundEvent event) {
        try {
            if (event == null || event.result == null || SweatsEnhancements.cfg == null) return;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.theWorld == null || mc.thePlayer == null) return;
            ItemStack held = mc.thePlayer.getHeldItem();
            if (SweatsEnhancements.cfg.getBool(Cfg.key("MS212"), false) && held != null
                    && isJerrySound(event.name, held.getDisplayName())) {
                event.result = null;
                return;
            }
            if (!SweatsEnhancements.cfg.getBool(Cfg.key("MS215"), false)) return;
            long now = System.currentTimeMillis();
            if ("mob.zombie.death".equals(normalize(event.name))) {
                lastZombieDeath = now;
            } else if (lastZombieDeath > 0 && isDropChime(event.name,
                    event.result.getVolume(), now - lastZombieDeath)) {
                event.result = null;
                lastZombieDeath = 0;
            }
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] sound tweak failed: " + t);
        }
    }
}
