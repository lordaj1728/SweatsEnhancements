package com.sweatsenhancements.cmd;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;

import java.util.List;
import com.sweatsenhancements.probe.Probe;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.EnumChatFormatting;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;

/**
 * The /se command tree. Lives here as plain Brigadier so the 1.21.11 build can reuse it
 * verbatim and only swap the bridge that feeds it.
 */
public final class SweatsCommands {

    private SweatsCommands() {
    }

    public static void register(CommandDispatcher<Object> d) {
        registerRoot(d, "se");
        registerRoot(d, "sweatsenhancements");
    }

    private static void registerRoot(CommandDispatcher<Object> d, String rootName) {
        d.register(literal(rootName)
                .executes(c -> help(c))

                .then(literal("help").executes(c -> help(c)))

                .then(literal("requeue").executes(c -> {
                    com.sweatsenhancements.feature.DungeonRequeue.send();
                    return 1;
                }))

                .then(literal("safeguard").executes(c -> {
                    net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
                    net.minecraft.item.ItemStack item = mc.thePlayer == null ? null : mc.thePlayer.getHeldItem();
                    Boolean result = com.sweatsenhancements.feature.ProtectedItems.toggle(item);
                    reply(c, result == null ? "§cThis item has no server UUID (or no item is held); cannot safeguard it."
                            : result ? "§aSafeguarding this UUID item, even when moved."
                            : "§eSafeguard removed from this item.");
                    return 1;
                }))

                .then(literal("menu").executes(c -> {
                    com.sweatsenhancements.gui.MoulMenu.open();
                    return 1;
                }))

                .then(literal("hud")
                        .executes(c -> {
                            net.minecraft.client.Minecraft.getMinecraft()
                                    .displayGuiScreen(new com.sweatsenhancements.gui.HudEditor());
                            return 1;
                        })
                        .then(literal("reset").executes(c -> {
                            com.sweatsenhancements.feature.HudLayout.resetAll();
                            SweatsEnhancements.cfg.save();
                            reply(c, EnumChatFormatting.YELLOW
                                    + "HUD elements back to the automatic stack.");
                            return 1;
                        })))

                .then(literal("tablist")
                        .executes(c -> {
                            reply(c, EnumChatFormatting.YELLOW + "Tab list: " + SweatsEnhancements.tabList.statusLine());
                            reply(c, EnumChatFormatting.YELLOW + "  " + SweatsEnhancements.tabList.summaryLine());
                            reply(c, EnumChatFormatting.GRAY + "  /se tablist lines|keys|sections|find <text>");
                            return 1;
                        })
                        .then(literal("lines").executes(c -> {
                            int i = 0;
                            for (com.sweatsenhancements.feature.TabListReader.Line l : SweatsEnhancements.tabList.lines()) {
                                reply(c, EnumChatFormatting.DARK_GRAY + "[" + l.section + "] "
                                        + EnumChatFormatting.GRAY + (i++) + "  " + l.clean);
                            }
                            reply(c, EnumChatFormatting.YELLOW + SweatsEnhancements.tabList.statusLine());
                            return 1;
                        }))
                        .then(literal("keys").executes(c -> {
                            for (com.sweatsenhancements.feature.TabListReader.Line l : SweatsEnhancements.tabList.lines()) {
                                int colon = l.clean.indexOf(':');
                                if (colon > 0) {
                                    reply(c, EnumChatFormatting.AQUA + "  " + l.clean.substring(0, colon)
                                            + EnumChatFormatting.GRAY + " = " + l.clean.substring(colon + 1));
                                }
                            }
                            return 1;
                        }))
                        .then(literal("sections").executes(c -> {
                            for (String s : SweatsEnhancements.tabList.sections()) {
                                reply(c, EnumChatFormatting.GRAY + "  section: " + EnumChatFormatting.YELLOW + s);
                            }
                            return 1;
                        }))
                        .then(literal("find")
                                .then(argument("text", StringArgumentType.greedyString()).executes(c -> {
                                    String needle = StringArgumentType.getString(c, "text");
                                    int n = 0;
                                    for (String l : SweatsEnhancements.tabList.containing(needle)) {
                                        reply(c, EnumChatFormatting.GRAY + "  " + l);
                                        n++;
                                    }
                                    reply(c, EnumChatFormatting.YELLOW + String.valueOf(n) + " line(s) matching " + needle);
                                    return 1;
                                }))))

                .then(literal("area").executes(c -> {
                    String area = SweatsEnhancements.tabList.area();
                    reply(c, EnumChatFormatting.YELLOW + "area: " + EnumChatFormatting.WHITE
                            + (area.isEmpty() ? "(not detected)" : area));
                    if (!SweatsEnhancements.tabList.server().isEmpty()) {
                        reply(c, EnumChatFormatting.YELLOW + "server: " + EnumChatFormatting.WHITE + SweatsEnhancements.tabList.server());
                    }
                    if (!SweatsEnhancements.tabList.mayor().isEmpty()) {
                        reply(c, EnumChatFormatting.YELLOW + "mayor: " + EnumChatFormatting.WHITE + SweatsEnhancements.tabList.mayor());
                    }
                    return 1;
                }))

                .then(literal("reload").executes(c -> {
                    SweatsEnhancements.chatTools.reload();
                    reply(c, EnumChatFormatting.GREEN + "Chat filters reloaded: "
                            + SweatsEnhancements.chatTools.ruleCount() + " rules.");
                    return 1;
                }))

                .then(literal("filters").executes(c -> {
                    reply(c, EnumChatFormatting.YELLOW + "Edit: " + SweatsEnhancements.chatTools.ruleFile().getAbsolutePath());
                    reply(c, EnumChatFormatting.GRAY + "  one rule per line: <FEATURE-ID> <regex>");
                    reply(c, EnumChatFormatting.GRAY + "  then /se reload");
                    return 1;
                }))

                .then(literal("version").executes(c -> version(c)))

                .then(literal("dump").executes(c -> Probe.dump(sender(c))))

                .then(literal("chat")
                        .then(literal("clear").executes(c -> {
                            SweatsEnhancements.chat.clear();
                            reply(c, EnumChatFormatting.GREEN + "Chat log cleared.");
                            return 1;
                        }))
                        .then(literal("count").executes(c -> {
                            reply(c, EnumChatFormatting.YELLOW + "Chat lines held: " + SweatsEnhancements.chat.size());
                            return 1;
                        })))

                .then(literal("probe")
                        .then(argument("on", BoolArgumentType.bool()).executes(c -> {
                            boolean on = BoolArgumentType.getBool(c, "on");
                            SweatsEnhancements.cfg.setBool(com.sweatsenhancements.config.Cfg.PROBE_ENABLED, on);
                            reply(c, EnumChatFormatting.YELLOW + "Probe " + (on ? "on" : "off") + ".");
                            return 1;
                        })))

                .then(literal("config")
                        .then(literal("list").executes(c -> {
                            reply(c, EnumChatFormatting.YELLOW + "Config: " + SweatsEnhancements.cfg.file().getAbsolutePath());
                            for (String key : SweatsEnhancements.cfg.keys()) {
                                reply(c, EnumChatFormatting.GRAY + "  " + key + " = "
                                        + SweatsEnhancements.cfg.get(key, ""));
                            }
                            return 1;
                        }))
                        .then(literal("get")
                                .then(argument("key", StringArgumentType.word()).executes(c -> {
                                    String key = StringArgumentType.getString(c, "key");
                                    reply(c, EnumChatFormatting.YELLOW + key + " = "
                                            + SweatsEnhancements.cfg.get(key, EnumChatFormatting.RED + "(no such key)"));
                                    return 1;
                                })))
                        .then(literal("toggle")
                                .then(argument("key", StringArgumentType.word()).executes(c -> {
                                    String key = StringArgumentType.getString(c, "key");
                                    String now = SweatsEnhancements.cfg.get(key, null);
                                    if (now == null) {
                                        reply(c, EnumChatFormatting.RED + "No such key: " + key);
                                        return 0;
                                    }
                                    boolean next = !Boolean.parseBoolean(now.trim());
                                    SweatsEnhancements.cfg.setBool(key, next);
                                    reply(c, EnumChatFormatting.GREEN + key + " = " + next);
                                    return 1;
                                })))));

        // A second register call on the same root. Brigadier merges the children of a literal that
        // is registered twice - checked by running it against 1.3.10 - so this command does not
        // have to be threaded into the chain above.
        d.register(literal(rootName)
                .then(literal("panels")
                        .executes(c -> panels(c))
                        .then(literal("on").executes(c -> panelsSet(c, true)))
                        .then(literal("off").executes(c -> panelsSet(c, false)))
                        .then(literal("reset").executes(c -> panelsReset(c))))
                .then(literal("sidebar")
                        .executes(c -> sidebar(c, false))
                        .then(literal("raw").executes(c -> sidebar(c, true))))
                .then(literal("elements")
                        .executes(c -> elements(c)))
                .then(literal("power")
                        .executes(c -> power(c, null))
                        .then(literal("clear").executes(c -> power(c, "")))
                        .then(argument("text", StringArgumentType.greedyString())
                                .executes(c -> power(c, StringArgumentType.getString(c, "text"))))));
    }

