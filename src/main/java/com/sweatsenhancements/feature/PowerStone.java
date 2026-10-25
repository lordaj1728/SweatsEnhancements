package com.sweatsenhancements.feature;

import com.sweatsenhancements.SweatsEnhancements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The accessory power and the magical power, learned and remembered.
 *
 * A player asked for the scoreboard line the reference draws - `Power: Hurtful` - and this server
 * sends no `Power:` line anywhere, which the player confirmed: "no power does not appear in the
 * tablist". The reference mods solve that by keeping it themselves, and this is the same two
 * sources, reimplemented from what they do rather than copied from them:
 *
 *   * **the tooltip on the selected power's own item** - "Power is selected!" in the lore, with the
 *     power's name as the item's name, and "Accessory Power: 1,263" on the same lore. The player
 *     corrected the first attempt at this: "the power is only learned when you change, not open the
 *     thaumaturgist, so you see the power is selected tooltip" - so the menu is the source that
 *     matters, and the earlier version missed it by insisting the menu's *title* name it as a
 *     Thaumaturgy or accessory bag screen. The menu is what it is read from, and the player then
 *     said which one: "the thaumaturgist inventory says Accessory Bag Thaumaturgy so no need to
 *     check every inventory just that one";
 *   * **the chat** - "You selected the §aX §epower for your §aAccessory Bag§e!" and "Your selected
 *     power was set to §aX§e!" - which costs nothing to watch for and catches a change the moment it
 *     happens, menu open or not.
 *
 * Both numbers are kept, because both are on that tooltip: the selected power and the accessory
 * (magical) power beside it. What is learned lives in the config file, so the line survives a
 * restart without either source ever being seen again. Nothing here is sent anywhere.
 */
public class PowerStone {

    /** where the learned power is kept */
    public static final String KEY = "power.selected";
    /** where the learned accessory power is kept */
    public static final String MAGICAL_KEY = "power.magical";

    /** the chat line you get when you pick a power in the Thaumaturgy menu */
    private static final Pattern CHAT_SELECTED = Pattern.compile(
            "(?i)you selected the (.+?) power for your accessory bag");
    /** the chat line you get when the game sets it for you */
    private static final Pattern CHAT_SET = Pattern.compile(
            "(?i)your selected power was set to (.+?)!?$");
    /** the lore line that spells the selected power out */
    private static final Pattern LORE_SELECTED = Pattern.compile("(?i)selected power:?\\s*(.+)");
    /** the lore line that carries the accessory power, e.g. "Accessory Power: 1,263" */
    private static final Pattern LORE_MAGICAL = Pattern.compile(
            "(?i)(?:accessory power|magical power):?\\s*([\\d,.]+)");
    /** the other way that same number is written out, e.g. "Total: 1,263 Accessory Power" */
    private static final Pattern LORE_TOTAL = Pattern.compile(
            "(?i)total:?\\s*([\\d,.]+)\\s*(?:accessory|magical) power");
    /** the marker in the selected item's lore that says it is the one */
    private static final String LORE_MARKER = "power is selected!";
    /**
     * What the menu this feature is about titles itself.
     *
     * The player: "the thaumaturgist inventory says Accessory Bag Thaumaturgy so no need to check
     * every inventory just that one". So only the inventory whose title carries both words is read,
     * and nothing else on the screen is looked at. The wording matters: this server's title has both
     * words in it, and the first version refused to look at anything it could not call a Thaumaturgy
     * menu, which is why opening it learned nothing.
     */
    private static final String MENU_WORD_ONE = "accessory bag";
    private static final String MENU_WORD_TWO = "thaumaturgy";

    /** the last thing that carried a power, for /se power */
    public static volatile String lastSource = "nothing yet";

    private static GuiScreen scanned;
    private static int rescanTicks;

    public PowerStone() {
    }

    // ------------------------------------------------------------------ what has been learned

    /** The power learned so far, or "" when none has been seen. */
    public static String stored() {
        return read(KEY);
    }

    /** The accessory power learned so far, or "" when none has been seen. */
    public static String magical() {
        return read(MAGICAL_KEY);
    }

    private static String read(String key) {
        try {
            String value = SweatsEnhancements.cfg == null ? null : SweatsEnhancements.cfg.get(key, "");
            return value == null ? "" : value.trim();
        } catch (Throwable t) {
            return "";
        }
    }

    /** Remembers a power, if it is one and it is not already the one. */
    public static void learn(String power, String source) {
        String name = clean(power);
        if (name.isEmpty() || name.length() > 32) return;
        try {
            if (!name.equals(stored()) && SweatsEnhancements.cfg != null) {
                SweatsEnhancements.cfg.set(KEY, name);
                SweatsEnhancements.cfg.set("power.learnedAt", String.valueOf(System.currentTimeMillis()));
                System.out.println("[SweatsEnhancements] accessory power learned: " + name + " (" + source + ")");
            }
            lastSource = source;
        } catch (Throwable t) {
            // a power that cannot be remembered is not worth a crash over
        }
    }

