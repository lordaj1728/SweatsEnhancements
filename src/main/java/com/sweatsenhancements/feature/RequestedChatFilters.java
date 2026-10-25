package com.sweatsenhancements.feature;

import java.util.Locale;
import java.util.regex.Pattern;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Five separately switchable, conservative chat filters. No action-bar filtering. */
public final class RequestedChatFilters {
    private static final Pattern FRIEND_STATUS = Pattern.compile(
            "^friend\\s*(?:>|▶)\\s*.*\\b(joined|left|online|offline)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern ZOMBIE_DROP = Pattern.compile(
            "\\brare drop!\\s*(?:poisonous potato|potato|carrot)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLAYER_CHAT = Pattern.compile(
            "^(?:\\[[A-Za-z0-9_+ -]{1,30}\\]\\s*){0,4}[A-Za-z0-9_]{3,16}:\\s*(.*)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SELLING_RANKS = Pattern.compile(
            "\\bselling\\b.{0,100}\\branks?\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern INVITATION = Pattern.compile(
            "\\binvites you to play\\b", Pattern.CASE_INSENSITIVE);

    /** Return the one feature ID that owns this message, or null to leave it alone. */
    public static String matchingFeature(String message) {
        if (message == null) return null;
        String plain = EnumChatFormatting.getTextWithoutFormattingCodes(message);
        if (plain == null) return null;
        String line = plain.trim();
        if (line.isEmpty()) return null;
        String lower = line.toLowerCase(Locale.ROOT);
        if (lower.startsWith("[watchdog announcement]")
                || lower.startsWith("[player information]")) return "CH201";
        if (FRIEND_STATUS.matcher(line).find()) return "CH202";
        if (ZOMBIE_DROP.matcher(line).find()) return "CH203";
        java.util.regex.Matcher player = PLAYER_CHAT.matcher(line);
        if (player.matches() && SELLING_RANKS.matcher(player.group(1)).find()) return "CH204";
        if (INVITATION.matcher(line).find()) return "CH205";
        return null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onChat(ClientChatReceivedEvent event) {
        try {
            if (event == null || event.message == null || event.type == 2 || SweatsEnhancements.cfg == null) return;
            String id = matchingFeature(event.message.getUnformattedText());
            if (id != null && SweatsEnhancements.cfg.getBool(Cfg.key(id), false)) event.setCanceled(true);
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] requested chat filter failed: " + t);
        }
    }
}
