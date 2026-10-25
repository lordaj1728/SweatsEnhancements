package com.sweatsenhancements.feature;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import io.github.notenoughupdates.moulconfig.ChromaColour;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/** Configurable block outline, with an optional translucent full-block fill. No hitbox changes. */
public final class BlockOutline {
    public static final String DEFAULT_COLOUR = "0:150:51:187:255";

    public static int colour(String value) {
        try { return ChromaColour.forLegacyString(value).getEffectiveColourRGB(); }
        catch (Throwable ignored) { return 0x9633BBFF; }
    }

    private static void vertex(double x, double y, double z) { GL11.glVertex3d(x, y, z); }

    /** Draw all six cube faces in world-relative coordinates, not just the selected thin shape. */
    private static void fill(AxisAlignedBB b) {
        GL11.glBegin(GL11.GL_QUADS);
        vertex(b.minX,b.minY,b.minZ); vertex(b.maxX,b.minY,b.minZ); vertex(b.maxX,b.minY,b.maxZ); vertex(b.minX,b.minY,b.maxZ);
        vertex(b.minX,b.maxY,b.maxZ); vertex(b.maxX,b.maxY,b.maxZ); vertex(b.maxX,b.maxY,b.minZ); vertex(b.minX,b.maxY,b.minZ);
        vertex(b.minX,b.minY,b.minZ); vertex(b.minX,b.maxY,b.minZ); vertex(b.maxX,b.maxY,b.minZ); vertex(b.maxX,b.minY,b.minZ);
        vertex(b.maxX,b.minY,b.maxZ); vertex(b.maxX,b.maxY,b.maxZ); vertex(b.minX,b.maxY,b.maxZ); vertex(b.minX,b.minY,b.maxZ);
        vertex(b.minX,b.minY,b.maxZ); vertex(b.minX,b.maxY,b.maxZ); vertex(b.minX,b.maxY,b.minZ); vertex(b.minX,b.minY,b.minZ);
        vertex(b.maxX,b.minY,b.minZ); vertex(b.maxX,b.maxY,b.minZ); vertex(b.maxX,b.maxY,b.maxZ); vertex(b.maxX,b.minY,b.maxZ);
        GL11.glEnd();
    }

    @SubscribeEvent
    public void onHighlight(DrawBlockHighlightEvent event) {
        if (event == null || SweatsEnhancements.cfg == null || !SweatsEnhancements.cfg.getBool(Cfg.key("MS225"), false)
                || event.target == null || event.target.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK
                || SweatsEnhancements.mc == null || SweatsEnhancements.mc.theWorld == null || event.player == null) return;
        try {
            BlockPos pos = event.target.getBlockPos();
            if (pos == null) return;
            AxisAlignedBB selected = SweatsEnhancements.mc.theWorld.getBlockState(pos).getBlock()
                    .getSelectedBoundingBox(SweatsEnhancements.mc.theWorld, pos);
            if (selected == null) return;
            boolean whole = SweatsEnhancements.cfg.getBool("blockOutline.fill", false);
            AxisAlignedBB box = whole ? new AxisAlignedBB(pos, pos.add(1, 1, 1)) : selected;
            // Coplanar cube faces fight with block surfaces at grazing camera angles.
            if (whole) box = box.expand(0.004, 0.004, 0.004);
            float t = event.partialTicks;
            double x = event.player.lastTickPosX + (event.player.posX - event.player.lastTickPosX) * t;
            double y = event.player.lastTickPosY + (event.player.posY - event.player.lastTickPosY) * t;
            double z = event.player.lastTickPosZ + (event.player.posZ - event.player.lastTickPosZ) * t;
            int argb = colour(SweatsEnhancements.cfg.get("blockOutline.colour", DEFAULT_COLOUR));
            float r = ((argb >>> 16) & 255) / 255f;
            float g = ((argb >>> 8) & 255) / 255f;
            float b = (argb & 255) / 255f;
            float alpha = ((argb >>> 24) & 255) / 255f;
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LINE_BIT | GL11.GL_COLOR_BUFFER_BIT
                    | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_CURRENT_BIT | GL11.GL_POLYGON_BIT);
            try {
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glDepthMask(false);
                AxisAlignedBB relative = box.offset(-x, -y, -z);
                if (whole && alpha > 0f) {
                    // Render only front-facing faces, once, slightly above the block surface.
                    GL11.glEnable(GL11.GL_CULL_FACE);
                    GL11.glCullFace(GL11.GL_BACK);
                    GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
                    GL11.glPolygonOffset(-1f, -1f);
                    GL11.glColor4f(r, g, b, alpha);
                    fill(relative);
                    GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
                }
                GL11.glLineWidth(2.0f);
                GL11.glColor4f(r, g, b, 1f);
                RenderGlobal.drawSelectionBoundingBox(relative.expand(0.002, 0.002, 0.002));
            } finally {
                GL11.glPopAttrib();
            }
            event.setCanceled(true);
        } catch (Throwable err) { System.out.println("[SweatsEnhancements] block outline failed: " + err); }
    }
}
