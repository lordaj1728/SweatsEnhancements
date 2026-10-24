package com.sweatsenhancements.probe;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.feature.CustomScoreboard;
import com.sweatsenhancements.cmd.CommandBridge;
import com.sweatsenhancements.config.Cfg;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.command.ICommandSender;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;

/**
 * Writes a report of what Fakepixel is actually sending us.
 *
 * This exists because every feature after this point is written against the server's real
 * scoreboard lines, item names, lore and chat - and none of that can be guessed from a
 * the other server's client, because Fakepixel is a different server with different wording.
 *
 * Run it once in a lobby, once on each island you care about, and once with a menu open.
 */
public final class Probe {

    private Probe() {
    }

    public static int dump(ICommandSender sender) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null || mc.theWorld == null) {
                CommandBridge.reply(sender, EnumChatFormatting.RED + "Not in a world - nothing to report.");
                return 0;
            }
            String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
            File dir = new File(new File(mc.mcDataDir, "config"), "sweatsenhancements");
            if (!dir.exists()) dir.mkdirs();
            File out = new File(dir, "dump-" + stamp + ".txt");

            StringBuilder sb = new StringBuilder(64 * 1024);
            header(mc, sb);
            scoreboard(mc, sb);
            settings(sb);
            tabList(mc, sb);
            heldItem(mc, sb);
            inventory(mc, sb);
            entities(mc, sb);
            chat(mc, sb);
            actionBar(mc, sb);
            footer(sb);

            write(out, sb.toString());
            CommandBridge.reply(sender, EnumChatFormatting.GREEN + "SweatsEnhancements dump written:");
            CommandBridge.reply(sender, EnumChatFormatting.GRAY + "  " + out.getAbsolutePath());
            CommandBridge.reply(sender, EnumChatFormatting.GRAY + "  " + sb.length() + " chars - send this file back.");
            return 1;
        } catch (Throwable t) {
            CommandBridge.reply(sender, EnumChatFormatting.RED + "Dump failed: " + t);
            t.printStackTrace();
            return 0;
        }
    }

    // ---------------------------------------------------------------- sections

    private static void header(Minecraft mc, StringBuilder sb) {
        line(sb, "=== SweatsEnhancements PROBE DUMP ===");
        line(sb, "mod version   : " + SweatsEnhancements.VERSION + "  (map this file to the version that wrote it)");
        line(sb, "minecraft     : " + mc.getVersion());
        line(sb, "time          : " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        line(sb, "player        : " + mc.thePlayer.getName());
        line(sb, "uuid          : " + mc.thePlayer.getUniqueID());
        line(sb, "coords        : " + fmt(mc.thePlayer.posX) + " / " + fmt(mc.thePlayer.posY) + " / " + fmt(mc.thePlayer.posZ));
        line(sb, "dimension     : " + mc.thePlayer.dimension);
        ServerData sd = mc.getCurrentServerData();
        line(sb, "server        : " + (sd == null ? "(singleplayer)" : sd.serverIP));
        line(sb, "screen open   : " + (mc.currentScreen == null ? "(none)" : mc.currentScreen.getClass().getName()));
        line(sb, "health/food   : " + fmt(mc.thePlayer.getHealth()) + " hp / " + mc.thePlayer.getFoodStats().getFoodLevel() + " food");
        line(sb, "map HUD enabled: " + (SweatsEnhancements.cfg != null && SweatsEnhancements.cfg.getBool(Cfg.key("DU016"), false)));
        line(sb, "map HUD input  : " + com.sweatsenhancements.feature.DungeonMapHud.status(mc));
        line(sb, "key alert probe: " + com.sweatsenhancements.feature.DungeonSmallQoL.keyStatus());
        line(sb, "leap menu probe : " + (mc.currentScreen instanceof com.sweatsenhancements.thirdparty.skyvelor.gui.LeapScreen ? "Leap selector open" : "Leap selector not open"));
        line(sb, "slayer probe    : " + com.sweatsenhancements.feature.nef.exact.SlayerData.getCurrentSlayer()
                + " quest=" + com.sweatsenhancements.feature.nef.exact.SlayerData.isSlayerActive()
                + " boss=" + com.sweatsenhancements.feature.nef.exact.SlayerData.isBossActive()
                + " location=" + com.sweatsenhancements.feature.nef.exact.SlayerData.getCurrentLocation());
        if (SweatsEnhancements.dungeonAdditions != null)
            line(sb, "boss box target: " + SweatsEnhancements.dungeonAdditions.targetStatus());
        line(sb, "");
    }

    private static void scoreboard(Minecraft mc, StringBuilder sb) {
        line(sb, "=== SCOREBOARD ===");
        try {
            Scoreboard board = mc.theWorld.getScoreboard();
            for (int slot = 0; slot < 3; slot++) {
                ScoreObjective obj = board.getObjectiveInDisplaySlot(slot);
                String slotName = Scoreboard.getObjectiveDisplaySlot(slot);
                line(sb, "-- slot " + slot + " (" + slotName + "): "
                        + (obj == null ? "(empty)" : "\"" + obj.getDisplayName() + "\"  name=" + obj.getName()));
                if (obj == null) continue;
                List<Score> scores = new ArrayList<Score>(board.getSortedScores(obj));
                Collections.sort(scores, new Comparator<Score>() {
                    @Override
                    public int compare(Score a, Score b) {
                        return b.getScorePoints() - a.getScorePoints();
                    }
                });
                for (Score s : scores) {
                    line(sb, "   [" + s.getScorePoints() + "] " + s.getPlayerName());
                }
                line(sb, "   (" + scores.size() + " lines)");
            }

            // the custom scoreboard takes the sidebar out of its display slot, so the loop above
            // finds nothing while it is on. What it drew is reported from there instead.
            String customTitle = CustomScoreboard.probeTitle();
            if (customTitle != null && !customTitle.isEmpty()) {
                line(sb, "-- custom scoreboard (MS085): \"" + customTitle + "\"");
                for (String row : CustomScoreboard.probeLines()) {
                    line(sb, row);
                }
            }
        } catch (Throwable t) {
            line(sb, "   scoreboard read failed: " + t);
        }
        line(sb, "");
    }

    /**
     * What the panels were drawing with, so a report about a missing line can be answered from the
     * dump rather than guessed at.
     *
     * The player, twice: *"mayor stats in sb doesnt work"*, then *"its in tablist but not
     * scoreboard"*. Everything the election needs is in the two lists below - an element that is in
     * neither is claimed and never drawn - so the dump carries them, and what the panel actually
     * drew a moment ago.
     */
    private static void settings(StringBuilder sb) {
        line(sb, "=== SweatsEnhancements PANEL SETTINGS ===");
        try {
            if (SweatsEnhancements.cfg == null) {
                line(sb, "   (no config loaded)");
                line(sb, "");
                return;
            }
            line(sb, "custom scoreboard on : " + SweatsEnhancements.cfg.getBool("features.MS085", false));
            line(sb, "appearance           : " + SweatsEnhancements.cfg.get("scoreboard.appearance", ""));
            line(sb, "tab list lines       : " + SweatsEnhancements.cfg.get("scoreboard.tabLines", ""));
            line(sb, "events priority      : " + SweatsEnhancements.cfg.get("scoreboard.eventsPriority", ""));
            line(sb, "slayer meter         : " + SweatsEnhancements.cfg.getBool("scoreboard.slayerMeter", false));
            line(sb, "chunked stats        : " + SweatsEnhancements.cfg.get("scoreboard.chunkedStats", ""));
            line(sb, "tab lines come from  : " + CustomScoreboard.tabSource);
            line(sb, "tab list reader      : "
                    + (SweatsEnhancements.tabList == null ? "(none)" : SweatsEnhancements.tabList.statusLine()));
            line(sb, "tab list reader names: "
                    + (SweatsEnhancements.tabList == null ? "0" : String.valueOf(SweatsEnhancements.tabList.playerNames().size())));
            line(sb, "custom lines only    : " + SweatsEnhancements.cfg.getBool("scoreboard.customLines", false));
            line(sb, "");
            line(sb, "-- what the panel drew a moment ago");
            for (String row : CustomScoreboard.probeLines()) line(sb, row);
        } catch (Throwable t) {
            line(sb, "   settings read failed: " + t);
        }
        line(sb, "");
    }

    private static void tabList(Minecraft mc, StringBuilder sb) {
        line(sb, "=== TAB LIST ===");
        try {
            if (mc.getNetHandler() == null) {
                line(sb, "   (not connected)");
                line(sb, "");
                return;
            }
            List<NetworkPlayerInfo> infos = new ArrayList<NetworkPlayerInfo>(mc.getNetHandler().getPlayerInfoMap());
            line(sb, "   " + infos.size() + " entries");
            for (NetworkPlayerInfo info : infos) {
                String name = info.getGameProfile() == null ? "?" : info.getGameProfile().getName();
                String display = info.getDisplayName() == null ? "" : info.getDisplayName().getFormattedText();
                line(sb, "   " + name + "  |  " + display + "  |  ping " + info.getResponseTime()
                        + "  |  mode " + info.getGameType());
            }
        } catch (Throwable t) {
            line(sb, "   tab read failed: " + t);
        }
        line(sb, "");
    }

    private static void heldItem(Minecraft mc, StringBuilder sb) {
        line(sb, "=== HELD ITEM ===");
        try {
            ItemStack held = mc.thePlayer.getHeldItem();
            itemStack(sb, "   ", held);
        } catch (Throwable t) {
            line(sb, "   held item read failed: " + t);
        }
        line(sb, "");
    }

    private static void inventory(Minecraft mc, StringBuilder sb) {
        line(sb, "=== INVENTORY / OPEN CONTAINER ===");
        try {
            String title = containerTitle(mc);
            if (title != null) line(sb, "   container title: " + title);
            int shown = 0;
            for (Slot slot : mc.thePlayer.openContainer.inventorySlots) {
                ItemStack stack = slot.getStack();
                if (stack == null) continue;
                shown++;
                line(sb, "   slot " + slot.slotNumber + " (index " + slot.getSlotIndex() + ")");
                itemStack(sb, "      ", stack);
            }
            line(sb, "   " + shown + " non-empty slots");
        } catch (Throwable t) {
            line(sb, "   inventory read failed: " + t);
        }
        line(sb, "");
    }

    private static void entities(Minecraft mc, StringBuilder sb) {
        line(sb, "=== ENTITIES NEARBY ===");
        try {
            int radius = SweatsEnhancements.cfg.getInt(Cfg.PROBE_ENTITY_RADIUS, 24);
            List<Entity> list = new ArrayList<Entity>();
            for (Object o : mc.theWorld.loadedEntityList) {
                Entity e = (Entity) o;
                if (e == mc.thePlayer) continue;
                if (e.getDistanceToEntity(mc.thePlayer) <= radius) list.add(e);
            }
            Collections.sort(list, new Comparator<Entity>() {
                @Override
                public int compare(Entity a, Entity b) {
                    return Float.compare(a.getDistanceToEntity(Minecraft.getMinecraft().thePlayer),
                            b.getDistanceToEntity(Minecraft.getMinecraft().thePlayer));
                }
            });
            line(sb, "   " + list.size() + " within " + radius + " blocks (chat report only if big)");
            int shown = 0;
            for (Entity e : list) {
                if (shown++ >= 120) {
                    line(sb, "   ... " + (list.size() - 120) + " more");
                    break;
                }
                String hp = "";
                if (e instanceof EntityLivingBase) {
                    hp = "  hp " + fmt(((EntityLivingBase) e).getHealth())
                            + "/" + fmt(((EntityLivingBase) e).getMaxHealth());
                }
                String kind = (e instanceof EntityPlayer) ? "player" : e.getClass().getSimpleName();
                line(sb, "   " + fmt(e.getDistanceToEntity(mc.thePlayer)) + "m  " + kind
                        + "  name=\"" + e.getName() + "\"  display=\"" + e.getDisplayName().getFormattedText()
                        + "\"" + hp
                        + "  at " + fmt(e.posX) + "/" + fmt(e.posY) + "/" + fmt(e.posZ));
            }
        } catch (Throwable t) {
            line(sb, "   entity read failed: " + t);
        }
        line(sb, "");
    }

    private static void chat(Minecraft mc, StringBuilder sb) {
        line(sb, "=== CHAT (last 300, raw with colour codes) ===");
        try {
            List<String> lines = SweatsEnhancements.chat.tail(300);
            for (String s : lines) line(sb, "   " + s);
            line(sb, "   (" + lines.size() + " of " + SweatsEnhancements.chat.size() + " held)");
        } catch (Throwable t) {
            line(sb, "   chat read failed: " + t);
        }
        line(sb, "");
    }

    private static void actionBar(Minecraft mc, StringBuilder sb) {
        line(sb, "=== ACTION BAR (last 60) ===");
        try {
            List<String> lines = SweatsEnhancements.chat.actionBarTail(60);
            for (String s : lines) line(sb, "   " + s);
            line(sb, "   (" + lines.size() + " of " + SweatsEnhancements.chat.actionBarSize() + " held)");
        } catch (Throwable t) {
            line(sb, "   action bar read failed: " + t);
        }
        line(sb, "");
    }

    private static void footer(StringBuilder sb) {
        line(sb, "=== END ===");
    }

    // ---------------------------------------------------------------- helpers

    private static void itemStack(StringBuilder sb, String indent, ItemStack stack) {
        if (stack == null) {
            line(sb, indent + "(empty)");
            return;
        }
        try {
            line(sb, indent + "item: " + stack.getItem().getRegistryName() + "  x" + stack.stackSize
                    + "  meta " + stack.getMetadata());
            line(sb, indent + "name: " + stack.getDisplayName());
            line(sb, indent + "name(raw): " + stack.getDisplayName());
            try {
                List<String> tooltip = stack.getTooltip(Minecraft.getMinecraft().thePlayer,
                        Minecraft.getMinecraft().gameSettings.advancedItemTooltips);
                for (String t : tooltip) line(sb, indent + "  tooltip: " + t);
            } catch (Throwable t) {
                line(sb, indent + "  tooltip failed: " + t);
            }
            line(sb, indent + "nbt: " + (stack.getTagCompound() == null ? "(none)" : stack.getTagCompound().toString()));
        } catch (Throwable t) {
            line(sb, indent + "item read failed: " + t);
        }
    }

    /**
     * Reads the open container's title. GuiChest keeps its inventory private, and looking
     * the field up by name would break in production (names are remapped on build), so the
     * field is found by type instead - that survives obfuscation.
     */
    private static String containerTitle(Minecraft mc) {
        try {
            if (mc.currentScreen == null) return null;
            for (java.lang.reflect.Field f : mc.currentScreen.getClass().getDeclaredFields()) {
                if (!IInventory.class.isAssignableFrom(f.getType())) continue;
                f.setAccessible(true);
                Object value = f.get(mc.currentScreen);
                if (value instanceof IInventory) {
                    return ((IInventory) value).getDisplayName().getFormattedText();
                }
            }
        } catch (Throwable t) {
            return "(title unreadable: " + t + ")";
        }
        return null;
    }

    private static void line(StringBuilder sb, String s) {
        sb.append(s).append('\n');
    }

    private static String fmt(double d) {
        return String.format("%.2f", d);
    }

    private static void write(File out, String text) {
        Writer w = null;
        try {
            w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(out), "UTF-8"));
            w.write(text);
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] could not write dump: " + t);
        } finally {
            if (w != null) {
                try {
                    w.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }
}