    /**
     * What both on-screen panels have seen since launch, and why nothing is on the screen.
     *
     * Written because "the panel does nothing" and "the panel is switched off" and "the panel has
     * nothing to draw" all look the same from the outside. This says which one it is, in chat.
     */
    private static int panels(CommandContext<Object> c) {
        try {
            reply(c, EnumChatFormatting.GOLD + "Panels " + EnumChatFormatting.GRAY
                    + "SweatsEnhancements " + SweatsEnhancements.VERSION);

            reply(c, EnumChatFormatting.YELLOW + "Custom Scoreboard "
                    + (com.sweatsenhancements.feature.CustomScoreboard.on()
                            ? EnumChatFormatting.GREEN + "ON" : EnumChatFormatting.RED + "off")
                    + EnumChatFormatting.GRAY + "   features.MS085 = "
                    + SweatsEnhancements.cfg.get(Cfg.key("MS085"), "(not in the file)"));
            reply(c, EnumChatFormatting.GRAY + "  sidebar objective: "
                    + quote(com.sweatsenhancements.feature.CustomScoreboard.probeObjective())
                    + " from display slot "
                    + String.valueOf(com.sweatsenhancements.feature.CustomScoreboard.probeSlot())
                    + "   lines read " + String.valueOf(com.sweatsenhancements.feature.CustomScoreboard.probeRead())
                    + ", drawn " + String.valueOf(com.sweatsenhancements.feature.CustomScoreboard.probeDrawn()));
            reply(c, EnumChatFormatting.GRAY + "  frames: Pre(ALL) "
                    + String.valueOf(com.sweatsenhancements.feature.CustomScoreboard.framesPre)
                    + ", Post(ALL) " + String.valueOf(com.sweatsenhancements.feature.CustomScoreboard.framesPost)
                    + ", Text " + String.valueOf(com.sweatsenhancements.feature.CustomScoreboard.framesText)
                    + "   client sidebar hidden " + String.valueOf(com.sweatsenhancements.feature.CustomScoreboard.hidFrames)
                    + "f, panel drawn " + String.valueOf(com.sweatsenhancements.feature.CustomScoreboard.drawFrames) + "f");
            reply(c, EnumChatFormatting.GRAY + "  last thing it did: " + state(com.sweatsenhancements.feature.CustomScoreboard.why)
                    + "   errors " + String.valueOf(com.sweatsenhancements.chat.Report.failures("scoreboard.draw"))
                    + ", hidden-without-drawing " + String.valueOf(com.sweatsenhancements.feature.CustomScoreboard.hiddenWithoutDrawing));
            reply(c, EnumChatFormatting.GRAY + "  settings: "
                    + com.sweatsenhancements.feature.CustomScoreboard.probeSettings());
            reply(c, EnumChatFormatting.GRAY + "  elements: "
                    + String.valueOf(com.sweatsenhancements.feature.CustomScoreboard.probeBuilt())
                    + " lines drawn, " + String.valueOf(com.sweatsenhancements.feature.CustomScoreboard.unknownCount)
                    + " server lines not recognised   /se elements lists them");

            reply(c, EnumChatFormatting.YELLOW + "Compact Tab List "
                    + (com.sweatsenhancements.feature.CustomTabList.on()
                            ? EnumChatFormatting.GREEN + "ON" : EnumChatFormatting.RED + "off")
                    + EnumChatFormatting.GRAY + "   features.MS063 = "
                    + SweatsEnhancements.cfg.get(Cfg.key("MS063"), "(not in the file)"));
            reply(c, EnumChatFormatting.GRAY + "  frames: Pre(PLAYER_LIST) "
                    + String.valueOf(com.sweatsenhancements.feature.CustomTabList.framesPreList)
                    + ", Post(PLAYER_LIST) " + String.valueOf(com.sweatsenhancements.feature.CustomTabList.framesPostList)
                    + ", Post(ALL) " + String.valueOf(com.sweatsenhancements.feature.CustomTabList.framesPostAll)
                    + ", Text " + String.valueOf(com.sweatsenhancements.feature.CustomTabList.framesText));
            reply(c, EnumChatFormatting.GRAY + "  built " + String.valueOf(com.sweatsenhancements.feature.CustomTabList.columnsBuilt)
                    + " columns from " + String.valueOf(com.sweatsenhancements.feature.CustomTabList.sectionsBuilt)
                    + " sections");
            reply(c, EnumChatFormatting.GRAY + "  TAB held " + String.valueOf(com.sweatsenhancements.feature.CustomTabList.tabFrames)
                    + "f, client list hidden " + String.valueOf(com.sweatsenhancements.feature.CustomTabList.cancelFrames)
                    + "f, list drawn " + String.valueOf(com.sweatsenhancements.feature.CustomTabList.drawFrames)
                    + "f   rows read " + String.valueOf(com.sweatsenhancements.feature.CustomTabList.readEntries)
                    + ", dropped " + String.valueOf(com.sweatsenhancements.feature.CustomTabList.droppedEntries));
            reply(c, EnumChatFormatting.GRAY + "  rebuilds skipped " 
                    + String.valueOf(com.sweatsenhancements.feature.CustomScoreboard.buildsSkipped)
                    + ", config writes " + String.valueOf(com.sweatsenhancements.config.Cfg.writes)
                    + (SweatsEnhancements.cfg != null && SweatsEnhancements.cfg.isDirty() ? " (a change is waiting)" : ""));
            reply(c, EnumChatFormatting.GRAY + "  last thing it did: " + state(com.sweatsenhancements.feature.CustomTabList.why)
                    + "   errors " + String.valueOf(com.sweatsenhancements.chat.Report.failures("tablist.draw")));
            reply(c, EnumChatFormatting.GRAY + "  settings: " + com.sweatsenhancements.feature.CustomTabList.probeSettings());

            reply(c, EnumChatFormatting.YELLOW + "Menu "
                    + EnumChatFormatting.GRAY + com.sweatsenhancements.gui.MoulMenu.menuState);
            reply(c, EnumChatFormatting.GRAY + "  if this reply arrived at all, the mod finished"
                    + " starting up, so both panels are registered; /se panels on|off|reset");
            return 1;
        } catch (Throwable t) {
            reply(c, EnumChatFormatting.RED + "The panels report failed: " + t);
            return 0;
        }
    }

