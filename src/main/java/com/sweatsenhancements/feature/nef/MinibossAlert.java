/* Adapted from NEF MinibossAlert.java (f80cddf6), LGPL-3.0.
 * Upstream S29 sound packet predicate retained; SweatsEnhancements adds separate opt-in title/sound.
 */
package com.sweatsenhancements.feature.nef;
import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.feature.nef.exact.SlayerData;
import com.sweatsenhancements.feature.nef.exact.SlayerSettingsStore;
import com.sweatsenhancements.config.Cfg;
import com.sweatsenhancements.feature.DungeonRunHud;
import com.sweatsenhancements.feature.DungeonAlertCard;
import net.minecraft.network.play.server.S29PacketSoundEffect;
public final class MinibossAlert {
    private static long lastAlert;
    public static boolean isSpawnSound(String name,float volume,float pitch) {
        return "random.explode".equals(name) && volume==0.6f && pitch==9/7f;
    }
    public static void process(S29PacketSoundEffect packet) {
        if (packet == null || SweatsEnhancements.mc == null || SweatsEnhancements.mc.theWorld == null || SweatsEnhancements.mc.thePlayer == null
                || SweatsEnhancements.cfg == null || DungeonRunHud.isInDungeon() || !SlayerData.isSlayerActive()) return;
        if (isSpawnSound(packet.getSoundName(),packet.getVolume(),packet.getPitch())) {
            if (System.currentTimeMillis()-lastAlert<700L) return;
            lastAlert=System.currentTimeMillis();
            if (SlayerSettingsStore.get().minibossSettings.slayerMinibossTitle) DungeonAlertCard.show("MINIBOSS!",0xFFFF5555);
            if (SlayerSettingsStore.get().minibossSettings.slayerMinibossSound) SweatsEnhancements.mc.thePlayer.playSound("random.orb",1f,1f);
        }
    }
}
