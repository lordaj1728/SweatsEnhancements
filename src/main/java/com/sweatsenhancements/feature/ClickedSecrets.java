package com.sweatsenhancements.feature;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.block.BlockLever;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/** Label every locally clicked chest or lever in a Catacombs run, regardless of secret count. */
public final class ClickedSecrets {
    private final Set<BlockPos> clicked = new LinkedHashSet<BlockPos>();

    public static boolean eligible(Block block) {
        return block instanceof BlockChest || block instanceof BlockEnderChest || block instanceof BlockLever;
    }

    private boolean inDungeon() {
        return SweatsEnhancements.cfg != null && DungeonRunHud.isInDungeon()
                && SweatsEnhancements.mc != null && SweatsEnhancements.mc.theWorld != null;
    }
    private boolean enabled() { return inDungeon() && SweatsEnhancements.cfg.getBool(Cfg.key("DU004"), false); }

    @SubscribeEvent
    public void onInteract(PlayerInteractEvent event) {
        try {
            if (!inDungeon() || (!enabled() && !SweatsEnhancements.cfg.getBool(Cfg.key("DU019"), false))
                    || event == null || event.entityPlayer != SweatsEnhancements.mc.thePlayer || event.pos == null
                    || event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK
                    || !eligible(SweatsEnhancements.mc.theWorld.getBlockState(event.pos).getBlock())) return;
            if (enabled()) {
                if (clicked.size() >= 256) clicked.clear();
                clicked.add(event.pos);
            }
            if (SweatsEnhancements.cfg.getBool(Cfg.key("DU019"), false))
                SweatsEnhancements.mc.thePlayer.playSound("random.orb", .55f, 1.35f);
        } catch (Throwable t) { System.out.println("[SweatsEnhancements] clicked chest/lever input failed: " + t); }
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        if (!enabled() || SweatsEnhancements.mc.thePlayer == null || SweatsEnhancements.mc.fontRendererObj == null
                || clicked.isEmpty()) return;
        try {
            Entity camera = SweatsEnhancements.mc.getRenderViewEntity();
            if (camera == null) return;
            float t = event.partialTicks;
            double x = camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * t;
            double y = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * t;
            double z = camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * t;
            for (BlockPos pos : clicked) {
                if (pos.distanceSq(x, y, z) > 4096) continue;
                Block block = SweatsEnhancements.mc.theWorld.getBlockState(pos).getBlock();
                if (!eligible(block)) continue; // block was removed/replaced after the click
                GL11.glPushMatrix();
                GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
                try {
                    double height = block instanceof BlockLever ? .65 : 1.05;
                    GL11.glTranslated(pos.getX() + .5 - x, pos.getY() + height - y, pos.getZ() + .5 - z);
                    GL11.glRotatef(-SweatsEnhancements.mc.getRenderManager().playerViewY, 0, 1, 0);
                    GL11.glRotatef(SweatsEnhancements.mc.getRenderManager().playerViewX, 1, 0, 0);
                    GL11.glScalef(-0.025f, -0.025f, 0.025f);
                    GL11.glDisable(GL11.GL_LIGHTING);
                    GL11.glEnable(GL11.GL_BLEND);
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                    String label = "Clicked!";
                    SweatsEnhancements.mc.fontRendererObj.drawStringWithShadow(label,
                            -SweatsEnhancements.mc.fontRendererObj.getStringWidth(label) / 2, 0, 0x55FF66);
                } finally {
                    GL11.glPopAttrib();
                    GL11.glPopMatrix();
                }
            }
        } catch (Throwable t) { System.out.println("[SweatsEnhancements] clicked chest/lever label failed: " + t); }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world != null && event.world.isRemote) clicked.clear();
    }
}