    /** Remembers the accessory power beside the selected one. */
    public static void learnMagical(String amount, String source) {
        String number = clean(amount);
        if (number.isEmpty() || number.length() > 12) return;
        try {
            if (!number.equals(magical()) && SweatsEnhancements.cfg != null) {
                SweatsEnhancements.cfg.set(MAGICAL_KEY, number);
                SweatsEnhancements.cfg.set("power.magicalAt", String.valueOf(System.currentTimeMillis()));
                System.out.println("[SweatsEnhancements] accessory power learned: " + number + " (" + source + ")");
            }
        } catch (Throwable t) {
            // same again: nothing here is worth a crash
        }
    }

    /** The power a chat line carries, or null. Plain text, colour codes already stripped. */
    public static String fromChat(String plain) {
        if (plain == null) return null;
        String text = clean(plain);
        if (text.isEmpty()) return null;
        Matcher selected = CHAT_SELECTED.matcher(text);
        if (selected.find()) return selected.group(1);
        Matcher set = CHAT_SET.matcher(text);
        if (set.find()) return set.group(1);
        return null;
    }

    /**
     * The power an item's lore carries, or null.
     *
     * Two shapes, from the two references: the marker line the selected power's own item carries -
     * whose name is the power's name - and the name written out on a `Selected Power:` line. The
     * player sees the marker in the tooltip, so it is the one that has to work.
     */
    public static String fromLore(List<String> lore, String itemName) {
        if (lore == null) return null;
        String spelled = null;
        boolean marked = false;
        for (String line : lore) {
            String text = clean(line);
            if (text.isEmpty()) continue;
            if (text.toLowerCase(Locale.ROOT).equals(LORE_MARKER)) marked = true;
            Matcher m = LORE_SELECTED.matcher(text);
            if (m.find()) spelled = m.group(1);
        }
        if (marked) {
            // the item is called "Hurtful Power Stone" and the power is "Hurtful": the menu names
            // the item after the power, so the item's own name has to have that tail taken off it
            String name = powerName(clean(itemName));
            if (!name.isEmpty()) return name;
        }
        return spelled;
    }

    /** "Hurtful Power Stone" -> "Hurtful"; anything else is left as it is. */
    static String powerName(String itemName) {
        if (itemName == null || itemName.isEmpty()) return "";
        String name = itemName.replaceAll("(?i)\\s*(?:power\\s+stone|power|stone)\\s*$", "").trim();
        return name.isEmpty() ? itemName.trim() : name;
    }

    /** The accessory power an item's lore carries, or null. */
    public static String magicalFromLore(List<String> lore) {
        if (lore == null) return null;
        for (String line : lore) {
            String text = clean(line);
            if (text.isEmpty()) continue;
            Matcher m = LORE_MAGICAL.matcher(text);
            if (m.find()) return m.group(1);
            Matcher t = LORE_TOTAL.matcher(text);
            if (t.find()) return t.group(1);
        }
        return null;
    }

    // ------------------------------------------------------------------ the two hooks

