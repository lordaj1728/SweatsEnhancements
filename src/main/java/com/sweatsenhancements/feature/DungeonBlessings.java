package com.sweatsenhancements.feature;

import com.sweatsenhancements.SweatsEnhancements;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.util.EnumChatFormatting;

/** Reads the server's tab FOOTER: dungeon buffs are not player-list rows. */
public final class DungeonBlessings {
    private static final Pattern BLESSING = Pattern.compile(
            "Blessing of (Power|Wisdom|Life|Stone|Time)\\s+([IVXLCDM]+|[0-9]+)", Pattern.CASE_INSENSITIVE);
    private DungeonBlessings() {}

    public static int roman(String text) {
        if (text == null || text.isEmpty()) return 0;
        if (text.matches("[0-9]+")) { try { return Integer.parseInt(text); } catch (NumberFormatException ex) { return 0; } }
        int result = 0, prev = 0;
        for (int i = text.length() - 1; i >= 0; i--) {
            int n = "IVXLCDM".indexOf(Character.toUpperCase(text.charAt(i)));
            if (n < 0) return 0;
            int value = new int[]{1,5,10,50,100,500,1000}[n];
            result += value < prev ? -value : value;
            prev = value;
        }
        return result;
    }

    public static Map<String, Integer> levels(String text) {
        Map<String, Integer> result = new LinkedHashMap<String, Integer>();
        if (text == null) return result;
        for (String line : text.split("\\r?\\n")) {
            String clean = EnumChatFormatting.getTextWithoutFormattingCodes(line);
            if (clean == null) continue;
            Matcher m = BLESSING.matcher(clean);
            while (m.find()) {
                String name = m.group(1).toLowerCase(java.util.Locale.ROOT);
                int level = roman(m.group(2));
                if (level > 0 && level <= 1000 && (!result.containsKey(name) || result.get(name) < level))
                    result.put(name, level);
            }
        }
        return result;
    }

    /** Kept for offline text fixtures and possible future display of other blessing types. */
    public static List<String> extract(List<String> tabLines) {
        List<String> out = new ArrayList<String>();
        if (tabLines == null) return out;
        for (String line : tabLines) {
            Map<String,Integer> parsed = levels(line);
            for (Map.Entry<String,Integer> e : parsed.entrySet()) {
                String label = e.getKey() + " " + e.getValue();
                if (!out.contains(label)) out.add(label);
            }
        }
        return out;
    }

    public static String formatLevel(int number, boolean roman) {
        if (!roman) return String.valueOf(number);
        if (number <= 0 || number > 1000) return String.valueOf(number);
        StringBuilder out = new StringBuilder();
        int[] values = {1000,900,500,400,100,90,50,40,10,9,5,4,1};
        String[] digits = {"M","CM","D","CD","C","XC","L","XL","X","IX","V","IV","I"};
        for (int i = 0; i < values.length; i++) while (number >= values[i]) {
            out.append(digits[i]); number -= values[i];
        }
        return out.toString();
    }

    private static final String[] ORDER = {"power", "time", "wisdom", "stone", "life"};
    private static final String[] COLOUR = {"§4", "§6", "§b", "§7", "§2"};

    public static List<HudLayout.Line> format(Map<String,Integer> buffs, boolean roman) {
        List<HudLayout.Line> out = new ArrayList<HudLayout.Line>();
        if (buffs == null) return out;
        for (int i = 0; i < ORDER.length; i++) {
            Integer level = buffs.get(ORDER[i]);
            if (level == null || level <= 0) continue;
            String name = Character.toUpperCase(ORDER[i].charAt(0)) + ORDER[i].substring(1);
            out.add(new HudLayout.Line(COLOUR[i] + name + "§f: §a" + formatLevel(level, roman), 0xffffff));
        }
        return out;
    }

    public static List<HudLayout.Line> lines() {
        if (SweatsEnhancements.mc == null || SweatsEnhancements.mc.theWorld == null || !DungeonRunHud.isInDungeon())
            return new ArrayList<HudLayout.Line>();
        Map<String,Integer> buffs = levels(CustomTabList.serverFooter(SweatsEnhancements.mc));
        boolean roman = SweatsEnhancements.cfg != null && SweatsEnhancements.cfg.getBool("dungeonBlessings.roman", false);
        return format(buffs, roman);
    }
}
