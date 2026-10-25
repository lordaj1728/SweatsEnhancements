package com.sweatsenhancements.feature;

import java.util.Locale;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Optional sound-only tweak; never touches movement, packets or unrelated sounds. */
public final class MuteTeleportItemSound {
    public static boolean shouldMute(String soundName, String heldName) {
        if (soundName == null || heldName == null) return false;
        String sound = soundName.toLowerCase(Locale.ROOT);
        if (!"mob.endermen.portal".equals(sound)
                && !"minecraft:mob.endermen.portal".equals(sound)) return false;
        String plain = EnumChatFormatting.getTextWithoutFormattingCodes(heldName);
        if (plain == null) return false;
        String name = plain.toLowerCase(Locale.ROOT);
        return name.contains("aspect of the end") || name.contains("aspect of the void");
    }

    @SubscribeEvent
    public void onSound(PlaySoundEvent event) {
        try {
            if (event == null || event.result == null || SweatsEnhancements.cfg == null
                    || !SweatsEnhancements.cfg.getBool(Cfg.key("MS206"), false)) return;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.theWorld == null || mc.thePlayer == null) return;
            ItemStack held = mc.thePlayer.getHeldItem();
            if (held != null && shouldMute(event.name, held.getDisplayName())) event.result = null;
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] teleport sound filter failed: " + t);
        }
    }
}