    /**
     * Reads the chat for a power change.
     *
     * `receiveCanceled` is set: the chat filter may well be hiding this message - it is one of the
     * server's own notices - and the power still has to be learned from it. Learning is not showing.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onChat(ClientChatReceivedEvent event) {
        try {
            if (event == null || event.message == null || event.type == 2) return;
            String plain = EnumChatFormatting.getTextWithoutFormattingCodes(event.message.getUnformattedText());
            String power = fromChat(plain);
            if (power != null) learn(power, "chat");
        } catch (Throwable t) {
            // never let a notice about a power stone break the chat
        }
    }

    /**
     * Reads the open menu for the tooltip.
     *
     * Any container screen is looked at, because the item that carries the marker is the accessory
     * bag's own power item and the screen it lives on titles itself in whatever way the server
     * likes. The scan runs when the screen changes and then once a second while it stays open: the
     * items are filled in a tick or two after the menu opens, so a single scan on open would
     * sometimes look at an empty inventory.
     */
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null || mc.currentScreen == null) {
                scanned = null;
                return;
            }
            if (mc.currentScreen != scanned) {
                scanned = mc.currentScreen;
                rescanTicks = 0;
            } else if (--rescanTicks > 0) {
                return;
            }
            rescanTicks = 20;

            List<IInventory> inventories = containers(mc.currentScreen);
            if (inventories.isEmpty()) return;
            IInventory menu = accessoryBagMenu(inventories);
            if (menu == null) return;                     // not the menu this feature is about
            String source = menuName(menu);
            for (int slot = 0; slot < menu.getSizeInventory(); slot++) {
                ItemStack stack = menu.getStackInSlot(slot);
                if (stack == null || stack.getTagCompound() == null) continue;
                List<String> lore = loreOf(stack);
                String power = fromLore(lore, stack.getDisplayName());
                if (power != null) learn(power, source);
                String magical = magicalFromLore(lore);
                if (magical != null) learnMagical(magical, source);
            }
        } catch (Throwable t) {
            // a menu that cannot be read is not a failure worth reporting
        }
    }

    /** The one inventory titled as the accessory bag's Thaumaturgy menu, or null. */
    private static IInventory accessoryBagMenu(List<IInventory> inventories) {
        try {
            for (IInventory inventory : inventories) {
                if (inventory == null || inventory.getDisplayName() == null) continue;
                String title = clean(inventory.getDisplayName().getUnformattedText()).toLowerCase(Locale.ROOT);
                if (title.contains(MENU_WORD_ONE) && title.contains(MENU_WORD_TWO)) return inventory;
            }
        } catch (Throwable t) {
            return null;
        }
        return null;
    }

    /** What the menu calls itself, for the report. */
    private static String menuName(IInventory inventory) {
        try {
            if (inventory != null && inventory.getDisplayName() != null) {
                String title = clean(inventory.getDisplayName().getUnformattedText());
                if (!title.isEmpty()) return "the " + title + " menu";
            }
        } catch (Throwable t) {
            // fall through to the plain wording
        }
        return "the accessory bag menu";
    }

    /** An item's lore as plain lines, or an empty list. */
    private static List<String> loreOf(ItemStack stack) {
        List<String> out = new ArrayList<String>();
        try {
            NBTTagList lore = stack.getTagCompound().getCompoundTag("display").getTagList("Lore", 8);
            for (int i = 0; i < lore.tagCount(); i++) {
                out.add(EnumChatFormatting.getTextWithoutFormattingCodes(lore.getStringTagAt(i)));
            }
        } catch (Throwable t) {
            // an item with no lore is an item with nothing to say
        }
        return out;
    }

    /**
     * Every inventory behind the open screen.
     *
     * Found by type rather than by field name: names are remapped on build and would break in
     * production, and a screen may hold several inventories (the menu's and the player's). The
     * biggest is the menu's own; the rest are still worth looking at, because an item the player
     * has moved into their own inventory carries the same lore.
     */
    private static List<IInventory> containers(GuiScreen screen) {
        List<IInventory> found = new ArrayList<IInventory>();
        try {
            for (java.lang.reflect.Field field : screen.getClass().getDeclaredFields()) {
                if (!IInventory.class.isAssignableFrom(field.getType())) continue;
                field.setAccessible(true);
                Object value = field.get(screen);
                if (!(value instanceof IInventory)) continue;
                IInventory inventory = (IInventory) value;
                if (!found.contains(inventory)) found.add(inventory);
            }
            java.util.Collections.sort(found, new java.util.Comparator<IInventory>() {
                public int compare(IInventory a, IInventory b) {
                    return b.getSizeInventory() - a.getSizeInventory();
                }
            });
        } catch (Throwable t) {
            return found;
        }
        return found;
    }

    /** Drops the colour codes and the decoration the server wraps a power's name in. */
    private static String clean(String text) {
        if (text == null) return "";
        String plain = EnumChatFormatting.getTextWithoutFormattingCodes(text);
        if (plain == null) plain = text;
        StringBuilder out = new StringBuilder(plain.length());
        for (int i = 0; i < plain.length(); i++) {
            char c = plain.charAt(i);
            if (c == '\u00a7') {
                i++;                                  // a colour code that survived the strip
                continue;
            }
            if (c == '\u00a0' || c == '\u200b' || c == '\u200d' || c == '\u2060' || c == '\ufeff') continue;
            out.append(c);
        }
        // the marker line is compared as a whole, so a trailing full stop has to go; the power's
        // own name must not be trimmed this way
        return out.toString().replaceAll("\\s+", " ").trim();
    }

    /** For /se power: what is known, and where it came from. */
    public static String probeState() {
        String power = stored();
        String magical = magical();
        String known;
        if (power.isEmpty() && magical.isEmpty()) {
            known = "(nothing learned yet";
            if (!lastSource.equals("nothing yet")) known += " - the last thing seen was " + lastSource;
            return known + ")";
        }
        known = power.isEmpty() ? "(no power learned)" : power;
        if (!magical.isEmpty()) known += "   accessory power " + magical;
        return known + "   learned from " + lastSource;
    }
}
