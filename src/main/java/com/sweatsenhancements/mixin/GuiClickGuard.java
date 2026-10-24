package com.sweatsenhancements.mixin;

import com.sweatsenhancements.feature.ProtectedItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cancel dangerous GUI actions before PlayerControllerMP.windowClick transmits them. */
@Mixin(GuiContainer.class)
public abstract class GuiClickGuard {
    @Inject(method = "func_146984_a", at = @At("HEAD"), cancellable = true, remap = false)
    private void sweatsenhancements$protectClick(Slot slot, int slotId, int button, int mode, CallbackInfo ci) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;
        ItemStack cursor = mc.thePlayer.inventory.getItemStack();
        ItemStack clicked = slot == null ? null : slot.getStack();
        if (ProtectedItems.blockClick((GuiContainer) (Object) this, slotId, button, mode, cursor, clicked,
                mode == 2 && button >= 0 && button < 9 ? mc.thePlayer.inventory.getStackInSlot(button) : null)) ci.cancel();
    }
}
