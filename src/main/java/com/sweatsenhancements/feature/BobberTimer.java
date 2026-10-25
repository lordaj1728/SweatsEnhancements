package com.sweatsenhancements.feature;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityFishHook;

import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import org.lwjgl.opengl.GL11;

/**
 * Fishing bobber timer, drawn in the world just above your bobber.
 *
 * This used to be a HUD element in the corner, which is the wrong place for it: the number is only
 * useful while you are watching the bobber, and a corner readout makes you look away from it. It
 * is anchored to the bobber now and follows it.
 *
 * World-space text needs the view matrix undone, which is what the translate and two rotations do -
 * they put the origin at the bobber, facing the camera. The scale is negative on two axes because
 * the font renderer draws downward from its origin. Everything is restored afterwards whether or
 * not the draw succeeded, because leaked GL state wrecks the rest of the frame.
 */
public class BobberTimer {

    private static final int NORMAL_COLOUR = 0xCCCCCC;
    private static final int READY_COLOUR = 0x7FD47F;
    private static final int TOO_LONG_COLOUR = 0xFF8080;

    /** seconds after which the timer turns green - long enough that the bite is overdue */
    private static final int READY_AFTER = 20;
    /** seconds after which it turns red - something is wrong, not just slow */
    private static final int STUCK_AFTER = 90;

    public static boolean enabled(Cfg cfg) {
        return cfg != null && cfg.getBool(Cfg.key("FI030"), true);
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!enabled(SweatsEnhancements.cfg)) return;
        Minecraft mc = Minecraft.getMinecraft();
        EntityFishHook hook = ownBobber(mc);
        if (hook == null) return;

        FontRenderer font = mc.fontRendererObj;
        Entity view = mc.getRenderViewEntity();
        if (font == null || view == null) return;

        int seconds = hook.ticksExisted / 20;
        int colour = seconds >= STUCK_AFTER ? TOO_LONG_COLOUR
                : (seconds >= READY_AFTER ? READY_COLOUR : NORMAL_COLOUR);
        String text = seconds + "s";

        GL11.glPushMatrix();
        try {
            // the bobber moves between ticks, so interpolate it the same way the camera was
            float partial = event.partialTicks;
            double x = hook.prevPosX + (hook.posX - hook.prevPosX) * partial;
            double y = hook.prevPosY + (hook.posY - hook.prevPosY) * partial;
            double z = hook.prevPosZ + (hook.posZ - hook.prevPosZ) * partial;
            double viewX = view.prevPosX + (view.posX - view.prevPosX) * partial;
            double viewY = view.prevPosY + (view.posY - view.prevPosY) * partial;
            double viewZ = view.prevPosZ + (view.posZ - view.prevPosZ) * partial;

            GL11.glTranslated(x - viewX, y - viewY + 0.85, z - viewZ);
            GL11.glRotatef(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
            GL11.glScalef(-0.025F, -0.025F, 0.025F);

            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            font.drawStringWithShadow(text, -font.getStringWidth(text) / 2, 0, colour);
        } catch (Throwable t) {
            // a render hook that throws would spam the log every frame
        } finally {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glPopMatrix();
        }
    }

    /** The player's own bobber, or null when no line is cast. */
    public static EntityFishHook ownBobber(Minecraft mc) {
        try {
            if (mc == null || mc.theWorld == null || mc.thePlayer == null) return null;
            EntityFishHook best = null;
            double bestDistance = Double.MAX_VALUE;
            for (Entity entity : mc.theWorld.loadedEntityList) {
                if (!(entity instanceof EntityFishHook)) continue;
                EntityFishHook hook = (EntityFishHook) entity;
                if (hook.angler != mc.thePlayer) continue;
                double distance = hook.getDistanceSqToEntity(mc.thePlayer);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = hook;
                }
            }
            return best;
        } catch (Throwable t) {
            return null;
        }
    }
}
