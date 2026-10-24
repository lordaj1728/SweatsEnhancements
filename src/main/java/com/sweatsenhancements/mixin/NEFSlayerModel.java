/* NEF MixinRendererLivingEntity post-layer event adapted for SweatsEnhancements's Slayer-only model listeners.
 * Original NEF source revision f80cddf6, LGPL-3.0. */
package com.sweatsenhancements.mixin;
import com.sweatsenhancements.feature.nef.RenderEntityModelEvent;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(RendererLivingEntity.class)
public abstract class NEFSlayerModel<T extends EntityLivingBase> {
    // The shipped 1.8.9 class has the SRG field name; no refmap is bundled.
    @Shadow(remap=false) protected ModelBase field_77045_g;
    @Inject(method="func_76986_a(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V",
            at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/entity/RendererLivingEntity;func_177093_a(Lnet/minecraft/entity/EntityLivingBase;FFFFFFF)V",shift=At.Shift.AFTER),remap=false,require=0)
    private void sweatsenhancements$afterLayers(T entity,double x,double y,double z,float entityYaw,float partialTicks,CallbackInfo ci) {
        float limbSwing=entity.limbSwing-entity.limbSwingAmount*(1f-partialTicks);
        float amount=entity.prevLimbSwingAmount+(entity.limbSwingAmount-entity.prevLimbSwingAmount)*partialTicks;
        float age=entity.ticksExisted+partialTicks;
        float headYaw=entity.prevRotationYawHead+(entity.rotationYawHead-entity.prevRotationYawHead)*partialTicks;
        float headPitch=entity.prevRotationPitch+(entity.rotationPitch-entity.prevRotationPitch)*partialTicks;
        MinecraftForge.EVENT_BUS.post(new RenderEntityModelEvent(entity,limbSwing,amount,age,headYaw,headPitch,.0625f,field_77045_g));
    }
}
