/* Adapted from NEF VoidgloomSeraph.java (f80cddf6), LGPL-3.0.
 * Its S23 block-change beacon/air state machine is retained. SweatsEnhancements handles rendering separately.
 */
package com.sweatsenhancements.feature.nef;
import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.feature.nef.exact.SlayerData;
import com.sweatsenhancements.feature.nef.exact.Slayer;
import com.sweatsenhancements.feature.nef.exact.SlayerRender;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import com.sweatsenhancements.feature.nef.exact.SlayerSettingsStore;
import com.sweatsenhancements.config.Cfg;
import com.sweatsenhancements.feature.DungeonAlertCard;
import com.sweatsenhancements.feature.DungeonRunHud;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockBeacon;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.util.BlockPos;

public final class VoidgloomSeraph {
    @SubscribeEvent public void renderWorld(RenderWorldLastEvent event){
        if(SweatsEnhancements.mc==null || SweatsEnhancements.mc.theWorld==null || SweatsEnhancements.mc.thePlayer==null || DungeonRunHud.isInDungeon()
                || !SlayerData.isBossActive() || SlayerData.getCurrentSlayer()!=Slayer.VOIDGLOOM)return;
        BlockPos pos=current();if(pos==null)return;
        boolean box=SlayerSettingsStore.get().beaconSettings.slayerShowBeaconPath;
        boolean tracer=SlayerSettingsStore.get().beaconSettings.showTracerToBeacon;
        if(!box&&!tracer)return;
        SlayerRender.begin();try{SlayerRender.beacon(pos,event.partialTicks,
            SlayerRender.color(SlayerSettingsStore.get().beaconSettings.slayerBeaconColor,new java.awt.Color(128,0,128)),box,tracer);
        }finally{SlayerRender.end();}
    }
    private static BlockPos waypoint;
    private static long placed;
    public static void processBlockChange(S23PacketBlockChange packetIn) {
        if (packetIn==null || SweatsEnhancements.cfg==null || SweatsEnhancements.mc==null || SweatsEnhancements.mc.thePlayer==null || SweatsEnhancements.mc.theWorld==null
                || DungeonRunHud.isInDungeon() || !SlayerData.isBossActive()
                || SlayerData.getCurrentSlayer()!=Slayer.VOIDGLOOM) return;
        if (!SlayerSettingsStore.get().beaconSettings.slayerShowBeaconPath && !SlayerSettingsStore.get().beaconSettings.showTracerToBeacon
                && !SlayerSettingsStore.get().beaconSettings.notifyBeaconInScreen) return;
        Block block=packetIn.getBlockState().getBlock();
        BlockPos position=packetIn.getBlockPosition();
        if (block instanceof BlockBeacon && block.getLocalizedName().contains("Beacon")) {
            if (SweatsEnhancements.mc.thePlayer.getDistanceSq(position)<32*32) {
                if (!position.equals(waypoint) && SlayerSettingsStore.get().beaconSettings.notifyBeaconInScreen) {
                    DungeonAlertCard.show("Beacon",0xFFFF5555);
                    SweatsEnhancements.mc.thePlayer.playSound("note.pling",1f,1f);
                }
                waypoint=position;placed=System.currentTimeMillis();
            }
        } else if (block instanceof BlockAir && position.equals(waypoint)) waypoint=null;
    }
    public static BlockPos current() {
        if (System.currentTimeMillis()-placed>20000L) waypoint=null;
        return waypoint;
    }
    public static void reset() {waypoint=null;placed=0;}
}
