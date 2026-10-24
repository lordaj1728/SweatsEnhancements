/* Adapted from NEF HellionShieldFilter.java, LGPL-3.0, commit f80cddf6.
 * Only application config/location wiring changed; matching below is upstream code. */
package com.sweatsenhancements.feature.nef;
import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.feature.nef.exact.SlayerSettingsStore;
import com.sweatsenhancements.config.Cfg;
import com.sweatsenhancements.feature.DungeonRunHud;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;


public class HellionShieldFilter {

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (SweatsEnhancements.cfg == null || !SlayerSettingsStore.get().infernoDemonlord.slayerHellionShieldFilter) return;
        if (SweatsEnhancements.mc == null || SweatsEnhancements.mc.thePlayer == null || SweatsEnhancements.mc.theWorld == null || DungeonRunHud.isInDungeon()) return;
        // Keep the upstream message predicates; no false claim of Crimson area from absent data.

        if (event == null || event.message == null || event.type == 2) return;
        String msg = EnumChatFormatting.getTextWithoutFormattingCodes(event.message.getUnformattedText());
        if (msg.contains("Your hit was reduced by Hellion Shield!")
                || msg.contains("attunement on your dagger!")) {
            event.setCanceled(true);
        }
    }
}

