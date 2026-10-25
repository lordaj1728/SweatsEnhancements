package com.sweatsenhancements.feature;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import com.sweatsenhancements.core.RichText;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** The first tab-list pet and its own level status, refreshed once per second. */
public final class CurrentPetHud {
    private static final Pattern PET_ROW = Pattern.compile("(?i)^\\[lvl\\s+(\\d+)\\]\\s+(.+)$");
    private volatile Pet current;
    private int ticks;

    public static final class Pet {
        public final String name;
        public final int level;
        public final String status;
        public final String colour;
        public final boolean maxLevel;
        Pet(String name, int level, String status, boolean maxLevel, String colour) {
            this.name = name;
            this.level = level;
            this.status = status;
            this.maxLevel = maxLevel;
            this.colour = colour;
        }
        public String iconKey() {
            return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_")
                    .replaceAll("^_|_$", "");
        }
    }

    /** A bare level-100 Baby Yeti is maxed even if the tab omits its MAX LEVEL row. */
    public static int levelCap(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        if (key.equals("golden dragon") || key.equals("jade dragon")
                || key.equals("rose dragon")) return 200;
        return 100;
    }

    public static Pet parse(List<String> rows) {
        if (rows == null) return null;
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i) == null) continue;
            Matcher m = PET_ROW.matcher(RichText.strip(rows.get(i)).trim());
            if (!m.matches()) continue;
            int level;
            try { level = Integer.parseInt(m.group(1)); }
            catch (NumberFormatException ignored) { return null; }
            String name = m.group(2).trim();
            String colour = colourOf(rows.get(i));
            String progress = "";
            boolean explicitMax = false;
            // Tab entries may be padded; stop at another pet so its status cannot leak here.
            for (int j = i + 1; j < rows.size() && j <= i + 12; j++) {
                String next = rows.get(j) == null ? "" : RichText.strip(rows.get(j)).trim();
                if (PET_ROW.matcher(next).matches()) break;
                if (next.equalsIgnoreCase("MAX LEVEL")) { explicitMax = true; break; }
                if (next.toLowerCase(Locale.ROOT).startsWith("next level:")) {
                    progress = next;
                    break;
                }
            }
            boolean max = explicitMax || (progress.isEmpty() && level >= levelCap(name));
            return new Pet(name, level, max ? "MAX LEVEL" : progress, max, colour);
        }
        return null;
    }

    /** Last colour before the pet name; reset/trailing formatting is not a rarity. */
    public static String colourOf(String row) {
        if (row == null) return "§f";
        int rawClose = row.indexOf(']');
        if (rawClose < 0) return "§f";
        int next = row.indexOf(' ', rawClose);
        String prefix = next < 0 ? row.substring(rawClose + 1) : row.substring(next + 1);
        String colour = "§f";
        for (int i = 0; i < prefix.length();) {
            char c = prefix.charAt(i);
            if (Character.isWhitespace(c)) { i++; continue; }
            if (c == '§' && i + 1 < prefix.length()) {
                char code = Character.toLowerCase(prefix.charAt(i + 1));
                if ("0123456789abcdef".indexOf(code) >= 0) colour = "§" + code;
                i += 2;
                continue;
            }
            break;
        }
        return colour;
    }

    public Pet pet() { return current; }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.theWorld == null || SweatsEnhancements.cfg == null
                    || !SweatsEnhancements.cfg.getBool(Cfg.key("MS201"), false)) {
                current = null;
                ticks = 0;
                return;
            }
            if (++ticks < 20) return;
            ticks = 0;
            CustomTabList.ClientList tab = CustomTabList.clientList(mc);
            current = tab.error.isEmpty() ? parse(tab.rows) : null;
        } catch (Throwable t) {
            current = null;
            System.out.println("[SweatsEnhancements] current pet read failed: " + t);
        }
    }
}
