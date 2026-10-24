import com.sweatsenhancements.feature.ClickRateHud;
import com.sweatsenhancements.feature.AutoCloseSecretChest;
import com.sweatsenhancements.config.Implemented;
import com.sweatsenhancements.config.Features;
import com.sweatsenhancements.feature.HudLayout;
import com.sweatsenhancements.cmd.SweatsCommands;
import com.mojang.brigadier.CommandDispatcher;

public class SmallQolTest {
    static void ok(boolean b, String why) { if (!b) throw new AssertionError(why); }
    public static void main(String[] args) {
        for (String id : new String[]{"MS224", "MS225", "MS226"})
            ok(Features.byId(id) != null && Implemented.IDS.contains(id)
                    && Implemented.DEFAULT_OFF.contains(id), "registered default-off " + id);
        ok(AutoCloseSecretChest.shouldClose("Secret Chest"), "exact secret chest");
        ok(!AutoCloseSecretChest.shouldClose("Secret Chest Rewards")
                && !AutoCloseSecretChest.shouldClose("Large Chest")
                && !AutoCloseSecretChest.shouldClose("Your contests"), "never close other chests");
        ClickRateHud.reset();
        ClickRateHud.record(1000); ClickRateHud.record(1600); ClickRateHud.record(1601);
        ok(ClickRateHud.count(1999) == 3, "rolling window");
        ok(ClickRateHud.count(2000) == 2, "first click expired");
        ok(ClickRateHud.count(3000) == 0, "no stale click");
        ok(HudLayout.byKey("MS226") != null, "CPS HUD movable and resizable");
        CommandDispatcher<Object> d = new CommandDispatcher<Object>();
        SweatsCommands.register(d);
        ok(d.parse("sweatsenhancements safeguard", null).getReader().canRead() == false
                && d.parse("sweatsenhancements safeguard", null).getContext().getCommand() != null,
                "safeguard command, not the borrowed standalone name");
        ok(d.parse("sweatsenhancementsprotect", null).getContext().getCommand() == null, "old command removed");
        System.out.println("  New QoL: exact chest, rolling CPS, HUD and renamed safeguard command OK");
    }
}
