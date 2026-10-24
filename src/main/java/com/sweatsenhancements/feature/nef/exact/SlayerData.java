/* NEF SkyblockData Slayer fields and ScoreboardUtils Slayer parsing,
 * f80cddf6 LGPL-3.0; SweatsEnhancements supplies the received sidebar instead of NEF's tab parser. */
package com.sweatsenhancements.feature.nef.exact;
import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.feature.CustomScoreboard;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.StringUtils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import java.util.ArrayList;
import java.util.List;

public final class SlayerData {
    private static boolean slayerActive, bossActive;
    private static Slayer currentSlayer=Slayer.NONE;
    private static int slayerLevel,slayerXp,nextLevelXp,xpToNextLevel,sessionBosses;
    private static float rngesusMeter;
    private static double totalSeconds;
    private static Location location=Location.NONE;
    private static Gamemode gamemode=Gamemode.LOBBY;
    public static Location getCurrentLocation(){return location;}
    public static Gamemode getCurrentGamemode(){return gamemode;}
    public static boolean isSkyblock(){return gamemode.isSkyblock();}
    private static long bossStart=-1;
    @SubscribeEvent public void chat(ClientChatReceivedEvent e){
        if(e.message==null || e.type==2)return;
        String text=StringUtils.stripControlCodes(e.message.getUnformattedText());
        if(text==null)return;
        java.util.regex.Matcher meter=java.util.regex.Pattern.compile("^RNGesus Meter:\\s*(\\d+(?:\\.\\d+)?)%",java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text);
        if(meter.find())try{rngesusMeter=Float.parseFloat(meter.group(1));}catch(NumberFormatException ignored){}
        java.util.regex.Matcher lvl=java.util.regex.Pattern.compile("^Slayer LVL\\s*(\\d+)",java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text);
        if(lvl.find())try{
            slayerLevel=Integer.parseInt(lvl.group(1));
            if(slayerLevel==9){slayerXp=nextLevelXp=xpToNextLevel=0;}
            else if(currentSlayer!=Slayer.NONE){
                java.util.regex.Matcher next=java.util.regex.Pattern.compile("Next LVL in\\s*([\\d,]+)").matcher(text);
                if(next.find()){
                    xpToNextLevel=Integer.parseInt(next.group(1).replace(",",""));
                    nextLevelXp=currentSlayer.getNextLevelXp(slayerLevel);
                    slayerXp=currentSlayer.getAccumulatedXp(slayerLevel,xpToNextLevel);
                }
            }
        }catch(RuntimeException ignored){}
        if(text.contains("SLAYER QUEST COMPLETE!")){
            if(bossStart>0){double seconds=(System.currentTimeMillis()-bossStart)/1000.0;
                if(seconds>=0 && seconds<3600){sessionBosses++;totalSeconds+=seconds;}}
            bossStart=-1;bossActive=false;
        }else if(text.contains("SLAYER QUEST FAILED!")){bossStart=-1;bossActive=false;}
    }
    private Object world;
    private int ticks;
    public static boolean isSlayerActive(){return slayerActive;}
    public static boolean isBossActive(){return bossActive;}
    public static Slayer getCurrentSlayer(){return currentSlayer;}
    public static int getSlayerLevel(){return slayerLevel;}
    public static int getSlayerXp(){return slayerXp;}
    public static int getNextLevelXp(){return nextLevelXp;}
    public static int getXpToNextLevel(){return xpToNextLevel;}
    public static int getSessionBosses(){return sessionBosses;}
    public static float getRNGesusMeter(){return rngesusMeter;}
    public static double getTotalSeconds(){return totalSeconds;}
    public static void setSlayerLevel(int v){slayerLevel=v;}
    public static void setSlayerXp(int v){slayerXp=v;}
    public static void setNextLevelXp(int v){nextLevelXp=v;}
    public static void setXpToNextLevel(int v){xpToNextLevel=v;}
    public static void setSessionBosses(int v){sessionBosses=v;}
    public static void setRNGesusMeter(float v){rngesusMeter=v;}
    public static void setTotalSeconds(double v){totalSeconds=v;}
    private static void setCurrentSlayer(Slayer newSlayer){
        if(currentSlayer==newSlayer)return;
        currentSlayer=newSlayer;resetSlayerData();
    }
    public static void resetSlayerData(){
        slayerLevel=slayerXp=nextLevelXp=xpToNextLevel=sessionBosses=0;
        rngesusMeter=0;totalSeconds=0;
    }
    private static void reset(){slayerActive=false;bossActive=false;currentSlayer=Slayer.NONE;
        location=Location.NONE;gamemode=Gamemode.LOBBY;bossStart=-1;resetSlayerData();}
    private static List<String> lines(){
        if(CustomScoreboard.on() && SweatsEnhancements.scoreboard!=null)return SweatsEnhancements.scoreboard.serverSidebarLines();
        List<String> result=new ArrayList<String>();Scoreboard board=SweatsEnhancements.mc.theWorld.getScoreboard();
        ScoreObjective objective=board==null?null:board.getObjectiveInDisplaySlot(1);
        if(objective!=null)for(Score score:board.getSortedScores(objective)){
            String name=score.getPlayerName();
            if(name!=null && !name.startsWith("#"))result.add(ScorePlayerTeam.formatPlayerName(board.getPlayersTeam(name),name));
        }
        return result;
    }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;
        if(SweatsEnhancements.mc==null || SweatsEnhancements.mc.theWorld==null || SweatsEnhancements.mc.thePlayer==null){world=null;reset();return;}
        if(world!=SweatsEnhancements.mc.theWorld){world=SweatsEnhancements.mc.theWorld;reset();}
        if(++ticks<10)return;ticks=0;
        if(SweatsEnhancements.tabList!=null){
            String server=SweatsEnhancements.tabList.server();
            if(server!=null && !server.isEmpty()){
                server=server.replaceFirst("-\\d+$","-");
                location=Location.getLocation(server);
            }
            String area=SweatsEnhancements.tabList.area();
            if(area!=null && !area.isEmpty())gamemode=Gamemode.SKYBLOCK;
        }
        boolean observed=false,observedBoss=false;
        Scoreboard board=SweatsEnhancements.mc.theWorld.getScoreboard();
        ScoreObjective title=board==null?null:board.getObjectiveInDisplaySlot(1);
        if(title!=null){String cleanTitle=StringUtils.stripControlCodes(title.getDisplayName());
            if(cleanTitle!=null)gamemode=Gamemode.getGamemode(cleanTitle);}
        if(gamemode==Gamemode.LOBBY && location!=Location.NONE)gamemode=Gamemode.SKYBLOCK;
        for(String raw:lines()){
            String clean=StringUtils.stripControlCodes(raw);
            if(clean==null)continue;clean=clean.trim();
            // NEF ScoreboardUtils Slayer detection, in its upstream order.
            if      (clean.startsWith("Voidgloom Seraph"))      {setCurrentSlayer(Slayer.VOIDGLOOM);observed=true;}
            else if (clean.startsWith("Inferno Demonlord"))     {setCurrentSlayer(Slayer.INFERNO);observed=true;}
            else if (clean.startsWith("Sven Packmaster"))       {setCurrentSlayer(Slayer.SVEN);observed=true;}
            else if (clean.startsWith("Revenant Horror"))       {setCurrentSlayer(Slayer.REVENANT);observed=true;}
            else if (clean.startsWith("Tarantula Broodfather")) {setCurrentSlayer(Slayer.TARANTULA);observed=true;}
            if(clean.indexOf("Slay the boss!")>=0){bossActive=true;observedBoss=true;if(bossStart<0)bossStart=System.currentTimeMillis();}
            if(clean.indexOf(") Kills")>=0 || clean.indexOf("Quest Failed")>=0 || clean.indexOf("Boss slain!")>=0)bossActive=false;
        }
        slayerActive=observed;
        if(!observed){currentSlayer=Slayer.NONE;bossActive=false;bossStart=-1;}
        else if(!observedBoss && !bossActive)bossActive=false;
    }
    @SubscribeEvent public void unload(WorldEvent.Unload e){if(e.world!=null && e.world.isRemote){world=null;reset();}}
}
