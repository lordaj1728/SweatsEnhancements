/* Adapted from NEF SlayerTimer.java, LGPL-3.0, commit f80cddf6.
 * Config and scoreboard access use SweatsEnhancements's received vanilla objective. */
package com.sweatsenhancements.feature.nef;
import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.feature.nef.exact.SlayerSettingsStore;
import com.sweatsenhancements.config.Cfg;
import com.sweatsenhancements.feature.DungeonRunHud;
import com.sweatsenhancements.feature.CustomScoreboard;
import java.util.ArrayList;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;


public class SlayerTimer {

    private long startTime = -1;
    private boolean isBossActive = false;

    private static boolean enabled() {
        return SweatsEnhancements.cfg != null && SlayerSettingsStore.get().bossSettings.slayerBossTimer
                && SweatsEnhancements.mc != null && SweatsEnhancements.mc.thePlayer != null && SweatsEnhancements.mc.theWorld != null
                && !DungeonRunHud.isInDungeon();
    }
    private static List<String> scoreboardLines() {
        if (CustomScoreboard.on() && SweatsEnhancements.scoreboard != null) return SweatsEnhancements.scoreboard.serverSidebarLines();
        List<String> result = new ArrayList<String>();
        Scoreboard board = SweatsEnhancements.mc.theWorld.getScoreboard();
        ScoreObjective objective = board == null ? null : board.getObjectiveInDisplaySlot(1);
        if (objective != null) for (Score score:board.getSortedScores(objective)) {
            String name=score.getPlayerName();
            if (name!=null && !name.startsWith("#"))
                result.add(ScorePlayerTeam.formatPlayerName(board.getPlayersTeam(name),name));
        }
        return result;
    }
    @SubscribeEvent public void onUnload(net.minecraftforge.event.world.WorldEvent.Unload event) {
        if (event.world != null && event.world.isRemote) {startTime=-1;isBossActive=false;}
    }
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (enabled()) {
            if (event.side == net.minecraftforge.fml.relauncher.Side.CLIENT) {
                List<String> sidebarLines = scoreboardLines();

                for (String line : sidebarLines) {
                    if (line.contains("Slay the boss!")) {
                        if (!isBossActive) {
                            startTime = System.currentTimeMillis();
                            isBossActive = true;
                        }
                        break;
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (enabled()) {
            String message = StringUtils.stripControlCodes(event.message.getUnformattedText());

            if (message.contains("SLAYER QUEST COMPLETE!")) {
                if (isBossActive) {
                    long endTime = System.currentTimeMillis();
                    long duration = endTime - startTime;

                    double seconds = duration / 1000.0;

                    String chatMessage = String.format("[SweatsEnhancements] Boss took %.3f seconds!", seconds);
                    ChatComponentText formattedMessage = new ChatComponentText(chatMessage);
                    formattedMessage.getChatStyle().setColor(EnumChatFormatting.GREEN);

                    net.minecraft.client.Minecraft.getMinecraft().thePlayer.addChatMessage(formattedMessage);

                    startTime = -1;
                    isBossActive = false;
                }
            }
            if (message.contains("SLAYER QUEST FAILED!")) {
                startTime = -1;
                isBossActive = false;
            }
        }
    }
}
