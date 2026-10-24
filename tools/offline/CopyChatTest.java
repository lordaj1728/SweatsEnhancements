import java.io.File;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import com.sweatsenhancements.feature.CopyChatMessage;

import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;

public class CopyChatTest {
    private static void check(boolean value, String label) {
        if (!value) throw new AssertionError(label);
    }

    public static void main(String[] args) {
        SweatsEnhancements.cfg = new Cfg(new File("cfg-copy"));
        check(com.sweatsenhancements.config.Implemented.IDS.contains("CH200")
                && com.sweatsenhancements.config.Implemented.DEFAULT_OFF.contains("CH200")
                && com.sweatsenhancements.config.Features.byId("CH200") != null, "copy chat switch");
        CopyChatMessage listener = new CopyChatMessage();
        ClientChatReceivedEvent off = new ClientChatReceivedEvent((byte)0,
                new ChatComponentText("Hello"));
        listener.onChat(off);
        check(off.message.getSiblings().isEmpty(), "off adds no button");

        SweatsEnhancements.cfg.setBool(Cfg.key("CH200"), true);
        ClientChatReceivedEvent active = new ClientChatReceivedEvent((byte)0,
                new ChatComponentText("§aHello friend"));
        listener.onChat(active);
        check(active.message.getSiblings().size() == 1, "one button");
        IChatComponent button = active.message.getSiblings().get(0);
        ClickEvent click = button.getChatStyle().getChatClickEvent();
        check(click != null && click.getAction() == ClickEvent.Action.SUGGEST_COMMAND
                && "Hello friend".equals(click.getValue()), "1.8.9 chat-input default");
        check(button.getUnformattedText().contains("✍"), "compact icon default");

        SweatsEnhancements.cfg.set("chat.copyButtonStyle", "[COPY]");
        SweatsEnhancements.cfg.set("chat.copyAction", "Clipboard");
        ClientChatReceivedEvent labelled = new ClientChatReceivedEvent((byte)0,
                new ChatComponentText("§eHello again"));
        listener.onChat(labelled);
        check(labelled.message.getSiblings().size() == 1
                && labelled.message.getSiblings().get(0).getUnformattedText().contains("[COPY]"),
                "NEF-style label option");
        ClickEvent local = labelled.message.getSiblings().get(0).getChatStyle().getChatClickEvent();
        check(local.getAction() == ClickEvent.Action.RUN_COMMAND
                && local.getValue().startsWith("/se-copy "), "local clipboard command");
        long id = Long.parseLong(local.getValue().substring("/se-copy ".length()));
        check("Hello again".equals(CopyChatMessage.find(id)), "full plain text cached");

        SweatsEnhancements.cfg.set("chat.copyAction", "Chat Input");
        ClientChatReceivedEvent suggest = new ClientChatReceivedEvent((byte)0,
                new ChatComponentText("Meet at spawn\nthen visit the mines"));
        listener.onChat(suggest);
        ClickEvent suggestion = suggest.message.getSiblings().get(0).getChatStyle().getChatClickEvent();
        check(suggestion.getAction() == ClickEvent.Action.SUGGEST_COMMAND
                && "Meet at spawn then visit the mines".equals(suggestion.getValue()),
                "newline sanitization");
        String longLine = new String(new char[120]).replace('\0', 'x');
        check(CopyChatMessage.suggestedText(longLine).length() == 120,
                "SweatsEnhancements must not pre-truncate full 1.8.9 suggestion");
        ChatComponentText nested = new ChatComponentText("§aHello ");
        nested.appendSibling(new ChatComponentText("§cfriend"));
        listener.onChat(new ClientChatReceivedEvent((byte)0, nested));
        check("Hello friend".equals(nested.getSiblings().get(1).getChatStyle()
                .getChatClickEvent().getValue()), "nested formatted components");

        ClientChatReceivedEvent action = new ClientChatReceivedEvent((byte)2,
                new ChatComponentText("Action bar"));
        listener.onChat(action);
        check(action.message.getSiblings().isEmpty(), "action bar untouched");
        ClientChatReceivedEvent blank = new ClientChatReceivedEvent((byte)0,
                new ChatComponentText("   "));
        listener.onChat(blank);
        check(blank.message.getSiblings().isEmpty(), "blank untouched");
        for (int i = 0; i < 130; i++) CopyChatMessage.remember("line " + i);
        check(CopyChatMessage.find(id) == null, "old messages evicted");

        SweatsEnhancements.cfg.set("chat.copyAction", "Clipboard");
        com.sweatsenhancements.config.Migrate.run(SweatsEnhancements.cfg);
        check("Chat Input".equals(SweatsEnhancements.cfg.get("chat.copyAction", "")), "one-time default migration");
        SweatsEnhancements.cfg.set("chat.copyAction", "Clipboard");
        com.sweatsenhancements.config.Migrate.run(SweatsEnhancements.cfg);
        check("Clipboard".equals(SweatsEnhancements.cfg.get("chat.copyAction", "")), "later user choice respected");
        System.out.println("  Copy Chat: 1.8.9 full suggestion, icon/label, optional clipboard, one-time default migration OK");
    }
}
