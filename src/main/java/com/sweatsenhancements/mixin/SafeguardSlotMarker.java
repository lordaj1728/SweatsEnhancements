package com.sweatsenhancements.mixin;

import com.sweatsenhancements.feature.ProtectedItems;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Small, always-visible green corner on safeguarded items in every container. */
@Mixin(GuiContainer.class)
public abstract class SafeguardSlotMarker {
    @Inject(method = "func_146977_a", at = @At("TAIL"), remap = false)
    private void sweatsenhancements$slotMarker(Slot slot, CallbackInfo ci) {
        if (slot == null || !slot.getHasStack() || !ProtectedItems.isProtected(slot.getStack())) return;
        int x = slot.xDisplayPosition + 12, y = slot.yDisplayPosition + 1;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT);
        try {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_LIGHTING);
            Gui.drawRect(x, y, x + 4, y + 7, 0xFF073519);
            Gui.drawRect(x + 1, y + 1, x + 3, y + 6, 0xFF55FF70);
        } finally { GL11.glPopAttrib(); }
    }
}
