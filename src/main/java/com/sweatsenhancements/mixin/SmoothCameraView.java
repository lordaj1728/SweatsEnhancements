/*
 * Copyright (C) 2022 NotEnoughUpdates contributors.
 * Modified for SweatsEnhancements in 2026 from NEU 2.4.0 MixinEntityRenderer.java,
 * commit c47010a. Unrelated FOV/sensitivity and conflicting render-last redirect removed.
 * LGPL-3.0-or-later; see THIRD-PARTY-NEU-LGPL-3.0.txt.
 */
package com.sweatsenhancements.mixin;

import com.sweatsenhancements.feature.SmoothTeleportView;
import net.minecraft.client.renderer.EntityRenderer;
import org.lwjgl.util.vector.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Camera and world-pass visual coordinates without redirecting other mods' render-last hook. */
@Mixin(EntityRenderer.class)
public abstract class SmoothCameraView {
    private static double visual(int axis, double original) {
        Vector3f p = SmoothTeleportView.getCurrentPosition();
        return p == null ? original : axis == 0 ? p.x : axis == 1 ? p.y : p.z;
    }

    @ModifyVariable(method="func_78467_g", at=@At("STORE"), ordinal=0, remap=false)
    private double sweatsenhancements$cameraX(double d) { return visual(0, d); }
    @ModifyVariable(method="func_78467_g", at=@At("STORE"), ordinal=1, remap=false)
    private double sweatsenhancements$cameraY(double d) { return visual(1, d); }
    @ModifyVariable(method="func_78467_g", at=@At("STORE"), ordinal=2, remap=false)
    private double sweatsenhancements$cameraZ(double d) { return visual(2, d); }
    @ModifyVariable(method="func_175068_a", at=@At("STORE"), ordinal=0, remap=false)
    private double sweatsenhancements$worldX(double d) { return visual(0, d); }
    @ModifyVariable(method="func_175068_a", at=@At("STORE"), ordinal=1, remap=false)
    private double sweatsenhancements$worldY(double d) { return visual(1, d); }
    @ModifyVariable(method="func_175068_a", at=@At("STORE"), ordinal=2, remap=false)
    private double sweatsenhancements$worldZ(double d) { return visual(2, d); }

}
