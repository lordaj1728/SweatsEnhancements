import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import com.sweatsenhancements.feature.PanelLines;
import com.sweatsenhancements.feature.PowerStone;

/**
 * The accessory power, and the config screen's build cost.
 *
 * Power: this server sends no `Power:` line, so the line is drawn from what SweatsEnhancements learned. The chat
 * shapes are the references' own test cases, and so is the menu's item lore - the item marked
 * `Power is selected!` and the `Selected Power:` the same lore spells out. Then the panel: with
 * nothing claimed and a power learned, the line appears; with nothing learned, nothing is drawn.
 *
 * The menu: how long one build of the screen takes, which is the cost the caching removes from every
 * open after the first.
 */
public class PowerTest {

    static void wipe(File dir) {
        File[] kids = dir.listFiles();
        if (kids != null) {
            for (File kid : kids) {
                if (kid.isDirectory()) wipe(kid); else kid.delete();
            }
        }
        dir.delete();
    }

    public static void main(String[] args) throws Exception {
        wipe(new File("cfg-power"));
        SweatsEnhancements.cfg = new Cfg(new File("cfg-power"));

        System.out.println("---- the chat notice, as the references write it");
        String[] chats = {
                "\u00a7eYou selected the \u00a7aSighted \u00a7epower for your \u00a7aAccessory Bag\u00a7e!",
                "\u00a7eYour selected power was set to \u00a7r\u00a7aSighted\u00a7r\u00a7e!",
                "\u00a7eYou selected the \u00a7aFortuitous \u00a7epower for your \u00a7aAccessory Bag\u00a7e!",
                "\u00a7eYour selected power was set to \u00a7r\u00a7aHurtful\u00a7r\u00a7e!",
        };
        for (String chat : chats) {
            String power = PowerStone.fromChat(chat);
            System.out.println("   " + chat.replace('\u00a7', '&') + "  ->  " + power);
        }
        System.out.println("   a notice that is not about a power -> "
                + PowerStone.fromChat("\u00a7eYou selected the \u00a7aAspect of the Dragons \u00a7eitem!"));

        System.out.println("---- the tooltip on the item, which is what the player sees in the menu");
        java.util.List<String> tooltip = Arrays.asList(
                "\u00a77Selected Power: \u00a7aHurtful",
                "\u00a77Accessory Power: \u00a761,398",
                "\u00a77Total: \u00a761,398 Accessory Power",
                "\u00a7aPower is selected!");
        String marked = PowerStone.fromLore(tooltip, "\u00a7dHurtful Power Stone");
        System.out.println("   the marker line says it is this -> " + marked);
        System.out.println("   the accessory power beside it   -> " + PowerStone.magicalFromLore(tooltip));
        System.out.println("   the other way that number is written -> " + PowerStone.magicalFromLore(
                Arrays.asList("\u00a77Total: \u00a761,398 Accessory Power")));
        String spelled = PowerStone.fromLore(Arrays.asList(
                "\u00a77Selected Power: \u00a7aFortuitous",
                "\u00a77Accessory Power: \u00a761,398"), "\u00a7dFortuitous Power Stone");
        System.out.println("   the lore that spells it out     -> " + spelled);
        System.out.println("   an unrelated item               -> "
                + PowerStone.fromLore(Arrays.asList("\u00a77Some lore", "\u00a77More lore"), "\u00a7dSighted Power Stone"));
        // the title gate the first version had: the menu calls itself whatever it likes now
        System.out.println("   (the menu's title is not consulted any more - the player reported that opening");
        System.out.println("    the accessory bag menu learned nothing, only changing the power did)");

        System.out.println("---- what is remembered");
        PowerStone.learn(marked, "the test");
        System.out.println("   stored       : " + PowerStone.stored());
        System.out.println("   probe state  : " + PowerStone.probeState());

        System.out.println("---- the two blank-line settings, on a list with doubles and edges");
        java.util.List<PanelLines.Row> shape = new ArrayList<PanelLines.Row>();
        String[] lines = { "", "Purse: 1", "", "", "Bits: 2", "", "", "" };
        for (int i = 0; i < lines.length; i++) {
            shape.add(new PanelLines.Row(lines[i].isEmpty() ? "EXTRA" : "PURSE", lines[i], i, i));
        }
        System.out.println("   as the server sent it : " + shape.size() + " rows");
        System.out.println("   doubles collapsed     : " + PanelLines.collapseBlank(shape).size() + " rows");
        System.out.println("   edges trimmed as well : "
                + PanelLines.trimBlankEdges(PanelLines.collapseBlank(shape)).size() + " rows");
        for (PanelLines.Row row : PanelLines.trimBlankEdges(PanelLines.collapseBlank(shape))) {
            System.out.println("        | " + row.text);
        }
        System.out.println("   a single blank line survives, which is the server grouping its blocks");

        System.out.println("---- the panel draws it when the server sends none");
        List<PanelLines.Raw> sidebar = new ArrayList<PanelLines.Raw>();
        sidebar.add(new PanelLines.Raw("\u00a7fPurse: \u00a76107,259,158.6", 9, false));
        sidebar.add(new PanelLines.Raw("\u00a7fBank: \u00a760/0", 8, false));
        for (PanelLines.Row row : PanelLines.build(sidebar, null, null,
                java.util.Collections.<String>emptySet())) {
            System.out.println("   " + row.slot + "   | " + row.text.replace('\u00a7', '&'));
        }
        System.out.println("   powerLineOn : " + PanelLines.powerLineOn());

        System.out.println("---- the accessory power rides along, and its switch takes it off");
        PowerStone.learnMagical("61,398", "the test");
        System.out.println("   stored       : " + PowerStone.stored() + "  /  " + PowerStone.magical());
        for (PanelLines.Row row : PanelLines.build(sidebar, null, null,
                java.util.Collections.<String>emptySet())) {
            if ("POWER".equals(row.slot)) System.out.println("   with it on   : " + row.text.replace('\u00a7', '&'));
        }
        SweatsEnhancements.cfg.setBool("scoreboard.accessoryPower", false);
        for (PanelLines.Row row : PanelLines.build(sidebar, null, null,
                java.util.Collections.<String>emptySet())) {
            if ("POWER".equals(row.slot)) System.out.println("   with it off  : " + row.text.replace('\u00a7', '&'));
        }
        SweatsEnhancements.cfg.setBool("scoreboard.accessoryPower", true);

        System.out.println("---- switched off, the line goes");
        SweatsEnhancements.cfg.setBool("scoreboard.powerFromBag", false);
        for (PanelLines.Row row : PanelLines.build(sidebar, null, null,
                java.util.Collections.<String>emptySet())) {
            if ("POWER".equals(row.slot)) System.out.println("   still drawn: " + row.text);
        }
        System.out.println("   (nothing above means it is gone)");
        SweatsEnhancements.cfg.setBool("scoreboard.powerFromBag", true);

        System.out.println("---- forget it, and nothing is drawn");
        SweatsEnhancements.cfg.remove(PowerStone.KEY);
        boolean drew = false;
        for (PanelLines.Row row : PanelLines.build(sidebar, null, null,
                java.util.Collections.<String>emptySet())) {
            if ("POWER".equals(row.slot)) drew = true;
        }
        System.out.println("   power line drawn with nothing learned: " + drew);
        System.out.println("   probe state                          : " + PowerStone.probeState());

        System.out.println("---- the config screen's build cost");
        long t0 = System.nanoTime();
        com.sweatsenhancements.gui.SweatsConfig config = new com.sweatsenhancements.gui.SweatsConfig();
        config.loadFromCfg();
        io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor<com.sweatsenhancements.gui.SweatsConfig> processor =
                io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor.withDefaults(config);
        new io.github.notenoughupdates.moulconfig.processor.ConfigProcessorDriver(processor)
                .processConfig(config);
        io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor<com.sweatsenhancements.gui.SweatsConfig> editor =
                new io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor<com.sweatsenhancements.gui.SweatsConfig>(processor);
        long ms = (System.nanoTime() - t0) / 1_000_000L;
        System.out.println("   one build (processor + " + processor.getAllCategories().size()
                + " categories + editor ctor): " + ms + " ms");
        // no editor.render() here: it asks LWJGL for the keyboard, which the offline harness does
        // not have. MenuTest already builds every editor through the same pipeline.
        System.out.println("   the editor is built and holds the options: " + (editor != null));
    }
}
