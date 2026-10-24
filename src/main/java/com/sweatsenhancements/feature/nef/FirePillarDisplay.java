/* Adapted from NEF FirePillarDisplay.java (f80cddf6), LGPL-3.0.
 * Same armor-stand text/seconds/hits parsing and sound timing; SweatsEnhancements gating/title API. */
package com.sweatsenhancements.feature.nef;
import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.feature.nef.exact.SlayerSettingsStore;
import com.sweatsenhancements.config.Cfg;
import com.sweatsenhancements.feature.nef.exact.SlayerData;
import com.sweatsenhancements.feature.DungeonAlertCard;
import com.sweatsenhancements.feature.DungeonRunHud;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;


public class FirePillarDisplay {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private EntityArmorStand trackedPillar;
    private long lastSoundTime;
    private static final String displayText = "";
    private static final long endTime = 0;

    @SubscribeEvent
    public void onRenderLiving(RenderLivingEvent.Pre<EntityLivingBase> event) {
        if (SweatsEnhancements.cfg == null || !SlayerSettingsStore.get().infernoDemonlord.slayerFirePillarDisplay || mc.theWorld == null || mc.thePlayer == null) return;
        if (DungeonRunHud.isInDungeon() || !SlayerData.isSlayerActive()
                || SlayerData.getCurrentSlayer()!=com.sweatsenhancements.feature.nef.exact.Slayer.INFERNO) return;

        if (mc.theWorld.getTotalWorldTime() % 5 != 0) return;

        mc.theWorld.getLoadedEntityList().stream()
                .filter(e -> e instanceof EntityArmorStand)
                .map(e -> (EntityArmorStand) e)
                .forEach(this::processArmorStand);
    }

    @SubscribeEvent public void onUnload(net.minecraftforge.event.world.WorldEvent.Unload event) {
        if (event.world != null && event.world.isRemote) { trackedPillar = null;lastSoundTime=0; }
    }
    private void processArmorStand(EntityArmorStand armorStand) {

        if (armorStand.getDisplayName() == null) return;


        String rawName = armorStand.getDisplayName().getUnformattedText();
        String cleanName = rawName.trim().replaceAll("§.", "");

        String[] parts = cleanName.split(" ");
        if (parts.length != 3) return;
        if (!parts[0].endsWith("s") || !parts[2].equals("hits")) return;


        if (trackedPillar == null || trackedPillar.isDead) {
            trackedPillar = armorStand;
            lastSoundTime = System.currentTimeMillis();
        }


        if (trackedPillar.equals(armorStand)) {
            updatePillarDisplay(cleanName);
        }
    }

    private void updatePillarDisplay(String cleanName) {

        int seconds;
        try { seconds = Integer.parseInt(cleanName.split(" ")[0].replace("s", "")); }
        catch (NumberFormatException ignored) { return; }


        DungeonAlertCard.show(trackedPillar.getDisplayName().getFormattedText(),0xFFFF8855);

        if (System.currentTimeMillis() - lastSoundTime > seconds * 150L) {
            mc.thePlayer.playSound("note.pling",1.0F,1.0F);
            lastSoundTime = System.currentTimeMillis();
        }
    }
}
