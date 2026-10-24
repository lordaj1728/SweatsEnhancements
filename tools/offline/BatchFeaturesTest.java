import java.io.File;
import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import com.sweatsenhancements.feature.BatchSoundTweaks;
import com.sweatsenhancements.feature.PreventAbilityPlacement;
import com.sweatsenhancements.feature.ServerAnnouncementFilter;
import com.sweatsenhancements.feature.SkyblockMenuMiddleClick;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.ClientChatReceivedEvent;

public class BatchFeaturesTest {
    private static void check(boolean value, String name) {
        if (!value) throw new AssertionError(name);
    }
    private static ClientChatReceivedEvent message(byte type, String text) {
        // Forge's @Cancelable transformer runs in game, not in this standalone harness.
        return new ClientChatReceivedEvent(type, new ChatComponentText(text)) {
            @Override public boolean isCancelable() { return true; }
        };
    }
    public static void main(String[] args) {
        SweatsEnhancements.cfg = new Cfg(new File("cfg-batch"));
        SweatsEnhancements.cfg.load();
        for (String id : new String[]{"CH206", "MS212", "MS213", "MS214", "MS215"}) {
            check(com.sweatsenhancements.config.Features.byId(id) != null
                && com.sweatsenhancements.config.Implemented.IDS.contains(id)
                && com.sweatsenhancements.config.Implemented.DEFAULT_OFF.contains(id), "registered " + id);
        }
        check(ServerAnnouncementFilter.isAnnouncement("§7[VISIT THE STORE] Click here"), "visit notice");
        check(ServerAnnouncementFilter.isAnnouncement("Protect yourself from scams!"), "scam notice");
        check(!ServerAnnouncementFilter.isAnnouncement("§eYour §6MVP§c++ §erank expires in §a10 hours!"),
                "personal rank expiry warning is preserved");
        check(!ServerAnnouncementFilter.isAnnouncement("§8* §eAfter your rank expires, your account will return to your previous rank\n§8* §eYou can buy a new rank at the store!"),
                "do not discard the whole personal rank reminder");
        check(!ServerAnnouncementFilter.isAnnouncement("[VIP] Alex: Use /help before trading"), "player chat");
        check(!ServerAnnouncementFilter.isAnnouncement("Alex invites you to play BedWars"), "invite separate");
        ServerAnnouncementFilter chat = new ServerAnnouncementFilter();
        ClientChatReceivedEvent off = message((byte)0,"[VISIT THE STORE] Click here");
        chat.onChat(off);
        check(!off.isCanceled(), "announcement off");
        SweatsEnhancements.cfg.setBool(Cfg.key("CH206"),true);
        ClientChatReceivedEvent on = message((byte)0,"[VISIT THE STORE] Click here");
        chat.onChat(on);
        check(on.isCanceled(), "announcement on");
        ClientChatReceivedEvent action = message((byte)2,"[VISIT THE STORE] Click here");
        chat.onChat(action);
        check(!action.isCanceled(), "action bar unchanged");
        String reportedAdvert = "§7\n §fA special §6discount §fis now available on the store.\n"
                + " §fGet §635% OFF §fon §6ranks§f, §6boxes §fand §6SkyBlock gems§f.\n"
                + " §fUpgrade your gameplay while the offer lasts.\n §7[OPEN STORE]";
        check(ServerAnnouncementFilter.isAnnouncement(reportedAdvert), "real multiline store advert");
        ClientChatReceivedEvent liveShape = message((byte)0, reportedAdvert);
        // Another chat component may add a copy button before SweatsEnhancements sees the line.
        liveShape.message.appendSibling(new ChatComponentText(" §r§b§l[COPY]"));
        chat.onChat(liveShape);
        check(liveShape.isCanceled(), "real store advert suppressed with switch enabled");
        check(!ServerAnnouncementFilter.isAnnouncement("[VIP] Alex: A special discount is available"),
                "player's promotion is not a server announcement");
        check(BatchSoundTweaks.isJerrySound("mob.villager.yes", "§6Jerry-chine Gun"), "Jerry sound");
        check(!BatchSoundTweaks.isJerrySound("mob.villager.yes", "Villager Sword"), "other item sound");
        check(BatchSoundTweaks.isDropChime("note.pling", 0.4f, 200), "zombie drop chime");
        check(!BatchSoundTweaks.isDropChime("note.pling", 1f, 200), "other chime volume");
        check(!BatchSoundTweaks.isDropChime("note.pling", .4f, 900), "other chime time");
        check(SkyblockMenuMiddleClick.isSupportedMenu("SkyBlock Menu"), "menu whitelist");
        check(!SkyblockMenuMiddleClick.isSupportedMenu("Large Chest"), "chest safety");
        check(PreventAbilityPlacement.shouldCancel("§6Superboom TNT", false, false), "TNT guard");
        check(PreventAbilityPlacement.shouldCancel("Flower of Truth", true, false), "flower guard");
        check(!PreventAbilityPlacement.shouldCancel("Flower of Truth", false, false), "ordinary right click");
        check(!PreventAbilityPlacement.shouldCancel("Weird Tuba", false, false), "no hopper guard");
        System.out.println("  Next five: independent config, conservative sample matches and safeguards OK");
    }
}
