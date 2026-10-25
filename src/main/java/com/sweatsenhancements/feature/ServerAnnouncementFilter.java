package com.sweatsenhancements.feature;

import java.util.Locale;
import java.util.regex.Pattern;
import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Filter only identified server promotions, after other chat listeners have rewritten components. */
public final class ServerAnnouncementFilter {
    private static final Pattern PLAYER_CHAT = Pattern.compile(
            "^(?:\\[[A-Za-z0-9_+ -]{1,30}\\]\\s*){0,4}[A-Za-z0-9_]{3,16}:\\s+.+$");
    private static final Pattern PROMO = Pattern.compile(
            "\\b(?:use /(?:help|report|streams)\\b|protect yourself from scams\\b|"
            + "spotted a cheater\\b|special discount\\b|best rank for price and benefits\\b|"
            + "is streaming on the server\\b)", Pattern.CASE_INSENSITIVE);
    private static final Pattern BUTTON = Pattern.compile(
            "(?im)^\\s*\\[(?:visit|open|get|join)[A-Za-z0-9 _+!/-]{0,70}\\][^\\r\\n]*$");
    private boolean reportedOn;
    private boolean reportedOff;

    public static boolean isAnnouncement(String input) {
        if (input == null) return false;
        String text = EnumChatFormatting.getTextWithoutFormattingCodes(input);
        if (text == null) return false;
        text = text.trim();
        if (text.isEmpty() || PLAYER_CHAT.matcher(text).matches()) return false;
        // A multi-line advert can start with a blank line and end in a clickable store button.
        // Do not require its first line to be the button or a particular discount percentage.
        return BUTTON.matcher(text).find() || PROMO.matcher(text).find();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onChat(ClientChatReceivedEvent event) {
        try {
            if (event == null || event.message == null || event.type == 2 || SweatsEnhancements.cfg == null) return;
            boolean match = isAnnouncement(event.message.getUnformattedText())
                    || isAnnouncement(event.message.getFormattedText());
            if (!match) return;
            boolean enabled = SweatsEnhancements.cfg.getBool(Cfg.key("CH206"), false);
            if (!enabled) {
                if (!reportedOff) {
                    System.out.println("[SweatsEnhancements] CH206 recognised a server announcement but switch is OFF");
                    reportedOff = true;
                }
                return;
            }
            if (!reportedOn) {
                System.out.println("[SweatsEnhancements] CH206 recognised a server announcement and is suppressing it");
                reportedOn = true;
            }
            event.setCanceled(true);
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] server announcement filter failed: " + t);
        }
    }
}
