import com.sweatsenhancements.feature.SmoothTeleportView;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

public final class SmoothTeleportTypeTest {
    static void ok(boolean value, String label) { if (!value) throw new AssertionError(label); }
    static ItemStack item(String id) {
        ItemStack stack = new ItemStack(new Item() {});
        NBTTagCompound root = new NBTTagCompound(), attributes = new NBTTagCompound();
        attributes.setString("id", id); root.setTag("ExtraAttributes", attributes);
        stack.setTagCompound(root); return stack;
    }
    public static void main(String[] args) {
        ItemStack aotv = item("ASPECT_OF_THE_VOID");
        ok(SmoothTeleportView.type(aotv, false) == SmoothTeleportView.NORMAL, "normal AOTV");
        ok(SmoothTeleportView.type(aotv, true) == SmoothTeleportView.NORMAL, "sneak without ethermerge");
        aotv.getTagCompound().getCompoundTag("ExtraAttributes").setBoolean("ethermerge", true);
        ok(SmoothTeleportView.type(aotv, true) == SmoothTeleportView.ETHERWARP, "sneak ethermerge");
        ok(SmoothTeleportView.type(aotv, false) == SmoothTeleportView.NORMAL, "unsneak merged item");
        ok(SmoothTeleportView.type(item("ASPECT_OF_THE_LEECH_2"), false) == SmoothTeleportView.NORMAL, "AOTL");
        ok(SmoothTeleportView.type(item("ETHERWARP_CONDUIT"), false) == SmoothTeleportView.ETHERWARP, "conduit");
        ItemStack hyp = item("HYPERION");
        ok(SmoothTeleportView.type(hyp, false) == SmoothTeleportView.NONE, "scroll required");
        NBTTagList scrolls = new NBTTagList(); scrolls.appendTag(new NBTTagString("SHADOW_WARP_SCROLL"));
        hyp.getTagCompound().getCompoundTag("ExtraAttributes").setTag("ability_scroll", scrolls);
        ok(SmoothTeleportView.type(hyp, false) == SmoothTeleportView.SHADOW_WARP, "shadow warp scroll");
        ItemStack adapted = item("HYPERION");
        adapted.getTagCompound().getCompoundTag("ExtraAttributes").setBoolean("shadow_warp", true);
        ok(SmoothTeleportView.type(adapted, false) == SmoothTeleportView.SHADOW_WARP, "adapted shadow warp tag");
        ok(SmoothTeleportView.type(item("DIAMOND_SWORD"), false) == SmoothTeleportView.NONE, "unrelated item");
        System.out.println("  Smooth teleport metadata: AOTE, AOTV, AOTL, Etherwarp, Shadow Warp OK");
    }
}
