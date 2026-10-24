package com.sweatsenhancements.config;

import java.util.ArrayList;
import java.util.List;

import com.sweatsenhancements.core.PanelSlots;
import com.sweatsenhancements.core.TextList;

/**
 * Brings a settings file written by an older build forward when the names a list offers have
 * changed under it.
 *
 * The player removed fourteen elements from every list at once - *"remove the area,server,player,
 * motes,cold,soulflow,flintarrow,skills,stats,pet,party,active effect,website and extra from every
 * list"* - so a file written before that holds names this build no longer offers, and one element
 * this build does offer (the island) was never on the list at all. Dropping the old names leaves a
 * gap where an element should be; adding every missing default would put back entries the player
 * deleted on purpose. The rule is the narrow one: while the stored list still names an element that
 * no longer exists, the entries that are new are put back in their default places, and once the file
 * is written in the new names it is never touched again.
 *
 * Runs once at startup, in memory only: {@link Cfg#set} does not write, the usual flush does.
 */
public final class Migrate {

    private Migrate() {
    }

    public static void run(Cfg cfg) {
        if (cfg == null) return;
        try {
            List<String> elements = new ArrayList<String>(PanelSlots.ALL);
            elements.add(PanelSlots.EMPTY_LINE);
            merge(cfg, "scoreboard.appearance", elements, PanelSlots.DEFAULT_APPEARANCE);
            merge(cfg, "scoreboard.tabLines", PanelSlots.TAB_ELEMENTS, PanelSlots.DEFAULT_TAB_LINES);
            // Existing settings files predate Forges. The old list contains Powder in
            // Appearance but neither block in Select Tablist Lines, so both are invisible.
            // Add these once; a later user removal must stay removed.
            if (!cfg.getBool("migration.miningBlocks", false)) {
                addMining(cfg, "scoreboard.appearance", PanelSlots.DEFAULT_APPEARANCE);
                addMining(cfg, "scoreboard.tabLines", PanelSlots.DEFAULT_TAB_LINES);
                cfg.setBool("migration.miningBlocks", true);
            }
            // 0.9.55 changes the initial copy-button action to the 1.8.9 chat-input
            // behavior. Previous builds saved Clipboard as their default whenever
            // the config screen was opened. Convert that old default once; afterwards
            // the user may choose either action and we never override it again.
            if (!cfg.getBool("migration.copyChatAction", false)) {
                if ("Clipboard".equals(cfg.get("chat.copyAction", ""))) {
                    cfg.set("chat.copyAction", "Chat Input");
                }
                cfg.setBool("migration.copyChatAction", true);
            }
            // 0.9.65 broadens MS220 from a CUSTOM_ITEM-only badge to metadata-based
            // admin warnings. Do not silently broaden an old opt-in on upgrade.
            if (!cfg.getBool("migration.adminItemTag", false)) {
                if (cfg.getBool(Cfg.key("MS220"), false)) cfg.setBool(Cfg.key("MS220"), false);
                cfg.setBool("migration.adminItemTag", true);
            }
            if (!cfg.getBool("migration.dungeonSidebar", false)) {
                String stored = cfg.get("scoreboard.appearance", "");
                if (stored != null && !stored.trim().isEmpty()) {
                    List<String> chosen = TextList.split(stored, PanelSlots.DEFAULT_APPEARANCE);
                    if (!chosen.contains("DUNGEON_SIDEBAR")) {
                        int at = chosen.indexOf("LOCATION");
                        chosen.add(at < 0 ? 0 : at + 1, "DUNGEON_SIDEBAR");
                        cfg.set("scoreboard.appearance", TextList.join(chosen));
                    }
                }
                cfg.setBool("migration.dungeonSidebar", true);
            }
        } catch (Throwable t) {
            // a settings file that cannot be read is not a reason to fail a start
        }
    }

    private static void addMining(Cfg cfg, String key, List<String> defaults) {
        List<String> chosen = TextList.split(cfg.get(key, ""), defaults);
        for (String block : new String[] {"POWDER", "FORGES"}) {
            if (chosen.contains(block)) continue;
            int at = defaults.indexOf(block);
            int insert = chosen.size();
            for (int i = at + 1; i < defaults.size(); i++) {
                int next = chosen.indexOf(defaults.get(i));
                if (next >= 0) { insert = next; break; }
            }
            chosen.add(insert, block);
        }
        cfg.set(key, TextList.join(chosen));
    }

    private static void merge(Cfg cfg, String key, List<String> names, List<String> defaults) {
        String stored = cfg.get(key, "");
        if (stored == null || stored.trim().isEmpty()) return;
        List<Integer> merged = TextList.loadIndicesMerged(stored, names, defaults);
        String text = TextList.joinIndices(merged, names);
        if (!text.equals(stored.trim())) cfg.set(key, text);
    }
}
