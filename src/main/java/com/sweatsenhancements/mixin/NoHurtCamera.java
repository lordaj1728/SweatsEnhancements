/* Adapted from NotEnoughFakepixel MixinEntityRenderer.java, im2pb/NotEnoughFakepixel
 * revision f80cddf6ac1e752bd402d1d963f3f987253b4795, LGPL-3.0.
 * Corresponding SweatsEnhancements source and THIRD-PARTY-NEF-LGPL-3.0.txt are included. */
package com.sweatsenhancements.mixin;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Disable only the damage-induced camera rotation, when explicitly enabled. */
@Mixin(EntityRenderer.class)
public abstract class NoHurtCamera {
    @Inject(method = "func_78482_e", at = @At("HEAD"), cancellable = true, remap = false)
    private void sweatsenhancements$noHurtCam(float partialTicks, CallbackInfo ci) {
        if (SweatsEnhancements.cfg != null && SweatsEnhancements.cfg.getBool(Cfg.key("MS227"), false)) ci.cancel();
    }
}
