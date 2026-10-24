package com.arena.pekaclient.feature;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import java.util.List;

/**
 * Finds the server-opened Spirit Leap menu and its teammate slots.
 *
 * Sky VeLor only reads the menu that the server opens. It never sends a command or a packet of its own. A
 * teammate is a player-head item in the menu's own chest slots. Clicks go through the normal window-click path,
 * so the server still validates every one.
 *
 * Detection is not based on the title alone. The menu is recognised when either:
 *  - its title is "Spirit Leap", or
 *  - the player has just used a Spirit Leap item, is in a dungeon by the sidebar, and the chest holds at least
 *    one teammate head.
 * The second rule covers a server whose title differs. Any other chest is left alone.
 */
public final class SpiritLeap {

    /** How long after using a Spirit Leap item a chest can still count as the leap menu. */
    /** A chest opened this soon after a left-click on a Leap item is treated as the Leap menu. */
    public static final long USE_WINDOW_MS = 3000L;

    private SpiritLeap() {
    }

    /**
     * The decision, separated from the game types so it can be tested. {@code titleIsLeap} is the title check.
     * {@code recentUse} is whether a Spirit Leap item was used within {@link #USE_WINDOW_MS}.
     */
    public static boolean decide(boolean titleIsLeap, boolean recentUse, boolean inDungeon, int teammateHeads) {
        if (titleIsLeap) {
            return true;
        }
        return recentUse && inDungeon && teammateHeads > 0;
    }

    /** True when the container is the Spirit Leap menu, by either rule. */
    public static boolean isLeapMenu(Container container) {
        if (!(container instanceof ContainerChest)) {
            return false;
        }
        IInventory lower = ((ContainerChest) container).getLowerChestInventory();
        boolean title = lower != null && lower.getDisplayName() != null
                && LeapText.isLeapTitle(lower.getDisplayName().getUnformattedText());
        if (title) {
            return true;
        }
        // Without the title, only a recent left-click on a held Leap item counts.
        if (!LeapUse.recent(USE_WINDOW_MS) || !DungeonContext.inDungeon()) {
            return false;
        }
        return decide(false, true, true, teammateSlots(container).size());
    }

    /**
     * The teammate slots of an open Spirit Leap menu, in slot order. Only non-empty player-head slots in the
     * menu's own inventory are listed, so the player's own inventory is never shown.
     */
    public static void teammateSlots(Container container, List<Slot> out) {
        out.clear();
        if (!(container instanceof ContainerChest)) {
            return;
        }
        IInventory lower = ((ContainerChest) container).getLowerChestInventory();
        for (Slot s : container.inventorySlots) {
            if (s.inventory != lower || !s.getHasStack()) {
                continue;
            }
            ItemStack stack = s.getStack();
            if (stack != null && stack.getItem() == Items.skull) {
                out.add(s);
            }
        }
    }

    /** Convenience form that allocates a new list. Not used on the per-frame path. */
    public static List<Slot> teammateSlots(Container container) {
        List<Slot> out = new java.util.ArrayList<Slot>();
        teammateSlots(container, out);
        return out;
    }
}
