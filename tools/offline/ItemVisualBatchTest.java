import java.util.ArrayList;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import com.sweatsenhancements.feature.HideFlamingFists;
import com.sweatsenhancements.feature.UnclaimedContestHighlight;
import com.sweatsenhancements.feature.ItemDataTooltips;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

public class ItemVisualBatchTest {
    private static void check(boolean ok, String name) {
        if (!ok) throw new AssertionError(name);
    }
    public static void main(String[] args) {
        for (String id : new String[] {"MS216", "MS217", "MS218", "MS219", "MS220"}) {
            check(com.sweatsenhancements.config.Features.byId(id) != null
                && com.sweatsenhancements.config.Implemented.IDS.contains(id)
                && com.sweatsenhancements.config.Implemented.DEFAULT_OFF.contains(id), "registered " + id);
        }
        check(UnclaimedContestHighlight.isContestMenu("Your contests (1/3)"), "contest title");
        check(!UnclaimedContestHighlight.isContestMenu("Large Chest"), "ordinary chest");
        NBTTagList lore = new NBTTagList();
        lore.appendTag(new NBTTagString("§aClick to claim reward!"));
        check(UnclaimedContestHighlight.rewardStateLore(lore) == 1, "green claimable");
        lore = new NBTTagList();
        lore.appendTag(new NBTTagString("§cRewards claimed!"));
        check(UnclaimedContestHighlight.rewardStateLore(lore) == 2, "red claimed");
        NBTTagList altLore = new NBTTagList();
        altLore.appendTag(new NBTTagString("§cAlready Claimed"));
        check(UnclaimedContestHighlight.rewardStateLore(altLore) == 2, "alternate claimed phrasing");
        check(com.sweatsenhancements.feature.ActiveWardrobeHighlight.isEquippedLabel("§aEquipped"), "wardrobe active name");
        check(!com.sweatsenhancements.feature.ActiveWardrobeHighlight.isEquippedLabel("Empty Slot"), "wardrobe empty name");
        String texture = "{\"textures\":{\"SKIN\":{\"url\":\"https://textures.minecraft.net/texture/7717933c40fbf936aa9288513efe19bda4601efc0e4ecad2e023b0c1d28444b\"}}}";
        check(HideFlamingFists.matchesEncodedTexture(Base64.getEncoder().encodeToString(
                texture.getBytes(StandardCharsets.UTF_8))), "specific skull texture");
        check(!HideFlamingFists.matchesEncodedTexture("not-base64"), "bad texture");
        check(!HideFlamingFists.matchesEncodedTexture(Base64.getEncoder().encodeToString(
                "other skull".getBytes(StandardCharsets.UTF_8))), "other skull");
        NBTTagCompound attrs = new NBTTagCompound();
        attrs.setString("id", "CUSTOM_ITEM");
        attrs.setLong("stacking_enchant_counter", 1234567L);
        ArrayList<String> tooltip = new ArrayList<String>();
        tooltip.add("Sword");
        tooltip.add("§aCultivating VII");
        tooltip.add("§6LEGENDARY SWORD");
        ItemDataTooltips.append(tooltip, attrs, true, true, true);
        check(tooltip.size() == 6, "three additions");
        check(tooltip.get(2).contains("ADMIN ITEM") && tooltip.get(2).contains("Custom Item"), "admin reason before rarity");
        check(tooltip.get(3).contains("Cultivating: ") && tooltip.get(3).contains("1,234,567")
                && tooltip.get(3).contains("3,500,000"), "stacking threshold");
        check(tooltip.get(4).contains("LEGENDARY SWORD"), "rarity remains after additions");
        check(tooltip.get(5).contains("CUSTOM_ITEM"), "SweatsEnhancements-only internal ID");
        ArrayList<String> max = new ArrayList<String>();
        max.add("Sword");
        max.add("Compact X");
        NBTTagCompound full = new NBTTagCompound();
        full.setString("id", "DIAMOND_PICKAXE");
        full.setLong("stacking_enchant_counter", 500000L);
        ItemDataTooltips.append(max, full, false, true, false);
        check(max.get(2).contains("MAX LEVEL") && !max.get(2).contains("Next Level at 0"), "honest max level");
        NBTTagCompound strange = new NBTTagCompound();
        strange.setString("id", "DIAMOND_SWORD");
        strange.setInteger("stars", 6);
        check("Stars".equals(ItemDataTooltips.adminReason(strange)), "metadata reason");
        NBTTagCompound badge = new NBTTagCompound();
        badge.setString("id", "CAMPFIRE_TALISMAN_23");
        badge.setString("origin", "ALL_ITEMS_GUI_ACTOR_Regasky");
        check("Actor origin".equals(ItemDataTooltips.adminReason(badge)),
                "badge actor origin in user's dump is recognised");
        ArrayList<String> badgeTooltip = new ArrayList<String>();
        badgeTooltip.add("Campfire God Badge");
        badgeTooltip.add("LEGENDARY ACCESSORY");
        ItemDataTooltips.append(badgeTooltip, badge, false, false, true);
        check(badgeTooltip.size() == 3 && badgeTooltip.get(1).contains("ADMIN ITEM")
                && badgeTooltip.get(1).contains("Regasky"), "badge gets actor warning above rarity");
        NBTTagCompound ordinary = new NBTTagCompound();
        ordinary.setString("id", "NORMAL_ITEM");
        check(ItemDataTooltips.adminReason(ordinary) == null, "ordinary item not tagged");
        ArrayList<String> off = new ArrayList<String>();
        ItemDataTooltips.append(off, attrs, false, false, false);
        check(off.isEmpty(), "off leaves tooltip unchanged");
        com.sweatsenhancements.config.Cfg old = new com.sweatsenhancements.config.Cfg(new java.io.File("cfg-admin-migration"));
        old.load();
        old.setBool(com.sweatsenhancements.config.Cfg.key("MS220"), true);
        com.sweatsenhancements.config.Migrate.run(old);
        check(!old.getBool(com.sweatsenhancements.config.Cfg.key("MS220"), true), "old narrow tag reset on broadened upgrade");
        old.setBool(com.sweatsenhancements.config.Cfg.key("MS220"), true);
        com.sweatsenhancements.config.Migrate.run(old);
        check(old.getBool(com.sweatsenhancements.config.Cfg.key("MS220"), false), "later admin-tag choice respected");
        System.out.println("  Item/visual batch: claimed/unclaimed, admin reason, stacking tiers, skull metadata and migration OK");
    }
}
