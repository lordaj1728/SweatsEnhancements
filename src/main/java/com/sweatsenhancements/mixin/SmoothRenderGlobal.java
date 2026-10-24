/*
 * Copyright (C) 2022 NotEnoughUpdates contributors.
 * Modified for SweatsEnhancements in 2026 from NEU 2.4.0 MixinRenderGlobal.java,
 * commit c47010a. Unrelated cape-render hook removed.
 * LGPL-3.0-or-later; see THIRD-PARTY-NEU-LGPL-3.0.txt.
 */
package com.sweatsenhancements.mixin;

import com.sweatsenhancements.feature.SmoothTeleportView;
import net.minecraft.client.renderer.RenderGlobal;
import org.lwjgl.util.vector.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Terrain, entities and block damage must agree on the visual rendering origin. */
@Mixin(RenderGlobal.class)
public abstract class SmoothRenderGlobal {
    private static double visual(int axis, double original) {
        Vector3f p = SmoothTeleportView.getCurrentPosition();
        return p == null ? original : axis == 0 ? p.x : axis == 1 ? p.y : p.z;
    }
    @ModifyVariable(method="func_174970_a", at=@At("STORE"), ordinal=4, remap=false)
    private double sweatsenhancements$terrainX(double d) { return visual(0, d); }
    @ModifyVariable(method="func_174970_a", at=@At("STORE"), ordinal=5, remap=false)
    private double sweatsenhancements$terrainY(double d) { return visual(1, d); }
    @ModifyVariable(method="func_174970_a", at=@At("STORE"), ordinal=6, remap=false)
    private double sweatsenhancements$terrainZ(double d) { return visual(2, d); }
    @ModifyVariable(method="func_180446_a", at=@At("STORE"), ordinal=3, remap=false)
    private double sweatsenhancements$entitiesX(double d) { return visual(0, d); }
    @ModifyVariable(method="func_180446_a", at=@At("STORE"), ordinal=4, remap=false)
    private double sweatsenhancements$entitiesY(double d) { return visual(1, d); }
    @ModifyVariable(method="func_180446_a", at=@At("STORE"), ordinal=5, remap=false)
    private double sweatsenhancements$entitiesZ(double d) { return visual(2, d); }
    @ModifyVariable(method="func_174981_a", at=@At("STORE"), ordinal=0, remap=false)
    private double sweatsenhancements$damageX(double d) { return visual(0, d); }
    @ModifyVariable(method="func_174981_a", at=@At("STORE"), ordinal=1, remap=false)
    private double sweatsenhancements$damageY(double d) { return visual(1, d); }
    @ModifyVariable(method="func_174981_a", at=@At("STORE"), ordinal=2, remap=false)
    private double sweatsenhancements$damageZ(double d) { return visual(2, d); }
}