    /**
     * Exactly what the scoreboard panel draws, line by line, with the element each one came from.
     *
     * The point of this command is that a missing line and a wrong line look identical on the
     * screen. An element with no data behind it does not appear at all, so a line that is in this
     * list was drawn, and a line that is not in it had nothing to draw from.
     */
    private static int elements(CommandContext<Object> c) {
        try {
            List<String> rows = com.sweatsenhancements.feature.CustomScoreboard.probeElements();
            reply(c, EnumChatFormatting.GOLD + "Scoreboard elements " + EnumChatFormatting.GRAY
                    + String.valueOf(rows.size()) + " lines drawn from "
                    + String.valueOf(com.sweatsenhancements.feature.CustomScoreboard.probeRawCount())
                    + " server lines, "
                    + String.valueOf(com.sweatsenhancements.feature.CustomScoreboard.unknownCount)
                    + " of them unrecognised");
            if (rows.isEmpty()) {
                reply(c, EnumChatFormatting.GRAY + "  nothing is being built right now - either no"
                        + " sidebar is in a display slot, or every line of it is empty");
                return 1;
            }
            int shown = 0;
            for (String row : rows) {
                if (shown++ >= 40) {
                    reply(c, EnumChatFormatting.GRAY + "  ... and "
                            + String.valueOf(rows.size() - 40) + " more");
                    break;
                }
                int split = row.indexOf('\u0001');
                String slot = split < 0 ? row : row.substring(0, split);
                String text = split < 0 ? "" : row.substring(split + 1);
                reply(c, EnumChatFormatting.YELLOW + "  " + slot + EnumChatFormatting.GRAY + "  " + text);
            }
            reply(c, EnumChatFormatting.GRAY + "  unknown lines: "
                    + join(com.sweatsenhancements.feature.CustomScoreboard.unknownSample));
            reply(c, EnumChatFormatting.GRAY + "  order and elements: Scoreboard \u00b7 Appearance;"
                    + " what exists at all: Scoreboard \u00b7 Select Tablist Lines");
            return 1;
        } catch (Throwable t) {
            reply(c, EnumChatFormatting.RED + "The elements report failed: " + t);
            return 0;
        }
    }

