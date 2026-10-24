package com.sweatsenhancements.mixin;

import com.sweatsenhancements.feature.nef.MinibossAlert;
import com.sweatsenhancements.feature.nef.VoidgloomSeraph;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.network.play.server.S23PacketBlockChange;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** SweatsEnhancements equivalent of NEF's packet-read event: packet is only acted on in the client thread. */
@Mixin(NetHandlerPlayClient.class)
public abstract class NEFSlayerPackets {
    @Inject(method="func_147234_a",at=@At("HEAD"),remap=false,require=0)
    private void sweatsenhancements$slayerBeacon(S23PacketBlockChange packet,CallbackInfo ci) {
        if (Minecraft.getMinecraft().isCallingFromMinecraftThread()) VoidgloomSeraph.processBlockChange(packet);
    }
    @Inject(method="func_147255_a",at=@At("HEAD"),remap=false,require=0)
    private void sweatsenhancements$slayerSound(S29PacketSoundEffect packet,CallbackInfo ci) {
        if (Minecraft.getMinecraft().isCallingFromMinecraftThread()) MinibossAlert.process(packet);
    }
}
