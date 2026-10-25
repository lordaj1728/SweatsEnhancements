package com.sweatsenhancements.feature;

import com.sweatsenhancements.SweatsEnhancements;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import org.lwjgl.opengl.GL11;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Short, centered, size-adjustable HUD card for confirmed or explicitly estimated cues. */
public final class DungeonAlertCard {
    private static String message;
    private static long until;
    private static int color;

    public static void show(String title, int rgb) {
        if (title == null || SweatsEnhancements.mc == null || SweatsEnhancements.mc.thePlayer == null) return;
        message = title; color = rgb; until = System.currentTimeMillis() + 2200L;
    }
    @SubscribeEvent
    public void draw(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || SweatsEnhancements.mc == null
                || SweatsEnhancements.mc.currentScreen != null) return;
        render(event.resolution.getScaledWidth(),event.resolution.getScaledHeight());
    }
    @SubscribeEvent
    public void drawInGui(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (event != null && event.gui != null) render(event.gui.width,event.gui.height);
    }
    private void render(int width, int height) {
        if (message == null || SweatsEnhancements.mc == null || SweatsEnhancements.mc.fontRendererObj == null) return;
        if (System.currentTimeMillis() >= until) { message = null; return; }
        int percent = SweatsEnhancements.cfg == null ? 250 : SweatsEnhancements.cfg.getInt("dungeon.alertScalePercent", 250);
        double scale = Math.max(1d, Math.min(4d, percent / 100d));
        int textWidth = SweatsEnhancements.mc.fontRendererObj.getStringWidth(message);
        if (textWidth > 0) scale = Math.min(scale, Math.max(1d, (width - 32d) / textWidth));
        int cx = width / 2, cy = "Room Cleared!".equals(message) ? height / 2 : height / 3;
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        GlStateManager.pushMatrix();
        try {
            GlStateManager.disableDepth();
            GlStateManager.translate(cx - textWidth * scale / 2, cy - 4 * scale, 0);
            GlStateManager.scale(scale,scale,1);
            SweatsEnhancements.mc.fontRendererObj.drawStringWithShadow(message,0,0,color);
        } finally {
            if (depth) GlStateManager.enableDepth();
            else GlStateManager.disableDepth();
            GlStateManager.popMatrix();
        }
    }
    @SubscribeEvent
    public void unload(WorldEvent.Unload event) {
        if (event.world != null && event.world.isRemote) { message = null; until = 0; }
    }
}