    /**
     * What the slots claimed, so the claim-and-reorder model can be seen working in game.
     *
     * Reads the client's sidebar out of its display slot whether or not the panel is switched on,
     * so this answers "do the patterns work on this server" on its own, separately from whether
     * the panel draws anything.
     */
    private static int sidebar(CommandContext<Object> c, boolean raw) {
        try {
            reply(c, EnumChatFormatting.GOLD + "Sidebar " + EnumChatFormatting.GRAY
                    + String.valueOf(com.sweatsenhancements.feature.CustomScoreboard.probeRawCount())
                    + " lines from the server, "
                    + String.valueOf(com.sweatsenhancements.feature.CustomScoreboard.probeLines().size())
                    + " drawn, objective " + quote(com.sweatsenhancements.feature.CustomScoreboard.probeObjective()));

            if (com.sweatsenhancements.feature.CustomScoreboard.probeRawCount() == 0) {
                reply(c, EnumChatFormatting.GRAY + "  nothing to show - no sidebar is in a display slot");
                return 1;
            }

            if (raw) {
                for (String line : com.sweatsenhancements.feature.CustomScoreboard.probeRaw()) {
                    String slot = com.sweatsenhancements.core.SidebarClaims.slotOf(line);
                    reply(c, EnumChatFormatting.DARK_GRAY + "  ["
                            + (slot == null ? "extra" : slot.toLowerCase()) + "] "
                            + EnumChatFormatting.GRAY + line);
                }
                return 1;
            }

            for (String slot : com.sweatsenhancements.feature.CustomScoreboard.probeOrder()) {
                List<String> held = com.sweatsenhancements.feature.CustomScoreboard.probeSlot(slot);
                if (held.isEmpty()) continue;
                reply(c, EnumChatFormatting.YELLOW + "  " + slot + EnumChatFormatting.GRAY + "  "
                        + join(held));
            }
            reply(c, EnumChatFormatting.GRAY + "  /se sidebar raw shows the server's own order,"
                    + " with the slot each line was claimed by");
            return 1;
        } catch (Throwable t) {
            reply(c, EnumChatFormatting.RED + "The sidebar report failed: " + t);
            return 0;
        }
    }

