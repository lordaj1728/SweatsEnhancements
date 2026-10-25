package com.sweatsenhancements.feature;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.scoreboard.Score;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Estimates the live dungeon score from observable client-side fields; never changes server score. */
public final class DungeonScoreHud {
    private static final Pattern FLOOR = Pattern.compile("Catacombs \\(([FM])([1-7])\\)");
    private static Object world;
    private static int floor, secret = -1, clear = -1, deaths = -1, crypts = -1, elapsed = -1;
    private static int failures;
    private static boolean bloodOpened, spiritReduction, paul;
    private static int ticks;
    private static final int[] SECRET_TARGET = {20,30,40,50,60,70,85,100};
    private static final int[] TIME_OFFSET = {-120,-120,-120,-120,-240,-120,-240,-360};

    private static void reset() {
        floor = 0; secret = clear = deaths = crypts = elapsed = -1;
        failures = 0; bloodOpened = spiritReduction = paul = false; ticks = 0;
    }
    public static int number(String row, String prefix) {
        if (!row.startsWith(prefix)) return -1;
        Matcher m = Pattern.compile("^\\s*(\\d+)").matcher(row.substring(prefix.length()).trim());
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }
    public static int time(String text) {
        String value = text.trim().replace("s", "");
        if (!value.matches("\\d{1,2}:\\d{2}(?::\\d{2})?")) return -1;
        String[] parts = value.split(":");
        int n = 0;
        for (String p : parts) n = n * 60 + Integer.parseInt(p);
        return n;
    }
    private static List<String> sidebar() {
        List<String> rows = new ArrayList<String>();
        if (SweatsEnhancements.mc == null || SweatsEnhancements.mc.theWorld == null) return rows;
        if (CustomScoreboard.on() && SweatsEnhancements.scoreboard != null) return SweatsEnhancements.scoreboard.serverSidebarLines();
        Scoreboard board = SweatsEnhancements.mc.theWorld.getScoreboard();
        ScoreObjective objective = board == null ? null : board.getObjectiveInDisplaySlot(1);
        if (objective != null) for (Score score : board.getSortedScores(objective)) {
            String name = score.getPlayerName();
            if (name != null && !name.startsWith("#")) rows.add(ScorePlayerTeam.formatPlayerName(board.getPlayersTeam(name),name));
        }
        return rows;
    }
    private static void read() {
        int currentFloor = 0, newClear = -1, newTime = -1;
        boolean master = false;
        for (String raw : sidebar()) {
            String s = DungeonFloorSixInfo.clean(raw);
            Matcher m = FLOOR.matcher(s);
            if (m.find()) { currentFloor = Integer.parseInt(m.group(2)); master = "M".equals(m.group(1)); }
            int value = number(s,"Dungeon Cleared:");
            if (value >= 0) newClear = Math.min(100,value);
            if (s.startsWith("Time Elapsed:")) newTime = time(s.substring("Time Elapsed:".length()));
        }
        if (currentFloor == 0) { reset(); return; }
        floor = currentFloor + (master ? 10 : 0);
        clear = newClear;
        elapsed = newTime;
        List<String> tab=new ArrayList<String>();
        if (SweatsEnhancements.tabList != null) for (TabListReader.Line line : SweatsEnhancements.tabList.lines()) tab.add(line.clean);
        int[] values=parseTab(tab);
        // The live player-info map is the server source even when the cached tab reader has
        // not run or is stale; no need to open the tab UI for the server to supply these rows.
        if (values[0]<0 || values[1]<0 || values[2]<0) {
            try {
                List<String> live=new ArrayList<String>();
                for (NetworkPlayerInfo info : SweatsEnhancements.mc.thePlayer.sendQueue.getPlayerInfoMap()) {
                    String row=SweatsEnhancements.mc.ingameGUI.getTabList().getPlayerName(info);
                    if (row != null) live.add(row);
                }
                int[] fresh=parseTab(live);
                for (int i=0;i<4;i++) if (fresh[i]>=0) values[i]=fresh[i];
            } catch (Throwable ignored) { /* missing live list remains unknown */ }
        }
        secret=values[0]; deaths=values[1]; crypts=values[2]; paul=values[3]==1;

    }
    /** Observed tab labels only. -1 means missing, never a guessed zero. */
    public static int[] parseTab(List<String> rows) {
        int[] result={-1,-1,-1,-1};
        if (rows==null) return result;
        for (String raw:rows) {
            String text=DungeonFloorSixInfo.clean(raw);
            if (text.startsWith("Secrets Found:") && text.contains("%"))
                result[0]=number(text,"Secrets Found:");
            if (text.startsWith("Deaths:")) {
                Matcher m=Pattern.compile("\\((\\d+)\\)").matcher(text);
                if (m.find()) result[1]=Integer.parseInt(m.group(1));
            }
            if (text.startsWith("Crypts:") && !text.contains("/"))
                result[2]=number(text,"Crypts:");
            if (text.startsWith("Current Mayor:") || text.startsWith("Winner:"))
                result[3]=text.toLowerCase(java.util.Locale.ROOT).contains("paul")?1:0;
        }
        return result;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (SweatsEnhancements.mc == null || SweatsEnhancements.mc.theWorld == null || SweatsEnhancements.mc.thePlayer == null) { reset(); world = null; return; }
        if (world != SweatsEnhancements.mc.theWorld) { reset(); world = SweatsEnhancements.mc.theWorld; }
        if (++ticks >= 10) { ticks = 0; try { read(); } catch (Throwable t) { reset(); } }
    }
    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (floor == 0 || event == null || event.type == 2 || event.message == null) return;
        String text = DungeonFloorSixInfo.clean(event.message.getUnformattedText());
        if (text.contains("PUZZLE FAIL!")) failures++;
        if (text.contains("The BLOOD DOOR has been opened!")) bloodOpened = true;
        if (text.equals("Your Spirit Pet reduced your death score penalty to 1!")) spiritReduction = true;
    }
    @SubscribeEvent public void onUnload(WorldEvent.Unload event) {
        if (event.world != null && event.world.isRemote) { reset(); world = null; }
    }
    public static boolean active() { return floor > 0 && world != null; }
    public static int secrets() { return secret; }
    public static int clear() { return clear; }
    public static int floor() { return floor; }
    public static int target(int f) { return SECRET_TARGET[(f > 10 ? f - 10 : f)]; }
    public static int skill(int deaths, int failures, boolean spirit) {
        return Math.max(0, Math.min(100, 100 - Math.max(0,deaths - (spirit ? 1 : 0))*2 - failures*14));
    }
    public static int speed(int seconds, int offset) {
        int adjusted = seconds + offset;
        if (adjusted < 480) return 100;
        if (adjusted < 600) return 140 - (int)Math.ceil(adjusted / 12.0);
        if (adjusted < 840) return 115 - (int)Math.ceil(adjusted / 24.0);
        if (adjusted < 1140) return 108 - (int)Math.ceil(adjusted / 30.0);
        if (adjusted < 3940) return (int)Math.ceil(98.5 - Math.ceil(adjusted / 40.0));
        return 0;
    }
    /** Estimated from observed sidebar/tab values. -1 means incomplete, never a guaranteed score. */
    public static int estimatedTotal() {
        int idx = floor > 10 ? floor - 10 : floor;
        if (!active() || idx < 1 || idx >= 8) return -1;
        int sk = deaths < 0 ? -1 : skill(deaths, failures, spiritReduction);
        int exp = clear < 0 || secret < 0 ? -1 : Math.min(60,(int)Math.floor(Math.min(100,clear + (bloodOpened ? 9 : 0)) * 0.6))
                + Math.min(40,(int)Math.floor(Math.min(100,secret) * 40.0 / target(floor)));
        int sp = elapsed < 0 ? -1 : speed(elapsed, floor == 16 ? -120 : floor > 10 && floor < 17 ? 0 : TIME_OFFSET[idx]);
        int bonus = crypts < 0 ? -1 : Math.min(5, crypts) + (paul ? 10 : 0);
        return sk < 0 || exp < 0 || sp < 0 || bonus < 0 ? -1 : sk + exp + sp + bonus;
    }
    public static List<HudLayout.Line> lines() {
        List<HudLayout.Line> out = new ArrayList<HudLayout.Line>();
        if (SweatsEnhancements.cfg == null || !SweatsEnhancements.cfg.getBool(Cfg.key("DU020"), false)) return out;
        if (!active()) {
            if (SweatsEnhancements.mc != null && DungeonMapHud.available(SweatsEnhancements.mc)) {
                out.add(new HudLayout.Line("§7Total score: §e? (§e?§e)",0xffffff));
                out.add(new HudLayout.Line("§7Waiting for dungeon sidebar",0xffffff));
            }
            return out;
        }
        int idx = (floor > 10 ? floor - 10 : floor);
        if (idx < 1 || idx >= 8) return out;
        int required = target(floor);
        int sk = deaths < 0 ? -1 : skill(deaths, failures, spiritReduction);
        int exp = clear < 0 || secret < 0 ? -1 : Math.min(60,(int)Math.floor(Math.min(100,clear + (bloodOpened ? 9 : 0)) * 0.6))
                + Math.min(40,(int)Math.floor(Math.min(100,secret) * 40.0 / required));
        int sp = elapsed < 0 ? -1 : speed(elapsed, floor == 16 ? -120 : floor > 10 && floor < 17 ? 0 : TIME_OFFSET[idx]);
        int bonus = crypts < 0 ? -1 : Math.min(5, crypts) + (paul ? 10 : 0);
        int total = estimatedTotal();
        String grade = total < 0 ? "?" : total < 100 ? "D" : total < 160 ? "C" : total < 230 ? "B" : total < 270 ? "A" : total < 300 ? "S" : "S+";
        String scoreColor = total < 0 ? "§e" : total >= 300 ? "§a" : total >= 230 ? "§e" : "§c";
        String gradeColor = total < 0 ? "§e" : total < 100 ? "§c" : total < 160 ? "§9" :
                total < 230 ? "§a" : total < 270 ? "§d" : "§6";
        out.add(new HudLayout.Line("§7Total score: " + scoreColor + fmt(total) + " "
                + gradeColor + "(" + grade + ")",0xffffff));
        out.add(new HudLayout.Line("",0xffffff));
        boolean compact = SweatsEnhancements.cfg.getBool("dungeon.scoreHudCompactNef", true);
        if (!compact) {
            out.add(new HudLayout.Line("§7Skill: " + componentColor(sk,100,94)+fmt(sk),0xffffff));
            out.add(new HudLayout.Line("§7Exploration: " + componentColor(exp,100,90)+fmt(exp),0xffffff));
            out.add(new HudLayout.Line("§7Speed: " + componentColor(sp,100,90)+fmt(sp),0xffffff));
            out.add(new HudLayout.Line("§7Bonus: " + componentColor(bonus,5,1)+fmt(bonus),0xffffff));
            out.add(new HudLayout.Line("",0xffffff));
        }
        int needed = sk < 0 || sp < 0 || bonus < 0 ? -1 : (int)Math.ceil((300.0 - 60 - sk - sp - bonus) * required / 40.0);
        if (needed > required) needed = -1;
        if (needed >= 0) needed = Math.max(0, needed);
        String secretColor = secret < 0 ? "§e" : needed >= 0 && secret >= needed ? "§a" : "§c";
        out.add(new HudLayout.Line("§7Secrets: " + secretColor + (secret < 0 ? "?" : secret + "%")
                + " §7/ " + (needed < 0 ? "§c?" : (secret >= needed ? "§a" : "§c") + needed + "%")
                + " §7/ §a" + required + "%",0xffffff));
        return out;
    }
    private static String componentColor(int value,int full,int near) { return value < 0 ? "§e" : value >= full ? "§a" : value >= near ? "§e" : "§c"; }
    private static String fmt(int value) { return value < 0 ? "?" : String.valueOf(value); }
}
