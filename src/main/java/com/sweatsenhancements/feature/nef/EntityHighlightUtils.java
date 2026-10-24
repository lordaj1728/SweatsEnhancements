/* NEF EntityHighlightUtils.java, LGPL-3.0, revision f80cddf6; package/event adaptation. */
package com.sweatsenhancements.feature.nef;


import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public final class EntityHighlightUtils {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private EntityHighlightUtils() {}

    public static void renderEntityOutline(RenderEntityModelEvent event, Color color) {
        EntityLivingBase entity = event.getEntity();
        ModelBase model = event.getModel();

        GlStateManager.pushMatrix();
        GlStateManager.pushAttrib();

        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.depthMask(false);

        GlStateManager.color(
                color.getRed() / 255f,
                color.getGreen() / 255f,
                color.getBlue() / 255f,
                color.getAlpha() / 255f
        );

        float scaleFactor = 1.03f;
        GlStateManager.scale(scaleFactor, scaleFactor, scaleFactor);

        model.render(entity,
                event.getLimbSwing(),
                event.getLimbSwingAmount(),
                event.getAgeInTicks(),
                event.getHeadYaw(),
                event.getHeadPitch(),
                event.getScaleFactor());

        // Restore state
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.popAttrib();
        GlStateManager.popMatrix();
    }
}