    private static String join(List<String> lines) {
        StringBuilder out = new StringBuilder();
        for (String line : lines) {
            if (out.length() > 0) out.append(EnumChatFormatting.DARK_GRAY).append("  |  ")
                    .append(EnumChatFormatting.GRAY);
            out.append(line);
        }
        return out.toString();
    }

    /** Switches both panels on or off without going through the menu. */
    private static int panelsSet(CommandContext<Object> c, boolean state) {
        try {
            SweatsEnhancements.cfg.setBool(Cfg.key("MS085"), state);
            SweatsEnhancements.cfg.setBool(Cfg.key("MS063"), state);
            reply(c, EnumChatFormatting.GREEN + "Custom Scoreboard and Compact Tab List switched "
                    + (state ? "on" : "off") + EnumChatFormatting.GRAY + " - written to "
                    + SweatsEnhancements.cfg.file().getName() + ".");
            if (state) {
                reply(c, EnumChatFormatting.GRAY
                        + "Hold TAB for the list. The scoreboard appears on the next sidebar update.");
                reply(c, EnumChatFormatting.GRAY + "Run /se panels again to see what they found.");
            }
            return 1;
        } catch (Throwable t) {
            reply(c, EnumChatFormatting.RED + "Could not switch the panels: " + t);
            return 0;
        }
    }

