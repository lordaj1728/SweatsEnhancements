package com.sweatsenhancements.feature;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;
import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Tooltip-only Item Data helpers. Does not rewrite or rename the actual item. */
public final class ItemDataTooltips {
    private static final String[] RARITIES = {"VERY SPECIAL", "UNCOMMON", "LEGENDARY", "COMMON",
            "MYTHIC", "DIVINE", "SPECIAL", "EPIC", "RARE"};
    private static final Map<String, int[]> PROGRESSION = new LinkedHashMap<String, int[]>();
    static {
        // Thresholds are gameplay data, not implementation code from another mod.
        PROGRESSION.put("Cultivating", new int[] {1000,4000,20000,75000,200000,1200000,3500000,15000000,80000000});
        PROGRESSION.put("Compact", new int[] {100,400,1000,3500,10000,35000,100000,350000,500000});
        PROGRESSION.put("Expertise", new int[] {50,150,250,500,1500,3000,4500,5000});
        PROGRESSION.put("Champion", new int[] {50000,150000,250000,500000});
        PROGRESSION.put("Toxophilite", new int[] {50000,150000,250000,500000});
    }

    public static NBTTagCompound attributes(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) return null;
        NBTTagCompound root = stack.getTagCompound();
        return root != null && root.hasKey("ExtraAttributes", 10)
                ? root.getCompoundTag("ExtraAttributes") : null;
    }

    /** A reason is evidence from the item's tags, not a claim about who created it. */
    public static String adminReason(NBTTagCompound a) {
        if (a == null || !a.hasKey("id", 8)) return null;
        String id = a.getString("id").trim();
        if ("CUSTOM_ITEM".equalsIgnoreCase(id)) return "Custom Item";
        if (id.toUpperCase(Locale.ROOT).contains("PET_CUSTOM")) return "Custom Pet";
        String origin = a.getString("origin");
        if (origin.startsWith("ALL_ITEMS_GUI_ACTOR_")) return "Actor origin";
        if (origin.toLowerCase(Locale.ROOT).contains("random")) return "Gambled item";
        if (outside(a, "candy", 0, 10)) return "Candies";
        if (outside(a, "stars", 0, 5)) return "Stars";
        if (outside(a, "master_stars", 0, 5)) return "Master Stars";
        if (outside(a, "bid", 0, 10)) return "Edited tags";
        if (a.hasKey("timestamp", 99) && a.getLong("timestamp") < 1640995200L) return "Invalid date";
        if (a.hasKey("enchantments", 10)) {
            NBTTagCompound enchants = a.getCompoundTag("enchantments");
            for (String key : enchants.getKeySet()) {
                int level = enchants.getInteger(key);
                if (level < 0 || level > 10)
                    return "Enchantment " + safe(key) + " " + level;
            }
        }
        if (a.hasKey("exp", 99) && a.getDouble("exp") > 1.5e9) return "Leveled up";
        return null;
    }

    private static boolean outside(NBTTagCompound attrs, String key, int min, int max) {
        if (!attrs.hasKey(key, 99)) return false;
        long n = attrs.getLong(key);
        return n < min || n > max;
    }

    private static String safe(String s) {
        if (s == null) return "";
        String plain = EnumChatFormatting.getTextWithoutFormattingCodes(s);
        return plain == null ? "" : plain.substring(0, Math.min(plain.length(), 35));
    }

    public static String stackingEnchant(List<String> lore) {
        if (lore == null) return null;
        for (String line : lore) {
            String plain = EnumChatFormatting.getTextWithoutFormattingCodes(line);
            if (plain == null) continue;
            for (String enchant : PROGRESSION.keySet()) {
                if (plain.matches("(?i).*\\b" + enchant + "\\b.*")) return enchant;
            }
        }
        return null;
    }

    public static long nextThreshold(String enchant, long count) {
        int[] thresholds = PROGRESSION.get(enchant);
        if (thresholds == null) return -1;
        for (int threshold : thresholds) if (count < threshold) return threshold;
        return -1;
    }

    private static int beforeRarity(List<String> tooltip) {
        for (int i = tooltip.size() - 1; i >= 1; i--) {
            String plain = EnumChatFormatting.getTextWithoutFormattingCodes(tooltip.get(i));
            if (plain == null) continue;
            String upper = plain.toUpperCase(Locale.ROOT);
            for (String rarity : RARITIES) {
                if (upper.matches(".*\\b" + rarity + "\\b.*")) return i;
            }
        }
        return tooltip.size();
    }

    public static void append(List<String> tooltip, NBTTagCompound attrs,
                              boolean showId, boolean showCounter, boolean showAdmin) {
        if (tooltip == null || attrs == null || !attrs.hasKey("id", 8)) return;
        String id = attrs.getString("id");
        if (id.trim().isEmpty()) return;
        String enchant = showCounter ? stackingEnchant(tooltip) : null;
        int insert = beforeRarity(tooltip);
        if (showAdmin) {
            String reason = adminReason(attrs);
            if (reason != null) {
                String actor = attrs.getString("origin");
                String actorLabel = actor.startsWith("ALL_ITEMS_GUI_ACTOR_")
                        ? " §7(§c" + safe(actor.substring("ALL_ITEMS_GUI_ACTOR_".length())) + "§7)" : "";
                String line = "§c§l⚠ ADMIN ITEM ⚠" + actorLabel + " §8- (" + reason + ")";
                if (!tooltip.contains(line)) tooltip.add(insert++, line);
            }
        }
        if (enchant != null && attrs.hasKey("stacking_enchant_counter", 99)) {
            long count = attrs.getLong("stacking_enchant_counter");
            if (count >= 0) {
                long next = nextThreshold(enchant, count);
                String progress = next < 0 ? "§7(MAX LEVEL)"
                        : "§7(Next Level at §a" + String.format(Locale.US, "%,d", next) + "§7)";
                tooltip.add(insert++, "§6§l" + enchant + ": §e"
                        + String.format(Locale.US, "%,d", count) + " " + progress);
            }
        }
        // Extra SweatsEnhancements option retained separately; it is not part of the reference Item Data menu.
        if (showId) tooltip.add("§8Item ID: " + safe(id));
    }

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        try {
            if (event == null || SweatsEnhancements.cfg == null) return;
            append(event.toolTip, attributes(event.itemStack),
                    SweatsEnhancements.cfg.getBool(Cfg.key("MS218"), false),
                    SweatsEnhancements.cfg.getBool(Cfg.key("MS219"), false),
                    SweatsEnhancements.cfg.getBool(Cfg.key("MS220"), false));
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] item data tooltip failed: " + t);
        }
    }
}
