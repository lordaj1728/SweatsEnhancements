/* Adapted from NEF SlayerOverlay.java (f80cddf6), LGPL-3.0.
 * NEF getLines/session structure and tab/chat visibility retained; SweatsEnhancements supplies observed
 * sidebar/chat state rather than NEF's global SkyblockData or unverified XP estimates.
 */
package com.sweatsenhancements.feature.nef;
import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.feature.nef.exact.SlayerSettingsStore;
import com.sweatsenhancements.config.Cfg;
import com.sweatsenhancements.feature.nef.exact.SlayerData;
import com.sweatsenhancements.feature.nef.exact.SlayerSettingsStore;
import com.sweatsenhancements.feature.nef.exact.Position;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import com.sweatsenhancements.feature.DungeonRunHud;
import net.minecraft.client.gui.GuiChat;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class SlayerOverlay {

    public boolean shouldShow() {
        if(SweatsEnhancements.mc==null || SweatsEnhancements.mc.thePlayer==null || SweatsEnhancements.mc.theWorld==null || SweatsEnhancements.cfg==null) return false;
        if(DungeonRunHud.isInDungeon() || !SlayerData.isSlayerActive()
                || SlayerData.getCurrentLocation()==com.sweatsenhancements.feature.nef.exact.Location.PRIVATE_ISLAND)return false;
        if(!SlayerSettingsStore.get().slayerOverlaySettings.slayerOverlay)return false;
        if(SlayerSettingsStore.get().slayerOverlaySettings.slayerOverlayHideOnTab && SweatsEnhancements.mc.gameSettings.keyBindPlayerList.isKeyDown())return false;
        if(SlayerSettingsStore.get().slayerOverlaySettings.slayerOverlayHideOnChat && SweatsEnhancements.mc.currentScreen instanceof GuiChat)return false;
        return true;
    }
    public List<String> getLines() {
        List<String> lines=new ArrayList<String>();
        int itemCount=0;
        String currentSlayer=SlayerData.getCurrentSlayer().getName();
        if(!currentSlayer.isEmpty()) lines.add("§4"+currentSlayer+"§r Slayer");
        if(SlayerData.getSlayerLevel()>0) {
            if(SlayerData.getSlayerLevel()==9)lines.add("§7Level: §aMAX");
            else lines.add("§7Level: §4"+SlayerData.getSlayerLevel());
            itemCount++;
        }
        if(SlayerData.getSlayerXp()>0 && SlayerData.getXpToNextLevel()>0){
            lines.add("§7XP: §c"+SlayerData.getSlayerXp()+" §7/§a "+SlayerData.getNextLevelXp());itemCount++;
        }
        if(SlayerData.getRNGesusMeter()>0) {
            lines.add("§dRNGesus Meter§7:§d "+String.valueOf(SlayerData.getRNGesusMeter())+"%");
            itemCount++;
        }
        if(SlayerData.getSessionBosses()>0) {
            lines.add("§7Bosses This Session: §e"+SlayerData.getSessionBosses());
            itemCount++;
        }
        if(SlayerData.getSessionBosses()>0 && (long)(SlayerData.getTotalSeconds()*1000)>0) {
            double averageTime=(long)(SlayerData.getTotalSeconds()*1000)/(1000.0*SlayerData.getSessionBosses());
            lines.add(String.format(Locale.ROOT,"§7Average Kill time: §e%.1fs",averageTime));
            itemCount++;
        }
        if(SlayerData.isBossActive()) {lines.add("§7Slay the boss!");itemCount++;}
        if(itemCount<1 && currentSlayer.isEmpty())return Collections.emptyList();
        return lines;
    }
    @SubscribeEvent public void onRender(RenderGameOverlayEvent.Post event) {
        if(event.type!=RenderGameOverlayEvent.ElementType.ALL || !shouldShow())return;
        List<String> lines=getLines();if(lines.isEmpty())return;
        float scale=Math.max(.5f,Math.min(5f,SlayerSettingsStore.get().slayerOverlaySettings.slayerOverlayScale));
        int widest=0;for(String line:lines)widest=Math.max(widest,SweatsEnhancements.mc.fontRendererObj.getStringWidth(line));
        Position pos=SlayerSettingsStore.get().slayerOverlaySettings.slayerOverlayPos;
        if(pos==null)return;
        int x=pos.getAbsX(event.resolution,(int)(widest*scale)),y=pos.getAbsY(event.resolution,(int)(lines.size()*11*scale));
        java.awt.Color bg= com.sweatsenhancements.feature.nef.exact.SlayerRender.color(
            SlayerSettingsStore.get().slayerOverlaySettings.slayerOverlayBackgroundColor,new java.awt.Color(0,0,0,0));
        if(bg.getAlpha()>0)net.minecraft.client.gui.Gui.drawRect(x-3,y-3,x+(int)(widest*scale)+4,
            y+(int)(lines.size()*11*scale)+2,bg.getRGB());
        GlStateManager.pushMatrix();
        try {GlStateManager.translate(x,y,0);GlStateManager.scale(scale,scale,1);
            int lineY=0;for(String line:lines){SweatsEnhancements.mc.fontRendererObj.drawStringWithShadow(line,0,lineY,0xFFFFFFFF);lineY+=11;}
        } finally {GlStateManager.popMatrix();}
    }
}