    /** Clears the counters and the once-per-session messages, for a fresh test. */
    private static int panelsReset(CommandContext<Object> c) {
        try {
            com.sweatsenhancements.feature.CustomScoreboard.resetDiagnostics();
            com.sweatsenhancements.feature.CustomTabList.resetDiagnostics();
            com.sweatsenhancements.chat.Report.forget();
            reply(c, EnumChatFormatting.GREEN + "Panel counters and one-time messages cleared.");
            return 1;
        } catch (Throwable t) {
            reply(c, EnumChatFormatting.RED + "Could not clear the panel counters: " + t);
            return 0;
        }
    }

    private static String quote(String text) {
        return text == null || text.isEmpty() ? "(none)" : "\"" + text + "\"";
    }

    private static String state(String why) {
        return why == null || why.isEmpty() ? "drawing" : why;
    }

    private static ICommandSender sender(CommandContext<Object> c) {
        return (ICommandSender) c.getSource();
    }

    private static void reply(CommandContext<Object> c, String text) {
        CommandBridge.reply(sender(c), text);
    }

    private static int help(CommandContext<Object> c) {
        reply(c, EnumChatFormatting.GOLD + "SweatsEnhancements " + EnumChatFormatting.YELLOW + SweatsEnhancements.VERSION);
        reply(c, EnumChatFormatting.YELLOW + "/se safeguard" + EnumChatFormatting.GRAY + " - safeguard the unique item you hold (or remove safeguard)");
        reply(c, EnumChatFormatting.YELLOW + "/se requeue" + EnumChatFormatting.GRAY + " - manually join the last observed Catacombs floor; key is set in /se → Dungeon");
        reply(c, EnumChatFormatting.YELLOW + "/se dump" + EnumChatFormatting.GRAY + " - write a report of what the server is sending");
        reply(c, EnumChatFormatting.YELLOW + "/se menu" + EnumChatFormatting.GRAY + " - the config menu (key P)");
        reply(c, EnumChatFormatting.YELLOW + "/se tablist" + EnumChatFormatting.GRAY + " - what the tab list reader sees");
        reply(c, EnumChatFormatting.YELLOW + "/se panels [on|off|reset]" + EnumChatFormatting.GRAY + " - the scoreboard and tab list panels, and what they have seen");
        reply(c, EnumChatFormatting.YELLOW + "/se sidebar" + EnumChatFormatting.GRAY + " - the server's sidebar lines and the slot each one landed in");
        reply(c, EnumChatFormatting.YELLOW + "/se area" + EnumChatFormatting.GRAY + " - detected area / server / mayor");
        reply(c, EnumChatFormatting.YELLOW + "/se filters" + EnumChatFormatting.GRAY + " - where the chat filter rules live");
        reply(c, EnumChatFormatting.YELLOW + "/se reload" + EnumChatFormatting.GRAY + " - re-read the filter rules");
        reply(c, EnumChatFormatting.YELLOW + "/se chat clear|count" + EnumChatFormatting.GRAY + " - chat capture");
        reply(c, EnumChatFormatting.YELLOW + "/se probe on|off" + EnumChatFormatting.GRAY + " - enable the probe");
        reply(c, EnumChatFormatting.YELLOW + "/se config list|get <key>|toggle <key>" + EnumChatFormatting.GRAY + " - settings");
        reply(c, EnumChatFormatting.YELLOW + "/se version");
        reply(c, EnumChatFormatting.GRAY + "Key " + EnumChatFormatting.YELLOW + "I" + EnumChatFormatting.GRAY + " also runs the dump.");
        return 1;
    }

