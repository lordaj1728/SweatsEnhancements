package com.sweatsenhancements.mixin;

import com.sweatsenhancements.feature.ProtectedItems;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.item.EntityItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Stop the drop before the client sends its drop packet. */
@Mixin(EntityPlayerSP.class)
public abstract class PlayerDropGuard {
    @Inject(method = "func_71040_bB", at = @At("HEAD"), cancellable = true, remap = false)
    private void sweatsenhancements$protectDrop(boolean wholeStack, CallbackInfoReturnable<EntityItem> result) {
        EntityPlayerSP player = (EntityPlayerSP) (Object) this;
        if (ProtectedItems.isProtected(player.getHeldItem())) {
            ProtectedItems.blocked("Q drop");
            result.setReturnValue(null);
        }
    }
}
