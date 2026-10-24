import java.io.File;
import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import com.sweatsenhancements.feature.ProtectedItems;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class ProtectedItemsTest {
    static void ok(boolean b, String message) { if (!b) throw new AssertionError(message); }
    static ItemStack item(String uuid) {
        ItemStack s = new ItemStack((net.minecraft.item.Item) null);
        NBTTagCompound tag = new NBTTagCompound(), ea = new NBTTagCompound();
        ea.setString("uuid", uuid); tag.setTag("ExtraAttributes", ea); s.setTagCompound(tag); return s;
    }
    static ItemStack itemWithId(String id) {
        ItemStack s = new ItemStack((net.minecraft.item.Item) null);
        NBTTagCompound root = new NBTTagCompound(), attrs = new NBTTagCompound();
        attrs.setString("id", id); root.setTag("ExtraAttributes", attrs); s.setTagCompound(root); return s;
    }
    public static void main(String[] args) {
        ok(com.sweatsenhancements.config.Implemented.DEFAULT_OFF.contains("MS222"), "opt-in");
        SweatsEnhancements.cfg = new Cfg(new File("cfg-protection")); SweatsEnhancements.cfg.load();
        ProtectedItems.load(new File("cfg-protection"));
        String uuid = "b9f83244-496b-4940-b254-859676207bf1";
        ItemStack exact = item(uuid), moved = item(uuid), other = item("e7732a44-496b-4940-b254-859676207bf1");
        ok(!ProtectedItems.isProtected(exact), "off initially");
        ok(Boolean.TRUE.equals(ProtectedItems.toggle(exact)), "protect unique item");
        SweatsEnhancements.cfg.setBool(Cfg.key("MS222"), true);
        ok(ProtectedItems.isProtected(moved), "protection follows item after slot movement");
        ok(!ProtectedItems.isProtected(other), "different UUID not protected");
        ok(ProtectedItems.shouldBlock(-999, 0, 0, exact, null, null, false), "cannot throw cursor item");
        ok(ProtectedItems.shouldBlock(8, 0, 4, null, exact, null, false), "cannot Q from a slot");
        ok(ProtectedItems.shouldBlock(8, 1, 0, exact, null, null, true), "cannot sell cursor item");
        ok(ProtectedItems.shouldBlock(8, 0, 2, null, other, exact, true), "cannot hotbar-swap into trade");
        ok(!ProtectedItems.shouldBlock(8, 0, 0, null, exact, null, false), "normal move allowed");
        ok(!ProtectedItems.shouldBlock(8, 0, 0, null, other, null, true), "other item allowed");
        // Unknown NPC names are not a safety signal: all non-player GUI clicks pass risky=true.
        ok(ProtectedItems.shouldBlock(8, 0, 0, null, exact, null, true), "unknown NPC GUI must block protected slot");
        ok(ProtectedItems.shouldBlock(8, 0, 1, null, exact, null, true), "unknown NPC GUI shift-click must block sale");
        ok(ProtectedItems.shouldBlock(8, 0, 0, exact, null, null, true), "unknown NPC GUI cursor must block sale");
        ok(!ProtectedItems.shouldBlock(8, 0, 0, null, exact, null, false), "player inventory still movable");
        ItemStack sellAll = new ItemStack(new net.minecraft.item.Item() {});
        sellAll.setStackDisplayName("§6Sell Inventory");
        ok(ProtectedItems.bulkSellButton(sellAll), "detect sell-inventory button");
        ItemStack ordinary = new ItemStack(new net.minecraft.item.Item() {});
        ordinary.setStackDisplayName("§6Open Wardrobe");
        ok(!ProtectedItems.bulkSellButton(ordinary), "ordinary menu button not bulk sell");
        ok(ProtectedItems.itemUuid(new ItemStack((net.minecraft.item.Item) null)) == null, "no name-based guessing");
        ProtectedItems.load(new File("cfg-protection"));
        ok(ProtectedItems.isProtected(exact), "persists across restart");
        ok(Boolean.FALSE.equals(ProtectedItems.toggle(exact)), "unprotect item");
        ok(!ProtectedItems.isProtected(exact), "removed protection");
        net.minecraft.item.Item material = new net.minecraft.item.Item() {};
        ItemStack plain = new ItemStack(material), twin = new ItemStack(material), changed = new ItemStack(material);
        changed.setItemDamage(1);
        ok(Boolean.TRUE.equals(ProtectedItems.toggle(plain)), "UUID-less item accepted by Safeguard");
        ok(ProtectedItems.isProtected(twin), "UUID-less identical copy protected");
        ok(!ProtectedItems.isProtected(changed), "different metadata not protected");
        ItemStack tagged = new ItemStack(material);
        NBTTagCompound differentTag = new NBTTagCompound();
        differentTag.setString("marker", "different");
        tagged.setTagCompound(differentTag);
        ok(!ProtectedItems.isProtected(tagged), "different NBT not protected");
        ok(ProtectedItems.shouldBlock(8, 0, 0, null, twin, null, true), "UUID-less sale blocked");
        ProtectedItems.load(new File("cfg-protection"));
        ok(ProtectedItems.isProtected(twin), "fingerprint persists after reload");
        ok(Boolean.FALSE.equals(ProtectedItems.toggle(plain)), "UUID-less rule can be removed");
        ok(!ProtectedItems.isProtected(twin), "removed fingerprint no longer protects copies");
        ok(com.sweatsenhancements.config.Implemented.DEFAULT_OFF.contains("MS223"), "smooth view opt-in");
        ok(com.sweatsenhancements.feature.SmoothTeleportView.ease(-1) == 0 && com.sweatsenhancements.feature.SmoothTeleportView.ease(0.5) == 0.5
                && com.sweatsenhancements.feature.SmoothTeleportView.ease(5) == 1, "bounded smoothstep");
        ok(com.sweatsenhancements.feature.SmoothTeleportView.isTeleportItem(itemWithId("ASPECT_OF_THE_VOID")), "AOTV item ID");
        ok(!com.sweatsenhancements.feature.SmoothTeleportView.isTeleportItem(itemWithId("DIAMOND_SWORD")), "not every item");
        System.out.println("  Unique item protection: UUID, safe moves, drop/sell, swap and persistence OK");
    }
}
