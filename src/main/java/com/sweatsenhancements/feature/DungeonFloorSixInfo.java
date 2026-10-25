package com.sweatsenhancements.feature;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Observed F6 phase boundaries and server-provided map/end-of-run facts; no spawn prediction. */
public final class DungeonFloorSixInfo {
    private Object world;
    private int floor = -1;
    private int chatCursor;
    private long lastRead;
    private long terraStart, terraEnd;
    private String golemWhen = "";

    public static String clean(String text) {
        String s = EnumChatFormatting.getTextWithoutFormattingCodes(text == null ? "" : text);
        return s == null ? "" : s.trim();
    }
    public static boolean terraStart(String raw) {
        String s = clean(raw).toLowerCase(Locale.ROOT);
        return s.contains("[boss] sadan:") && s.contains("my terracotta army");
    }
    public static boolean terraEnd(String raw) {
        String s = clean(raw).toLowerCase(Locale.ROOT);
        return s.contains("[boss] sadan:") && s.matches(".*: enough!.*");
    }
    public static boolean golemCue(String raw) {
        String s = clean(raw).toLowerCase(Locale.ROOT);
        return s.contains("[boss] sadan:") && s.contains("golem")
                && (s.contains("waking") || s.contains("wake up") || s.contains("woke"));
    }
    private static final java.util.regex.Pattern DUNGEON_FLOOR =
            java.util.regex.Pattern.compile("(?i)Catacombs \\((?:F|M)([1-7])\\)");
    public static int floorFromRows(List<String> rows) {
        if (rows == null) return -1;
        for (String row : rows) {
            String s = clean(row);
            java.util.regex.Matcher m = DUNGEON_FLOOR.matcher(s);
            if (m.find()) return Integer.parseInt(m.group(1));
        }
        return -1;
    }
    private boolean on(String id) { return SweatsEnhancements.cfg != null && SweatsEnhancements.cfg.getBool(Cfg.key(id),false); }
    private void reset() {
        world = null; floor = -1; terraStart = terraEnd = lastRead = 0;
        chatCursor = SweatsEnhancements.chat == null ? 0 : SweatsEnhancements.chat.size();
        golemWhen = "";
    }
    private void worldCheck() {
        if (SweatsEnhancements.mc == null || SweatsEnhancements.mc.theWorld == null) { if (world != null) reset(); return; }
        if (world != SweatsEnhancements.mc.theWorld) { reset(); world = SweatsEnhancements.mc.theWorld; }
    }
    private List<String> sidebar() {
        List<String> lines = new ArrayList<String>();
        if (SweatsEnhancements.mc == null || SweatsEnhancements.mc.theWorld == null) return lines;
        if (CustomScoreboard.on() && SweatsEnhancements.scoreboard != null) return SweatsEnhancements.scoreboard.serverSidebarLines();
        Scoreboard board = SweatsEnhancements.mc.theWorld.getScoreboard();
        ScoreObjective objective = board == null ? null : board.getObjectiveInDisplaySlot(1);
        if (objective != null) for (Score score : board.getSortedScores(objective)) {
            String name = score.getPlayerName();
            if (name != null) lines.add(ScorePlayerTeam.formatPlayerName(board.getPlayersTeam(name),name));
        }
        return lines;
    }
    private void refresh() {
        try {
            worldCheck();
            if (world == null) return;
            long now = System.currentTimeMillis();
            if (now - lastRead < 300) return;
            lastRead = now;
            int observed = floorFromRows(sidebar());
            if (observed > 0) floor = observed;
            if (SweatsEnhancements.chat == null) return;
            List<String> history = SweatsEnhancements.chat.raw();
            for (int i=Math.max(chatCursor,history.size()-100);i<history.size();i++) accept(history.get(i),now);
            chatCursor = history.size();
        } catch (Throwable t) { System.out.println("[SweatsEnhancements] dungeon phase data failed: " + t); }
    }
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) refresh();
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onChat(ClientChatReceivedEvent event) {
        if (event == null || event.type == 2 || event.message == null) return;
        try {
            worldCheck();
            if (world == null) return;
            int observed = floorFromRows(sidebar());
            if (observed > 0) floor = observed;
            accept(event.message.getUnformattedText(),System.currentTimeMillis());
        } catch (Throwable t) { System.out.println("[SweatsEnhancements] dungeon phase chat failed: " + t); }
    }
    private void accept(String raw, long now) {
        String s = clean(raw);
        if (floor < 1) return;
        if (floor == 6) {
            if (golemCue(s) && golemWhen.isEmpty())
                golemWhen = terraStart == 0 ? "before army cue" : terraEnd == 0 ? "during army cue" : "after army cue";
            if (terraStart(s) && terraStart == 0) terraStart = now;
            if (terraEnd(s) && terraStart > 0 && terraEnd == 0) terraEnd = now;
        }

    }
    public List<HudLayout.Line> timerLines() {
        refresh();
        List<HudLayout.Line> lines = new ArrayList<HudLayout.Line>();
        if (world == null || floor != 6 || terraStart == 0) return lines;
        long seconds = Math.max(0,(terraEnd > 0 ? terraEnd : System.currentTimeMillis())-terraStart)/1000;
        lines.add(new HudLayout.Line((terraEnd > 0 ? "§aTerra ended §f" : "§6Terra elapsed §f")
                + String.format(Locale.ROOT,"%02d:%02d",seconds/60,seconds%60),0xffffff));
        if (!golemWhen.isEmpty()) lines.add(new HudLayout.Line("§7Golem cue " + golemWhen,0xffffff));
        return lines;
    }
    public static boolean dungeonMap(ItemStack stack) {
        NBTTagCompound a = ItemDataTooltips.attributes(stack);
        return a != null && "DUNGEON_MAP".equalsIgnoreCase(a.getString("id"));
    }
    @SubscribeEvent
    public void onUnload(WorldEvent.Unload event) { if (event.world != null && event.world.isRemote) reset(); }
}
