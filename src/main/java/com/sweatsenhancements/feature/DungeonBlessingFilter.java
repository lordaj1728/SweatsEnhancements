package com.sweatsenhancements.feature;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import java.util.Locale;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Opt-in filter for server blessing announcements, never action bar or player chat. */
public final class DungeonBlessingFilter {
    public static boolean isAnnouncement(String text) {
        if (text == null) return false;
        String plain = EnumChatFormatting.getTextWithoutFormattingCodes(text);
        if (plain == null) return false;
        String s = plain.trim().toLowerCase(Locale.ROOT);
        // Keep this narrow. In particular, do not hide a player's message containing "blessing".
        return s.matches("(?:[a-z0-9_]{3,16} found a |you found a )blessing of (?:power|time|wisdom|stone|life)(?: [ivxlcdm]+)?[!.]?")
                || s.matches("blessing of (?:power|time|wisdom|stone|life) (?:has been |was )found[!.]?");
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (event == null || event.isCanceled() || event.type != 0 || event.message == null
                || SweatsEnhancements.cfg == null || !SweatsEnhancements.cfg.getBool(Cfg.key("DU021"), false)
                || !DungeonRunHud.isInDungeon()) return;
        if (isAnnouncement(event.message.getUnformattedText())) event.setCanceled(true);
    }
}
