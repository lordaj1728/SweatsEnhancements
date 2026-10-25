package com.sweatsenhancements.feature;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/** Manual requeue from an observed Catacombs floor, never a guessed default floor. */
public final class DungeonRequeue {
    private String candidate = "";
    private long firstSeen;
    private boolean keyWasDown;
    private static long lastSendAt;
    private static String lastSentCommand = "";

    /** Suppress duplicate client-bus callbacks or a rapid key/command collision. */
    public static synchronized boolean acceptSend(String command, long now) {
        if (command == null || command.isEmpty()) return false;
        if (command.equals(lastSentCommand) && now >= lastSendAt && now-lastSendAt < 1200) return false;
        lastSendAt = now;
        lastSentCommand = command;
        return true;
    }

    /** Only the observed F1-F7/M1-M7 Catacombs marker is accepted. */
    public static String floorMarker(List<String> lines) {
        if (lines == null) return "";
        for (String raw : lines) {
            String text = EnumChatFormatting.getTextWithoutFormattingCodes(raw == null ? "" : raw);
            if (text == null) continue;
            java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                    "(?:^|\\s)The Catacombs \\(([FM][1-7])\\)(?:$|\\s)").matcher(text);
            if (m.find()) return m.group(1);
        }
        return "";
    }

    public static String joinFor(String marker) {
        if (marker == null || !marker.matches("[FM][1-7]")) return null;
        return "/joindungeon " + marker.charAt(1) + (marker.charAt(0) == 'M' ? " master" : "");
    }

    public static String savedCommand(Cfg cfg) {
        if (cfg == null || !cfg.getBool(Cfg.key("DU002"), true)) return null;
        int floor = cfg.getInt("dungeonRequeue.floor", 0);
        if (floor < 1 || floor > 7) return null;
        return joinFor((cfg.getBool("dungeonRequeue.master", false) ? "M" : "F") + floor);
    }

    public static void send() {
        try {
            // Commands execute while GuiChat may still be open. The key handler separately
            // requires currentScreen == null, but /se requeue must not silently abort here.
            if (SweatsEnhancements.mc == null || SweatsEnhancements.mc.thePlayer == null || SweatsEnhancements.mc.theWorld == null) return;
            String cmd = savedCommand(SweatsEnhancements.cfg);
            if (cmd == null) {
                SweatsEnhancements.mc.thePlayer.addChatMessage(new ChatComponentText(
                        "§eSweatsEnhancements: Requeue unavailable. Enable Dungeon Requeue and join a recognised F1–F7 or M1–M7 run first."));
                return;
            }
            if (!acceptSend(cmd,System.currentTimeMillis())) return;
            SweatsEnhancements.mc.thePlayer.addChatMessage(new ChatComponentText("§aSweatsEnhancements: Sending " + cmd));
            SweatsEnhancements.mc.thePlayer.sendChatMessage(cmd);
        } catch (Throwable t) { System.out.println("[SweatsEnhancements] dungeon requeue failed: " + t); }
    }

    /** Config-menu binding only; never registered in Minecraft Controls. */
    private void pollKey() {
        int key = SweatsEnhancements.cfg == null ? Keyboard.KEY_NONE
                : SweatsEnhancements.cfg.getInt("shortcuts.dungeonRequeueKey", Keyboard.KEY_NONE);
        boolean down = key > Keyboard.KEY_NONE && key < Keyboard.KEYBOARD_SIZE
                && Keyboard.isCreated() && Keyboard.isKeyDown(key);
        if (down && !keyWasDown && SweatsEnhancements.mc != null && SweatsEnhancements.mc.currentScreen == null
                && SweatsEnhancements.mc.gameSettings != null && key != SweatsEnhancements.mc.gameSettings.keyBindDrop.getKeyCode())
            send();
        keyWasDown = down; // holding a key while leaving a menu cannot trigger a requeue
    }

    /** Registered only on the FML bus; 6 consecutive client ticks avoid a transient sidebar. */
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        try {
            pollKey();
            if (SweatsEnhancements.mc == null || SweatsEnhancements.mc.theWorld == null || SweatsEnhancements.mc.thePlayer == null || SweatsEnhancements.cfg == null) {
                candidate = ""; firstSeen = 0; return;
            }
            List<String> rows = new ArrayList<String>();
            if (CustomScoreboard.on() && SweatsEnhancements.scoreboard != null) {
                rows.addAll(SweatsEnhancements.scoreboard.serverSidebarLines());
            } else {
                Scoreboard board = SweatsEnhancements.mc.theWorld.getScoreboard();
                ScoreObjective obj = board == null ? null : board.getObjectiveInDisplaySlot(1);
                if (obj != null) {
                    rows.add(obj.getDisplayName());
                    for (Score score : board.getSortedScores(obj)) {
                        String name = score.getPlayerName();
                        if (name != null && !name.startsWith("#"))
                            rows.add(ScorePlayerTeam.formatPlayerName(board.getPlayersTeam(name), name));
                    }
                }
            }
            String marker = floorMarker(rows);
            long now = System.currentTimeMillis();
            if (marker.isEmpty()) { candidate = ""; firstSeen = 0; return; }
            if (!marker.equals(candidate) || now < firstSeen) {
                candidate = marker; firstSeen = now; return;
            }
            if (now - firstSeen < 300) return;
            int floor = marker.charAt(1) - '0';
            boolean master = marker.charAt(0) == 'M';
            if (SweatsEnhancements.cfg.getInt("dungeonRequeue.floor", 0) != floor
                    || SweatsEnhancements.cfg.getBool("dungeonRequeue.master", false) != master) {
                SweatsEnhancements.cfg.setInt("dungeonRequeue.floor", floor);
                SweatsEnhancements.cfg.setBool("dungeonRequeue.master", master);
            }
        } catch (Throwable t) { System.out.println("[SweatsEnhancements] dungeon floor observation failed: " + t); }
    }
}
