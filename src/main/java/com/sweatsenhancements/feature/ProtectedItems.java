package com.sweatsenhancements.feature;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import net.minecraft.item.Item;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.inventory.Slot;

/** Safeguard by server UUID, or by exact type + metadata + NBT when a UUID is absent. */
public final class ProtectedItems {
    private static final Set<String> IDS = new HashSet<String>();
    private static final Set<String> FINGERPRINTS = new HashSet<String>();
    private static File file;

    public static synchronized void load(File configDir) {
        file = new File(new File(configDir, "sweatsenhancements"), "protected-items.properties");
        IDS.clear();
        FINGERPRINTS.clear();
        if (!file.exists()) return;
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            p.load(in);
            for (String key : p.stringPropertyNames()) {
                if (key.startsWith("uuid.") && key.substring(5).matches("[a-f0-9-]{16,64}"))
                    IDS.add(key.substring(5));
                else if (key.startsWith("fingerprint.") && key.substring(12).matches("[a-f0-9]{64}"))
                    FINGERPRINTS.add(key.substring(12));
                else if (key.matches("[a-f0-9-]{16,64}")) IDS.add(key); // legacy UUID format
            }
        } catch (Exception e) {
            System.out.println("[SweatsEnhancements] protected items load failed: " + e);
        }
    }

    public static String itemUuid(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) return null;
        NBTTagCompound root = stack.getTagCompound();
        if (!root.hasKey("ExtraAttributes", 10)) return null;
        NBTTagCompound ea = root.getCompoundTag("ExtraAttributes");
        if (!ea.hasKey("uuid", 8)) return null;
        String uuid = ea.getString("uuid").trim().toLowerCase(Locale.ROOT);
        return uuid.matches("[a-f0-9-]{16,64}") ? uuid : null;
    }

    /** UUID-less copies with equal item type, metadata and NBT share one protection rule. */
    public static String itemFingerprint(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return null;
        String identity = Item.getIdFromItem(stack.getItem()) + ":" + stack.getMetadata()
                + ":" + (stack.hasTagCompound() ? stack.getTagCompound().toString() : "");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(identity.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : digest) hex.append(String.format(Locale.ROOT, "%02x", b & 255));
            return hex.toString();
        } catch (Exception e) { return null; }
    }

    public static boolean enabled() {
        return SweatsEnhancements.cfg != null && SweatsEnhancements.cfg.getBool(Cfg.key("MS222"), false);
    }

    public static synchronized boolean isProtected(ItemStack stack) {
        if (!enabled() || stack == null) return false;
        String uuid = itemUuid(stack);
        if (uuid != null) return IDS.contains(uuid);
        String fingerprint = itemFingerprint(stack);
        return fingerprint != null && FINGERPRINTS.contains(fingerprint);
    }

    public static synchronized Boolean toggle(ItemStack stack) {
        if (file == null || stack == null) return null;
        String uuid = itemUuid(stack);
        String fingerprint = uuid == null ? itemFingerprint(stack) : null;
        if (uuid == null && fingerprint == null) return null;
        Set<String> target = uuid != null ? IDS : FINGERPRINTS;
        String value = uuid != null ? uuid : fingerprint;
        boolean now = target.add(value);
        if (!now) target.remove(value);
        save();
        return now;
    }

    private static void save() {
        Properties p = new Properties();
        for (String id : IDS) p.setProperty("uuid." + id, "protected");
        for (String digest : FINGERPRINTS) p.setProperty("fingerprint." + digest, "protected");
        File tmp = new File(file.getPath() + ".tmp");
        try {
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
            try (FileOutputStream out = new FileOutputStream(tmp)) { p.store(out, "SweatsEnhancements protected UUIDs and matching item fingerprints"); }
            if (file.exists() && !file.delete()) throw new IllegalStateException("could not replace file");
            if (!tmp.renameTo(file)) throw new IllegalStateException("could not install file");
        } catch (Exception e) {
            System.out.println("[SweatsEnhancements] protected items save failed: " + e);
        }
    }

    private static long lastBlockedLog;
    public static void blocked(String why) {
        long now = System.currentTimeMillis();
        if (now - lastBlockedLog >= 1000) {
            System.out.println("[SweatsEnhancements] protected item " + why + " blocked before sending");
            lastBlockedLog = now;
        }
    }

    /** Pure decision function for offline tests; vanilla player inventory remains unrestricted. */
    public static boolean shouldBlock(int slotId, int button, int mode, ItemStack cursor,
                                      ItemStack clicked, ItemStack hotbar, boolean risky) {
        if (slotId == -999) return isProtected(cursor);
        if (mode == 4) return isProtected(clicked); // Q or Ctrl-Q over a slot
        if (!risky) return false;
        if (isProtected(cursor) || isProtected(clicked)) return true;
        return mode == 2 && isProtected(hotbar); // number-key swap into trade/sell GUI
    }

    /** Also guard a sell-all button: the protected item need not be the clicked stack. */
    public static boolean bulkSellButton(ItemStack clicked) {
        if (clicked == null || !clicked.hasTagCompound()
                || !clicked.getTagCompound().hasKey("display", 10)) return false;
        // Use the server's NBT name directly; no translated/localized item-name fallback.
        String raw = clicked.getTagCompound().getCompoundTag("display").getString("Name");
        String name = EnumChatFormatting.getTextWithoutFormattingCodes(raw);
        if (name == null) return false;
        name = name.toLowerCase(Locale.ROOT).trim();
        return name.contains("sell inventory") || name.contains("sell all")
                || name.contains("sell items") || name.contains("sell your inventory");
    }

    public static boolean blockClick(GuiContainer gui, int slotId, int button, int mode,
                                     ItemStack cursor, ItemStack clicked, ItemStack hotbar) {
        if (!enabled() || gui == null) return false;
        // A seller can name a chest GUI after the NPC (e.g. Adventurer) rather than
        // Shop/Sell. Without a server-supplied container-purpose flag, titles cannot
        // prove an external GUI is safe. Keep normal player-inventory moves available,
        // but reject protected-item clicks in ALL external containers before send.
        boolean risky = !(gui instanceof GuiInventory);
        boolean blocked = shouldBlock(slotId, button, mode, cursor, clicked, hotbar, risky);
        // A bulk-sell GUI button alone does not identify which of the player's stacks the
        // server can actually sell. Never reject it just because an unsellable protected
        // item is present; direct protected-stack clicks remain guarded above.
        if (blocked) blocked("GUI action");
        return blocked;
    }

}
