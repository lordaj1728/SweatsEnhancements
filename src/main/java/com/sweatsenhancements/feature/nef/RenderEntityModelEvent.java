/* NEF RenderEntityModelEvent adapted without Lombok; LGPL-3.0, revision f80cddf6. */
package com.sweatsenhancements.feature.nef;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.common.eventhandler.Event;
public final class RenderEntityModelEvent extends Event {
    private final EntityLivingBase entity;
    private final float limbSwing,limbSwingAmount,ageInTicks,headYaw,headPitch,scaleFactor;
    private final ModelBase model;
    public RenderEntityModelEvent(EntityLivingBase entity,float limbSwing,float limbSwingAmount,
            float ageInTicks,float headYaw,float headPitch,float scaleFactor,ModelBase model) {
        this.entity=entity;this.limbSwing=limbSwing;this.limbSwingAmount=limbSwingAmount;
        this.ageInTicks=ageInTicks;this.headYaw=headYaw;this.headPitch=headPitch;
        this.scaleFactor=scaleFactor;this.model=model;
    }
    public EntityLivingBase getEntity(){return entity;}
    public float getLimbSwing(){return limbSwing;}
    public float getLimbSwingAmount(){return limbSwingAmount;}
    public float getAgeInTicks(){return ageInTicks;}
    public float getHeadYaw(){return headYaw;}
    public float getHeadPitch(){return headPitch;}
    public float getScaleFactor(){return scaleFactor;}
    public ModelBase getModel(){return model;}
}
