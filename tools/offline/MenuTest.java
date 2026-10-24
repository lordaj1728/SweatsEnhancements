import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import com.sweatsenhancements.gui.SweatsConfig;

import io.github.notenoughupdates.moulconfig.processor.ConfigProcessorDriver;
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor;
import io.github.notenoughupdates.moulconfig.processor.ProcessedCategory;
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption;

public class MenuTest {
    public static void main(String[] args) throws Exception {
        SweatsEnhancements.cfg = new Cfg(new File("/tmp/ptest/cfg"));
        SweatsEnhancements.cfg.load();
        SweatsConfig config = new SweatsConfig();
        config.loadFromCfg();
        if (config.dungeon.requeueKey != org.lwjgl.input.Keyboard.KEY_NONE)
            throw new AssertionError("Dungeon requeue key must start unbound in SweatsEnhancements config");
        config.dungeon.requeueKey = org.lwjgl.input.Keyboard.KEY_R;
        config.saveToCfg();
        SweatsConfig requeueReload = new SweatsConfig(); requeueReload.loadFromCfg();
        if (requeueReload.dungeon.requeueKey != org.lwjgl.input.Keyboard.KEY_R)
            throw new AssertionError("Dungeon requeue key must round-trip in SweatsEnhancements config");
        config.dungeon.requeueKey = org.lwjgl.input.Keyboard.KEY_NONE;
        config.saveToCfg();

        MoulConfigProcessor<SweatsConfig> processor = MoulConfigProcessor.withDefaults(config);
        new ConfigProcessorDriver(processor).processConfig(config);

        // NEF nested @Category fields are NOT navigable inside another category in MoulConfig.
        // Assert the real processor exposes every Slayer page and a live option on it.
        String[][] slayerTabs = {
            {"slayerOverlaySettings", "Slayer Overlay"},
            {"minibossSettings", "Slayer Minibosses Display"},
            {"bossSettings", "Slayer Bosses Display"},
            {"beaconSettings", "Show Beacon Waypoint"},
            {"infernoDemonlord", "Display Blaze Pillar Title"},
            {"blazeAttunementSettings", "Show Outline"}
        };
        for(String[] tab : slayerTabs){
            boolean found=false;
            for(ProcessedCategory category : processor.getAllCategories().values()){
                if(!category.getIdentifier().toString().contains(tab[0]))continue;
                for(ProcessedOption option : category.getOptions()){
                    if(option.getEditor()!=null && option.getName()!=null && option.getName().toString().contains(tab[1]))found=true;
                }
            }
            if(!found)throw new AssertionError("Missing visible NEF Slayer page/option: "+tab[0]+" / "+tab[1]);
        }
        if(config.bossSettings!=com.sweatsenhancements.feature.nef.exact.SlayerSettingsStore.get().bossSettings)
            throw new AssertionError("Slayer page is not bound to active persisted settings");
        config.bossSettings.slayerBossHP=true;
        config.saveToCfg();
        if(!new com.google.gson.JsonParser().parse(SweatsEnhancements.cfg.get("nef.slayer.settings", "{}"))
                .getAsJsonObject().getAsJsonObject("bossSettings").get("slayerBossHP").getAsBoolean())
            throw new AssertionError("Slayer boss health switch did not persist");
        config.bossSettings.slayerBossHP=false;
        config.saveToCfg();
        System.out.println("six NEF Slayer pages processed with visible controls and persisted live settings");

        // The original nested categories must not be processed as duplicate tabs.
        int slayerPages=0;
        for(ProcessedCategory category:processor.getAllCategories().values()){
            String id=String.valueOf(category.getIdentifier());
            if(id.contains("SweatsConfig.slayerOverlaySettings") || id.contains("SweatsConfig.minibossSettings")
                    || id.contains("SweatsConfig.bossSettings") || id.contains("SweatsConfig.beaconSettings")
                    || id.contains("SweatsConfig.infernoDemonlord") || id.contains("SweatsConfig.blazeAttunementSettings"))
                slayerPages++;
            if(id.contains("SlayerSettings.slayerOverlaySettings") || id.contains("SlayerSettings.minibossSettings")
                    || id.contains("SlayerSettings.bossSettings") || id.contains("SlayerSettings.beaconSettings")
                    || id.contains("SlayerSettings.infernoDemonlord") || id.contains("SlayerSettings.blazeAttunementSettings"))
                throw new AssertionError("Duplicate nested Slayer menu page: "+id);
        }
        if(slayerPages!=6)throw new AssertionError("Expected six unique Slayer settings pages; saw "+slayerPages);

        // Cover every live feature, not just the newest five: no registered switch may
        // disappear from /sweatsenhancements when SweatsConfig's generator is forgotten.
        java.util.Set<String> exposed = new java.util.HashSet<String>();
        for (java.lang.reflect.Field category : SweatsConfig.class.getFields()) {
            Object holder = category.get(config);
            if (holder == null) continue;
            for (java.lang.reflect.Field field : holder.getClass().getFields()) {
                if (!com.sweatsenhancements.config.Implemented.IDS.contains(field.getName())) continue;
                if (field.getType() != boolean.class
                        || field.getAnnotation(io.github.notenoughupdates.moulconfig.annotations.ConfigOption.class) == null
                        || field.getAnnotation(io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean.class) == null)
                    throw new AssertionError("Unusable /sweatsenhancements switch: " + field.getName());
                if (!exposed.add(field.getName())) throw new AssertionError("Duplicate /sweatsenhancements switch: " + field.getName());
            }
        }
        if (!exposed.equals(com.sweatsenhancements.config.Implemented.IDS)) {
            java.util.Set<String> missing = new java.util.HashSet<String>(com.sweatsenhancements.config.Implemented.IDS);
            missing.removeAll(exposed);
            throw new AssertionError("Missing live /sweatsenhancements switches: " + missing);
        }
        System.out.println("all " + exposed.size() + " live /sweatsenhancements switches have annotated menu fields");

        // Regression: a registered feature without an annotated SweatsConfig field is invisible
        // in /sweatsenhancements. Inspect the actual processed MoulConfig category, not the feature list.
        java.util.Set<String> dungeonOptions = new java.util.HashSet<String>();
        for (ProcessedCategory category : processor.getAllCategories().values()) {
            if (!String.valueOf(category.getIdentifier()).contains("SweatsConfig$Dungeon")) continue;
            for (ProcessedOption option : category.getOptions())
                if (option.getEditor() != null && option.getName() != null)
                    dungeonOptions.add(option.getName().toString());
        }
        for (String name : new String[] {"Hide Blessing Announcements", "Boss Highlights",
                "Scarf Minion Highlights", "Dropped Spirit Bow Highlight", "Hide Boss / Watcher Chat"}) {
            boolean visible = false;
            for (String option : dungeonOptions) if (option.contains("text='" + name + "'")) visible = true;
            if (!visible) throw new AssertionError("Missing live /sweatsenhancements Dungeon switch: " + name);
        }
        for (String id : new String[] {"DU021", "DU022", "DU023", "DU024", "DU025"}) {
            java.lang.reflect.Field field = SweatsConfig.Dungeon.class.getField(id);
            if (field.getAnnotation(io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean.class) == null
                    || field.getBoolean(config.dungeon))
                throw new AssertionError("New dungeon switch has no boolean editor or defaults on: " + id);
        }
        config.dungeon.DU022 = true;
        config.saveToCfg();
        if (!SweatsEnhancements.cfg.getBool(Cfg.key("DU022"), false) || SweatsEnhancements.cfg.getBool(Cfg.key("DU023"), false))
            throw new AssertionError("Boss highlight switch does not save independently");
        SweatsConfig dungeonReload = new SweatsConfig(); dungeonReload.loadFromCfg();
        if (!dungeonReload.dungeon.DU022 || dungeonReload.dungeon.DU023)
            throw new AssertionError("Boss highlight switch does not round-trip");
        config.dungeon.DU022 = false;
        config.saveToCfg();
        System.out.println("five new /sweatsenhancements dungeon switches: visible, default off and persisted");

        // A feature registered only in Features/Implemented is NOT visible in MoulConfig.
        // Assert the generated screen exposes all five chat-filter toggles.
        java.util.Set<String> chatOptions = new java.util.HashSet<String>();
        for (ProcessedCategory category : processor.getAllCategories().values()) {
            if (!String.valueOf(category.getIdentifier()).contains("SweatsConfig$Chat")) continue;
            for (ProcessedOption option : category.getOptions())
                if (option.getEditor() != null && option.getName() != null)
                    chatOptions.add(option.getName().toString());
        }
        for (String name : new String[] {"Hide Watchdog / Info", "Hide Friend Join / Leave",
                "Hide Zombie Rare Drops", "Hide Selling Ranks", "Hide Game Invites"}) {
            boolean visible = false;
            for (String option : chatOptions) if (option.contains("text='" + name + "'")) visible = true;
            if (!visible) throw new AssertionError("Missing live Chat switch: " + name);
        }
        if (config.chat.CH201 || config.chat.CH202 || config.chat.CH203 || config.chat.CH204 || config.chat.CH205)
            throw new AssertionError("New chat filters must default off");
        config.chat.CH203 = true;
        config.saveToCfg();
        if (!SweatsEnhancements.cfg.getBool(Cfg.key("CH203"), false) || SweatsEnhancements.cfg.getBool(Cfg.key("CH202"), false))
            throw new AssertionError("Chat toggles not independent");
        config.chat.CH203 = false;
        config.saveToCfg();
        System.out.println("five chat-filter switches: visible, default off, independent");
        for (String name : new String[] {"Hide Server Announcements"}) {
            boolean visible = false;
            for (String option : chatOptions) if (option.contains("text='" + name + "'")) visible = true;
            if (!visible) throw new AssertionError("Missing live Chat switch: " + name);
        }
        java.util.Set<String> miscBatch = new java.util.HashSet<String>();
        for (ProcessedCategory category : processor.getAllCategories().values()) {
            if (!String.valueOf(category.getIdentifier()).contains("SweatsConfig$Misc")) continue;
            for (ProcessedOption option : category.getOptions())
                if (option.getEditor() != null && option.getName() != null)
                    miscBatch.add(option.getName().toString());
        }
        for (String name : new String[] {"Mute Jerry-chine Gun", "Middle-Click SkyBlock Menus",
                "Prevent Ability Item Placement", "Mute Zombie Drop Chime"}) {
            boolean visible = false;
            for (String option : miscBatch) if (option.contains("text='" + name + "'")) visible = true;
            if (!visible) throw new AssertionError("Missing live Misc switch: " + name);
        }
        if (config.chat.CH206 || config.misc.MS212 || config.misc.MS213
                || config.misc.MS214 || config.misc.MS215)
            throw new AssertionError("New five switches must default off");
        config.misc.MS214 = true;
        config.saveToCfg();
        if (!SweatsEnhancements.cfg.getBool(Cfg.key("MS214"), false)
                || SweatsEnhancements.cfg.getBool(Cfg.key("MS213"), false))
            throw new AssertionError("New Misc switches not independent");
        config.misc.MS214 = false;
        config.saveToCfg();
        System.out.println("next five switches: visible, default off, independent");
        for (String name : new String[] {"Show Unclaimed Contests", "Hide Flaming Fists",
                "Show Item ID (SweatsEnhancements Extra)", "Show Stacking Counter", "Show Admin Item Tag"}) {
            boolean visible = false;
            for (String option : miscBatch) if (option.contains("text='" + name + "'")) visible = true;
            if (!visible) throw new AssertionError("Missing live Misc switch: " + name);
        }
        if (config.misc.MS216 || config.misc.MS217 || config.misc.MS218
                || config.misc.MS219 || config.misc.MS220)
            throw new AssertionError("Five item/visual switches must default off");
        config.misc.MS219 = true;
        config.saveToCfg();
        if (!SweatsEnhancements.cfg.getBool(Cfg.key("MS219"), false)
                || SweatsEnhancements.cfg.getBool(Cfg.key("MS218"), false))
            throw new AssertionError("Item tooltip switches not independent");
        config.misc.MS219 = false;
        config.saveToCfg();
        System.out.println("five item/visual switches: visible, default off, independent");
        boolean wardrobeVisible = false;
        for (String option : miscBatch)
            if (option.contains("text='Highlight Active Wardrobe Slot'")) wardrobeVisible = true;
        if (!wardrobeVisible || config.misc.MS221)
            throw new AssertionError("Active Wardrobe Slot switch missing or default-on");

        // Assert the actual generated screen exposes every shortcut switch.
        java.util.Set<String> batchNames = new java.util.HashSet<String>();
        int shortcutKeyEditors = 0;
        for (ProcessedCategory category : processor.getAllCategories().values()) {
            if (!String.valueOf(category.getIdentifier()).contains("SweatsConfig$Misc")) continue;
            for (ProcessedOption option : category.getOptions()) {
                if (option.getEditor() == null || option.getName() == null) continue;
                batchNames.add(option.getName().toString());
                if ((option.getName().toString().contains("text='Wardrobe Key'")
                        || option.getName().toString().contains("text='Equipment Key'"))
                        && option.getEditor().getClass().getSimpleName().toLowerCase().contains("keybind"))
                    shortcutKeyEditors++;
            }
        }
        if (shortcutKeyEditors != 2) throw new AssertionError("Both shortcuts need real menu keybind editors");
        for (String name : new String[] {"Disable Rain Rendering", "Wardrobe Shortcut", "Equipment Shortcut",
                "Wardrobe Key", "Equipment Key", "Wardrobe Slot Shortcuts", "Warp Shortcuts",
                "Wardrobe Slot 1 Key", "Wardrobe Slot 9 Key", "Warp Island Shortcut Key",
                "Warp Hub Shortcut Key", "Warp Dungeon Hub Shortcut Key"}) {
            boolean found = false;
            for (String entry : batchNames) {
                if (entry.contains("text='" + name + "'")) found = true;
            }
            if (!found) throw new AssertionError("Missing live Misc menu switch: " + name);
        }
        if (config.misc.MS207 || config.misc.MS208 || config.misc.MS209)
            throw new AssertionError("Batch-one switches must default off");
        if (config.misc.wardrobeKey != org.lwjgl.input.Keyboard.KEY_NONE
                || config.misc.equipmentKey != org.lwjgl.input.Keyboard.KEY_NONE)
            throw new AssertionError("Shortcut keys must default unbound");
        for (String field : new String[] {"wardrobeKey", "equipmentKey"}) {
            if (SweatsConfig.Misc.class.getField(field).getAnnotation(
                    io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind.class) == null)
                throw new AssertionError("Missing config keybind editor: " + field);
        }
        if (config.misc.MS210 || config.misc.MS211
                || config.misc.wardrobeSlotKey1 != org.lwjgl.input.Keyboard.KEY_1
                || config.misc.wardrobeSlotKey9 != org.lwjgl.input.Keyboard.KEY_9
                || config.misc.warpIslandKey != org.lwjgl.input.Keyboard.KEY_F7
                || config.misc.warpHubKey != org.lwjgl.input.Keyboard.KEY_F8
                || config.misc.warpDungeonHubKey != org.lwjgl.input.Keyboard.KEY_F9)
            throw new AssertionError("Slot and warp defaults incorrect");
        if (!com.sweatsenhancements.feature.WardrobeSlotKeys.isWardrobe("Wardrobe", 54)
                || com.sweatsenhancements.feature.WardrobeSlotKeys.isWardrobe("Backpack", 54)
                || com.sweatsenhancements.feature.WardrobeSlotKeys.isWardrobe("Wardrobe", 27)
                || com.sweatsenhancements.feature.WardrobeSlotKeys.chestSlotFor(8) != 44
                || com.sweatsenhancements.feature.WardrobeSlotKeys.slotForKey(org.lwjgl.input.Keyboard.KEY_1,
                    new int[] {2,3,4,5,6,7,8,9,10}) != 0
                || com.sweatsenhancements.feature.WardrobeSlotKeys.slotForKey(org.lwjgl.input.Keyboard.KEY_1,
                    new int[] {2,2,4,5,6,7,8,9,10}) != -1)
            throw new AssertionError("Wardrobe slot safety check incorrect");
        if (com.sweatsenhancements.feature.ConfigShortcuts.unique(0, new int[] {2,2}, new boolean[] {true,true})
                || !com.sweatsenhancements.feature.ConfigShortcuts.unique(0, new int[] {2,3}, new boolean[] {true,true}))
            throw new AssertionError("Shortcut duplicate key check incorrect");
        config.misc.wardrobeSlotKey1 = org.lwjgl.input.Keyboard.KEY_Z;
        config.misc.warpHubKey = org.lwjgl.input.Keyboard.KEY_F6;
        config.misc.wardrobeKey = org.lwjgl.input.Keyboard.KEY_R;
        config.misc.equipmentKey = org.lwjgl.input.Keyboard.KEY_U;
        config.saveToCfg();
        SweatsConfig reloaded = new SweatsConfig();
        reloaded.loadFromCfg();
        if (reloaded.misc.wardrobeKey != org.lwjgl.input.Keyboard.KEY_R
                || reloaded.misc.equipmentKey != org.lwjgl.input.Keyboard.KEY_U
                || reloaded.misc.wardrobeSlotKey1 != org.lwjgl.input.Keyboard.KEY_Z
                || reloaded.misc.warpHubKey != org.lwjgl.input.Keyboard.KEY_F6)
            throw new AssertionError("Shortcut keys not persisted independently");
        config.misc.wardrobeKey = org.lwjgl.input.Keyboard.KEY_NONE;
        config.misc.equipmentKey = org.lwjgl.input.Keyboard.KEY_NONE;
        config.misc.wardrobeSlotKey1 = org.lwjgl.input.Keyboard.KEY_1;
        config.misc.warpHubKey = org.lwjgl.input.Keyboard.KEY_F8;
        config.saveToCfg();
        if (com.sweatsenhancements.feature.ConfigShortcuts.shouldSend(true, true, true, true)
                || com.sweatsenhancements.feature.ConfigShortcuts.shouldSend(true, false, false, true)
                || com.sweatsenhancements.feature.ConfigShortcuts.shouldSend(true, false, true, false)
                || !com.sweatsenhancements.feature.ConfigShortcuts.shouldSend(true, false, true, true))
            throw new AssertionError("Shortcut edge/gate incorrect");
        config.misc.MS207 = true;
        config.misc.MS208 = true;
        config.misc.MS209 = true;
        config.saveToCfg();
        for (String id : new String[] {"MS207", "MS208", "MS209"}) {
            if (!SweatsEnhancements.cfg.getBool(Cfg.key(id), false)) throw new AssertionError("Toggle not saved: " + id);
        }
        config.misc.MS207 = false;
        config.misc.MS208 = false;
        config.misc.MS209 = false;
        config.saveToCfg();
        System.out.println("batch-one menu switches: visible, default off, and persisted independently");

        Map<String, Integer> kinds = new LinkedHashMap<String, Integer>();
        int options = 0;
        int accordions = 0;
        for (ProcessedCategory category : processor.getAllCategories().values()) {
            System.out.println("category " + String.valueOf(category.getIdentifier()) + " - " + category.getOptions().size() + " options");
            for (ProcessedOption option : category.getOptions()) {
                options++;
                String kind = option.getEditor() == null ? "null"
                        : option.getEditor().getClass().getSimpleName();
                if (kind.startsWith("GuiOptionEditorAccordion")) accordions++;
                Integer n = kinds.get(kind);
                kinds.put(kind, Integer.valueOf(n == null ? 1 : n.intValue() + 1));
            }
        }
        System.out.println("options: " + options + ", folding sections: " + accordions);
        for (Map.Entry<String, Integer> e : kinds.entrySet()) {
            System.out.println("  " + e.getKey() + " x" + e.getValue());
        }

        // the scoreboard section, in order, as the screen will lay it out
        System.out.println("--- Scoreboard, as the screen builds it");
        for (ProcessedCategory category : processor.getAllCategories().values()) {
            if (!String.valueOf(category.getIdentifier()).contains("$Scoreboard ")) continue;
            int shown = 0;
            for (ProcessedOption option : category.getOptions()) {
                if (shown++ > 30) { System.out.println("  ..."); break; }
                String name = option.getName() == null ? "" : option.getName().toString();
                System.out.println("  [acc " + option.getAccordionId() + "] " + name + "  <"
                        + (option.getEditor() == null ? "null" : option.getEditor().getClass().getSimpleName())
                        + ">");
            }
        }

        // ---- the three draggable lists
        //
        // This is the check the player's screenshots called for: MoulConfig's draggable editor
        // labels an entry by looking it up in the list it was given - for a list that is not an
        // enum, the map is keyed by *position*, 0..n-1, and the value is the example text. The
        // lists used to hold names, so every lookup missed and every row was drawn as
        // "<unknown NAME>". They hold positions now.
        //
        // The editor itself cannot be built in this harness - its constructor dies inside the
        // library's own GuiOptionEditor under the stubs - so the lookup below is the library's own
        // rule, run by hand: position in, name out. Anything that prints "<unknown" here would
        // print "<unknown" on the screen.
        System.out.println("--- the draggable lists, labelled the way the screen labels them");
        for (String group : new String[] { "appearance", "eventsPriority", "tabLines" }) {
            java.lang.reflect.Field field = SweatsConfig.Scoreboard.class.getField(group);
            io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList annotation =
                    field.getAnnotation(io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList.class);
            java.lang.reflect.Type type = field.getGenericType();
            java.util.List<?> entries = (java.util.List<?>) field.get(config.scoreboard);
            StringBuilder labels = new StringBuilder();
            int unknown = 0;
            for (Object entry : entries) {
                String label;
                Integer at = entry instanceof Number ? Integer.valueOf(((Number) entry).intValue()) : null;
                if (at != null && at >= 0 && at < annotation.exampleText().length) {
                    label = annotation.exampleText()[at.intValue()];
                } else {
                    label = "<unknown " + entry + ">";
                    unknown++;
                }
                if (labels.length() > 0) labels.append(", ");
                labels.append(label);
            }
            System.out.println("  " + group + " : " + type + "  " + entries.size() + " entries, "
                    + unknown + " unknown");
            System.out.println("      " + labels);
        }

        config.scoreboard.hideWhileTab = true;
        config.scoreboard.verticalAlign = "Top";
        config.scoreboard.appearance = com.sweatsenhancements.core.TextList.indices(
                com.sweatsenhancements.gui.SweatsConfig.Scoreboard.APPEARANCE_NAMES,
                com.sweatsenhancements.core.TextList.split("BANK;purse;EXTRA", null));
        config.tablist.hideAdverts = true;
        config.tablist.maxLines = 18;
        config.chat.copyAction = "Chat Input";
        config.chat.copyButtonStyle = "[COPY]";
        config.saveToCfg();
        Cfg cfg = SweatsEnhancements.cfg;
        // The two lists that have to agree: the names the config screen offers and the elements the
        // panel says it has. They are written in two places - the generator writes the screen, the
        // panel keeps its own - so a drift between them is a row that can be dragged but never
        // drawn, or an element that draws but cannot be reordered.
        System.out.println("--- the offered names, the screen's list against the panel's");
        java.util.List<String> drawnNames = new java.util.ArrayList<String>(com.sweatsenhancements.core.PanelSlots.ALL);
        java.util.List<String> screenNames =
                new java.util.ArrayList<String>(SweatsConfig.Scoreboard.APPEARANCE_NAMES);
        screenNames.remove(com.sweatsenhancements.core.PanelSlots.EMPTY_LINE);
        System.out.println("  appearance: screen " + screenNames.size() + ", panel " + drawnNames.size()
                + ", same names: " + screenNames.containsAll(drawnNames)
                + ", same order: " + screenNames.equals(drawnNames));
        System.out.println("  tab lines : screen " + SweatsConfig.Scoreboard.TAB_LINES_NAMES.size()
                + ", panel " + com.sweatsenhancements.core.PanelSlots.TAB_ELEMENTS.size()
                + ", same names: " + SweatsConfig.Scoreboard.TAB_LINES_NAMES
                        .containsAll(com.sweatsenhancements.core.PanelSlots.TAB_ELEMENTS));
        // every entry of the appearance list is drawn with a colour, so the list reads as the
        // scoreboard does - the annotation's example text is where that colour comes from
        java.lang.reflect.Field appearanceField = SweatsConfig.Scoreboard.class.getField("appearance");
        io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList appearanceAnn =
                appearanceField.getAnnotation(
                        io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList.class);
        int coloured = 0;
        for (String example : appearanceAnn.exampleText()) {
            if (example.startsWith("\u00a7")) coloured++;
        }
        System.out.println("  appearance entries drawn in colour: " + coloured + " of "
                + appearanceAnn.exampleText().length);

        System.out.println("written hideWhileTab  : " + cfg.getBool("scoreboard.hideWhileTab", false));
        System.out.println("written verticalAlign : " + cfg.get("scoreboard.verticalAlign", ""));
        System.out.println("written appearance    : " + cfg.get("scoreboard.appearance", ""));
        System.out.println("read back by panel    : "
                + com.sweatsenhancements.feature.PanelLines.list("scoreboard.appearance", null));
        System.out.println("written tab adverts   : " + cfg.getBool("tab.hideAdverts", false));
        System.out.println("written tab maxLines  : " + cfg.getInt("tab.maxLines", 0));
        SweatsConfig again = new SweatsConfig();
        again.loadFromCfg();
        if (!"Chat Input".equals(again.chat.copyAction)
                || !"[COPY]".equals(again.chat.copyButtonStyle))
            throw new AssertionError("copy chat dropdown settings did not round-trip");
        System.out.println("copy chat mode/style round-trip: Chat Input / [COPY]");
        System.out.println("reloaded appearance   : " + again.scoreboard.appearance.size()
                + " positions -> " + com.sweatsenhancements.core.TextList.namesOf(again.scoreboard.appearance,
                com.sweatsenhancements.gui.SweatsConfig.Scoreboard.APPEARANCE_NAMES));
        System.out.println("reloaded events       : " + again.scoreboard.eventsPriority.size() + " entries");
    }
}
