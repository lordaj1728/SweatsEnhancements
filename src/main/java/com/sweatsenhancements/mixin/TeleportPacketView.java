package com.sweatsenhancements.mixin;

import com.sweatsenhancements.feature.SmoothTeleportView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public abstract class TeleportPacketView {
    private double sweatsenhancements$beforeX, sweatsenhancements$beforeY, sweatsenhancements$beforeZ;

    @Inject(method = "func_147258_a", at = @At("HEAD"), remap = false)
    private void sweatsenhancements$before(S08PacketPlayerPosLook packet, CallbackInfo ci) {
        if (Minecraft.getMinecraft().thePlayer != null) {
            sweatsenhancements$beforeX = Minecraft.getMinecraft().thePlayer.posX;
            sweatsenhancements$beforeY = Minecraft.getMinecraft().thePlayer.posY;
            sweatsenhancements$beforeZ = Minecraft.getMinecraft().thePlayer.posZ;
        }
    }

    @Inject(method = "func_147258_a", at = @At("TAIL"), remap = false)
    private void sweatsenhancements$after(S08PacketPlayerPosLook packet, CallbackInfo ci) {
        if (Minecraft.getMinecraft().thePlayer != null)
            SmoothTeleportView.confirmed(sweatsenhancements$beforeX, sweatsenhancements$beforeY, sweatsenhancements$beforeZ,
                    Minecraft.getMinecraft().thePlayer.posX,
                    Minecraft.getMinecraft().thePlayer.posY,
                    Minecraft.getMinecraft().thePlayer.posZ);
    }
}
