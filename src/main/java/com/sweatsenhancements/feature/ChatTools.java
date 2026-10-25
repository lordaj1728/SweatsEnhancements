package com.sweatsenhancements.feature;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;

import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Chat filters.
 *
 * Patterns live in config/sweatsenhancements/chatfilters.txt, one per line:
 *
 *     <FEATURE-ID> <regex>        hide messages matching the regex
 *     # comment                   ignored
 *
 * Each line is tagged with the id of the feature it belongs to, so the toggle in the config
 * menu controls it - and you can add your own patterns in game without waiting for a rebuild.
 * That is deliberate: Fakepixel's exact wording is not known yet, so this is the part that
 * gets tuned by playing rather than by guessing.
 */
public class ChatTools {

    /** one rule: which feature owns it, and what it matches */
    private static class Rule {
        final String feature;
        final Pattern pattern;

        Rule(String feature, Pattern pattern) {
            this.feature = feature;
            this.pattern = pattern;
        }
    }

    private static final List<Rule> RULES = new ArrayList<Rule>();
    private static final String FILE_NAME = "chatfilters.txt";

    private static final String[] DEFAULT_RULES = {
            "# SweatsEnhancements chat filters - one rule per line: <FEATURE-ID> <regex>",
            "# lines starting with # are ignored. Edit in game, then /se reload.",
            "",
            "# hide empty / whitespace-only chat lines",
            "CH031 ^\\s*$",
            "",
            "# fire sale reminders",
            "CH014 .*(Fire Sale|fire sale).*",
            "",
            "# powder mining spam",
            "CH034 ^.*(Mithril Powder|Gemstone Powder|Glacite Powder).*\\+.*",
    };

    private File file;

    public void init(File configDir) {
        File dir = new File(configDir, "sweatsenhancements");
        if (!dir.exists()) dir.mkdirs();
        this.file = new File(dir, FILE_NAME);
        writeDefaultsIfMissing();
        reload();
    }

    /** Re-reads the rule file. Called at startup and by /se reload. */
    public void reload() {
        RULES.clear();
        int bad = 0;
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(
                    new FileInputStream(file), StandardCharsets.UTF_8));
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int sp = line.indexOf(' ');
                if (sp <= 0) {
                    bad++;
                    continue;
                }
                String id = line.substring(0, sp).trim();
                // Old user files can still contain rules for removed switches. Never
                // run those rules just because an old config value was left enabled.
                if (!com.sweatsenhancements.config.Implemented.IDS.contains(id)) continue;
                String regex = line.substring(sp + 1).trim();
                try {
                    RULES.add(new Rule(id, Pattern.compile(regex)));
                } catch (PatternSyntaxException e) {
                    bad++;
                    System.out.println("[SweatsEnhancements] bad regex in " + FILE_NAME + ": " + regex + " (" + e.getMessage() + ")");
                }
            }
            r.close();
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] could not read " + FILE_NAME + ": " + t);
        }
        System.out.println("[SweatsEnhancements] chat filters: " + RULES.size() + " rules" + (bad > 0 ? ", " + bad + " skipped" : ""));
    }

    public int ruleCount() {
        return RULES.size();
    }

    public File ruleFile() {
        return file;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onChat(ClientChatReceivedEvent event) {
        try {
            if (event == null || event.message == null) return;
            if (event.type == 2) return;              // action bar handled elsewhere

            String text = EnumChatFormatting.getTextWithoutFormattingCodes(event.message.getUnformattedText());
            if (text == null) return;

            if (SweatsEnhancements.cfg.getBool(Cfg.key("CH031"), true) && text.trim().isEmpty()) {
                event.setCanceled(true);
                return;
            }

            // CH033 - the welcome banner every time you land on SkyBlock
            if (SweatsEnhancements.cfg.getBool(Cfg.key("CH033"), true)
                    && (text.contains("Welcome to Fakepixel") || text.contains("Welcome to SkyBlock"))) {
                event.setCanceled(true);
                return;
            }

            for (Rule rule : RULES) {
                if (!SweatsEnhancements.cfg.getBool(Cfg.key(rule.feature), true)) continue;
                if (rule.pattern.matcher(text).find()) {
                    event.setCanceled(true);
                    return;
                }
            }

        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] chat filter failed: " + t);
        }
    }

    private void writeDefaultsIfMissing() {
        if (file.exists()) return;
        try {
            java.io.Writer w = new java.io.OutputStreamWriter(new java.io.FileOutputStream(file), StandardCharsets.UTF_8);
            for (String s : DEFAULT_RULES) {
                w.write(s);
                w.write('\n');
            }
            w.close();
            System.out.println("[SweatsEnhancements] wrote default " + FILE_NAME);
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] could not write default filters: " + t);
        }
    }
}