    /**
     * The accessory power: what SweatsEnhancements has learned, and how to set or forget it by hand.
     *
     * `/se power` prints what is known and where it came from; `/se power clear` forgets it;
     * `/se power <text>` sets it, which is here because the two automatic sources are a chat notice
     * and the Thaumaturgy menu, and neither is available while looking at the scoreboard.
     */
    private static int power(CommandContext<Object> c, String setTo) {
        try {
            if (setTo != null) {
                if (setTo.trim().isEmpty()) {
                    if (SweatsEnhancements.cfg != null) SweatsEnhancements.cfg.remove(com.sweatsenhancements.feature.PowerStone.KEY);
                    reply(c, EnumChatFormatting.YELLOW + "Accessory power forgotten.");
                } else {
                    com.sweatsenhancements.feature.PowerStone.learn(setTo, "set by hand");
                    reply(c, EnumChatFormatting.YELLOW + "Accessory power set to "
                            + EnumChatFormatting.LIGHT_PURPLE + setTo);
                }
                return 1;
            }
            reply(c, EnumChatFormatting.GOLD + "Accessory power " + EnumChatFormatting.GRAY
                    + com.sweatsenhancements.feature.PowerStone.probeState());
            reply(c, EnumChatFormatting.GRAY + "  learned from the chat notice when you change your"
                    + " power, and from the Thaumaturgy menu's items");
            reply(c, EnumChatFormatting.GRAY + "  /se power <text> sets it, /se power clear forgets it");
            reply(c, EnumChatFormatting.GRAY + "  drawn " + (com.sweatsenhancements.feature.PanelLines.powerLineOn()
                    ? EnumChatFormatting.GREEN + "ON" : EnumChatFormatting.RED + "off")
                    + EnumChatFormatting.GRAY + " (Power From Accessory Bag)");
            return 1;
        } catch (Throwable t) {
            reply(c, EnumChatFormatting.RED + "The power report failed: " + t);
            return 1;
        }
    }

    private static int version(CommandContext<Object> c) {
        reply(c, EnumChatFormatting.GOLD + "SweatsEnhancements " + SweatsEnhancements.VERSION
                + EnumChatFormatting.GRAY + " (1.8.9)");
        return 1;
    }
}
