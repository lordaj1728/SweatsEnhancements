import java.io.File;
import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import com.sweatsenhancements.feature.RequestedChatFilters;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.ClientChatReceivedEvent;

public class RequestedChatFiltersTest {
    private static void check(boolean ok, String name) {
        if (!ok) throw new AssertionError(name);
    }

    /** Forge patches @Cancelable during game loading; the standalone harness lacks that transformer. */
    private static ClientChatReceivedEvent event(byte type, String line) {
        return new ClientChatReceivedEvent(type, new ChatComponentText(line)) {
            @Override public boolean isCancelable() { return true; }
        };
    }

    public static void main(String[] args) {
        String[][] samples = {
            {"§4[WATCHDOG ANNOUNCEMENT]\nSuspicious activity was detected", "CH201"},
            {"§b[PLAYER INFORMATION]\nCheck the rules", "CH201"},
            {"§aFriend > Steve joined the game", "CH202"},
            {"Friend > Alex left the game", "CH202"},
            {"§aFriend §8▶ §fOnline Friends on Fakepixel:\n §ePekaDeco is playing on SkyBlock Island", "CH202"},
            {"Friend ▶ Alex is now online", "CH202"},
            {"§6§lRARE DROP! §fPoisonous Potato", "CH203"},
            {"RARE DROP! Carrot", "CH203"},
            {"[VIP] Alex: selling a rank cheap", "CH204"},
            {"§fAlex invites you to play §6BedWars Solo", "CH205"}
        };
        SweatsEnhancements.cfg = new Cfg(new File("cfg-chat"));
        SweatsEnhancements.cfg.load();
        RequestedChatFilters filter = new RequestedChatFilters();
        for (String[] sample : samples) {
            String id = sample[1];
            check(id.equals(RequestedChatFilters.matchingFeature(sample[0])), "match " + id);
            check(com.sweatsenhancements.config.Features.byId(id) != null
                && com.sweatsenhancements.config.Implemented.IDS.contains(id)
                && com.sweatsenhancements.config.Implemented.DEFAULT_OFF.contains(id), "registered " + id);
            ClientChatReceivedEvent off = event((byte) 0, sample[0]);
            filter.onChat(off);
            check(!off.isCanceled(), "off must not cancel " + id);
            SweatsEnhancements.cfg.setBool(Cfg.key(id), true);
            ClientChatReceivedEvent on = event((byte) 0, sample[0]);
            filter.onChat(on);
            check(on.isCanceled(), "on must cancel " + id);
            ClientChatReceivedEvent action = event((byte) 2, sample[0]);
            filter.onChat(action);
            check(!action.isCanceled(), "action bar unchanged " + id);
            SweatsEnhancements.cfg.setBool(Cfg.key(id), false);
        }
        for (String unaffected : new String[] {
                "Friend > Alex sent you a message", "RARE DROP! Zombie Heart",
                "Alex: selling pumpkins at the bazaar", "[SERVER] Selling ranks is prohibited",
                "Alex: Please do not invite your friends to play", "Watchdog is active",
                "You have sent a friend request!", "You are now friends with PekaDeco"}) {
            check(RequestedChatFilters.matchingFeature(unaffected) == null, "preserve: " + unaffected);
        }
        System.out.println("  Requested chat filters: five opt-in toggles, sample matches, negatives, action bar OK");
    }
}
