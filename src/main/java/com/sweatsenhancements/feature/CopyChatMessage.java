package com.sweatsenhancements.feature;

import java.util.LinkedHashMap;
import java.util.Map;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;

import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Click-to-copy chat suffix. Keeps bounded local text; never embeds it in a command. */
public final class CopyChatMessage {
    private static final int LIMIT = 128;
    private static final Map<Long, String> MESSAGES = new LinkedHashMap<Long, String>();
    private static long nextId = 1;

    public static synchronized long remember(String text) {
        if (text == null || text.trim().isEmpty()) return -1;
        long id = nextId++;
        MESSAGES.put(Long.valueOf(id), text);
        if (MESSAGES.size() > LIMIT) MESSAGES.remove(MESSAGES.keySet().iterator().next());
        return id;
    }

    public static synchronized String find(long id) {
        return MESSAGES.get(Long.valueOf(id));
    }

    /** Suggest the whole message, folding line breaks into spaces. The chat GUI applies its own limit. */
    public static String suggestedText(String text) {
        return text == null ? "" : text.replaceAll("[\\r\\n\\u2028\\u2029]+", " ");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onChat(ClientChatReceivedEvent event) {
        try {
            if (event == null || event.isCanceled() || event.type == 2 || event.message == null
                    || SweatsEnhancements.cfg == null || !SweatsEnhancements.cfg.getBool(Cfg.key("CH200"), false)) return;
            // Read the original formatted component before adding our own button; stripping
            // colour codes preserves the message's full visible text across nested siblings.
            String plain = EnumChatFormatting.getTextWithoutFormattingCodes(event.message.getFormattedText());
            if (plain == null || plain.trim().isEmpty()) return;
            String style = SweatsEnhancements.cfg.get("chat.copyButtonStyle", "✍");
            String mode = SweatsEnhancements.cfg.get("chat.copyAction", "Chat Input");
            boolean suggest = "Chat Input".equals(mode);
            ClickEvent click;
            if (suggest) {
                click = new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, suggestedText(plain));
            } else {
                long id = remember(plain);
                if (id < 0) return;
                click = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/se-copy " + id);
            }
            String symbol = "[COPY]".equals(style) ? "§b§l[COPY]" : "§8✍";
            ChatComponentText button = new ChatComponentText(" §r" + symbol);
            button.setChatStyle(new ChatStyle()
                    .setChatClickEvent(click)
                    .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new ChatComponentText(EnumChatFormatting.AQUA + (suggest
                                    ? "Put message in chat input" : "Copy message to clipboard")))));
            event.message.appendSibling(button);
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] copy chat button failed: " + t);
        }
    }
}
