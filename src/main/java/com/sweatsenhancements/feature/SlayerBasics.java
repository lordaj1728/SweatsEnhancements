package com.sweatsenhancements.feature;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Conservative, opt-in slayer essentials. NEF's sidebar/timer and Hellion Shield
 * features informed behavior; the state and rendering are implemented independently.
 * Unknown sidebar data is never interpreted as an active boss or invented XP.
 */
public final class SlayerBasics {
    private Object world;
    private long start = -1;
    private boolean active;
    private String quest = "";
    private boolean questSeen;
    private final List<String> rows = new ArrayList<String>();
    private int ticks;
    private int sessionKills = 0;
    private long totalBossMs = 0;
    private String slayerLevel = "",rngPercent = "";

    private static boolean on(String id) {
        return SweatsEnhancements.cfg != null && SweatsEnhancements.cfg.getBool(Cfg.key(id),false);
    }
    public static boolean isBossRow(String raw) {
        String s = EnumChatFormatting.getTextWithoutFormattingCodes(raw == null ? "" : raw);
        return s != null && s.trim().equalsIgnoreCase("Slay the boss!");
    }
    public static boolean isSlayerQuestRow(String raw) {
        String s = EnumChatFormatting.getTextWithoutFormattingCodes(raw == null ? "" : raw);
        return s != null && s.toLowerCase(Locale.ROOT).contains("slayer quest");
    }
    public static boolean shieldSpam(String raw) {
        String s = EnumChatFormatting.getTextWithoutFormattingCodes(raw == null ? "" : raw);
        return s != null && (s.trim().equals("Your hit was reduced by Hellion Shield!")
                || (s.startsWith("You ") && s.endsWith("attunement on your dagger!")));
    }
    private void reset() { world = null; ticks = 0; start = -1; active = false; quest = ""; questSeen=false; rows.clear();sessionKills=0;totalBossMs=0;slayerLevel="";rngPercent=""; }
    public boolean bossActive() { return active; }
    public boolean questActive() { return questSeen || active; }
    public String questName() { return quest; }
    public int slayerLevel() {try {return Integer.parseInt(slayerLevel);}catch(Exception ignored){return 0;}}
    public String rngMeter() {return rngPercent;}
    public int sessionKills() {return sessionKills;}
    public long totalBossMillis() {return totalBossMs;}
    private void readSidebar() {
        rows.clear();
        if (CustomScoreboard.on() && SweatsEnhancements.scoreboard != null) {
            rows.addAll(SweatsEnhancements.scoreboard.serverSidebarLines()); return;
        }
        Scoreboard board = SweatsEnhancements.mc.theWorld.getScoreboard();
        ScoreObjective objective = board == null ? null : board.getObjectiveInDisplaySlot(1);
        if (objective != null) for (Score score : board.getSortedScores(objective)) {
            String name = score.getPlayerName();
            if (name != null && !name.startsWith("#"))
                rows.add(ScorePlayerTeam.formatPlayerName(board.getPlayersTeam(name), name));
        }
    }
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (SweatsEnhancements.mc == null || SweatsEnhancements.mc.theWorld == null || SweatsEnhancements.mc.thePlayer == null) { reset(); return; }
        if (world != SweatsEnhancements.mc.theWorld) { reset(); world = SweatsEnhancements.mc.theWorld; }
        if (++ticks < 10) return;
        ticks=0;
        if (!on("SL053") && !on("SL054") && !SlayerNefPort.anyEnabled()) return;
        readSidebar();
        boolean boss = false,slayer = false;
        String name = "";
        for (String row:rows) {
            boss |= isBossRow(row);
            if (isSlayerQuestRow(row)) slayer=true;
            String clean = EnumChatFormatting.getTextWithoutFormattingCodes(row);
            if (clean != null && clean.matches("(?i).*\\b(Revenant Horror|Tarantula Broodfather|Sven Packmaster|Voidgloom Seraph|Inferno Demonlord|Riftstalker Bloodfiend)\\b.*"))
                name=clean.trim();
        }
        if (!slayer && !boss && name.isEmpty()) { active=false;start=-1;quest="";questSeen=false; return; }
        questSeen=slayer || boss || !name.isEmpty();
        if (!name.isEmpty()) quest=name;
        if (boss && !active) { start=System.currentTimeMillis();active=true; }
        if (!boss) { active=false;start=-1; }
    }
    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (event == null || event.message == null || event.type==2) return;
        String text = EnumChatFormatting.getTextWithoutFormattingCodes(event.message.getUnformattedText());
        if (text == null) return;
        // SL055 is handled by the directly ported NEF HellionShieldFilter.
        java.util.regex.Matcher meter=java.util.regex.Pattern.compile("^RNGesus Meter:\\s*(\\d+(?:\\.\\d+)?)%",java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text);
        if (meter.find()) rngPercent=meter.group(1)+"%";
        java.util.regex.Matcher lvl=java.util.regex.Pattern.compile("^Slayer LVL\\s*(\\d+)",java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text);
        if (lvl.find()) slayerLevel=lvl.group(1);
        if (on("SL070") && (text.trim().equals("SLAYER QUEST STARTED!") || text.startsWith("RNGesus Meter:")
                || text.startsWith("Slayer LVL"))) event.setCanceled(true);
        if (text.contains("SLAYER QUEST COMPLETE!") || text.contains("SLAYER QUEST FAILED!")) {
            if (active && start > 0 && text.contains("SLAYER QUEST COMPLETE!")) {
                long elapsed=System.currentTimeMillis()-start;
                if (elapsed >= 0 && elapsed < 3600000L) {
                    sessionKills++;totalBossMs+=elapsed;
                    // SL054 is handled by the directly ported NEF SlayerTimer.
                }
            }
            active=false;start=-1;quest="";questSeen=false;
        }
    }
    // Replaced by directly adapted NEF SlayerOverlay; preserve state parsing above.
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || !on("SL053") || rows.isEmpty()
                || SweatsEnhancements.mc == null || SweatsEnhancements.mc.thePlayer == null || SweatsEnhancements.mc.gameSettings.showDebugInfo
                || SweatsEnhancements.mc.gameSettings.keyBindPlayerList.isKeyDown() || SweatsEnhancements.mc.currentScreen instanceof GuiChat) return;
        if (quest.isEmpty() && !active) return;
        ScaledResolution screen = new ScaledResolution(SweatsEnhancements.mc);
        int x=8,y=Math.min(70,Math.max(10,screen.getScaledHeight()/6));
        SweatsEnhancements.mc.fontRendererObj.drawStringWithShadow("§cSlayer §7"+(quest.isEmpty()?"Boss":quest),x,y,0xFFFFFFFF);
        if (active) SweatsEnhancements.mc.fontRendererObj.drawStringWithShadow("§7Slay the boss!",x,y+11,0xFFFFFFFF);
        int row=active?2:1;
        if (!slayerLevel.isEmpty()) {SweatsEnhancements.mc.fontRendererObj.drawStringWithShadow("§7Level: §c"+slayerLevel,x,y+row*11,0xFFFFFFFF);row++;}
        if (!rngPercent.isEmpty()) {SweatsEnhancements.mc.fontRendererObj.drawStringWithShadow("§dRNGesus Meter: "+rngPercent,x,y+row*11,0xFFFFFFFF);row++;}
        if (sessionKills>0) {
            SweatsEnhancements.mc.fontRendererObj.drawStringWithShadow("§7Bosses: §e"+sessionKills,x,y+row*11,0xFFFFFFFF);row++;
            SweatsEnhancements.mc.fontRendererObj.drawStringWithShadow(String.format(Locale.ROOT,"§7Average: §e%.1fs",totalBossMs/(1000.0*sessionKills)),x,y+row*11,0xFFFFFFFF);
        }
    }
    @SubscribeEvent
    public void onUnload(WorldEvent.Unload event) { if (event.world != null && event.world.isRemote) reset(); }
}
