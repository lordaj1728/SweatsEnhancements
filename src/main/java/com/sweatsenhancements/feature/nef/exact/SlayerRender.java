/* NEF Slayer hitbox/tracer rendering integration; NEF source f80cddf6 LGPL-3.0.
 * Camera interpolation and GL restoration adapted for SweatsEnhancements's Forge 1.8.9 pipeline. */
package com.sweatsenhancements.feature.nef.exact;
import com.sweatsenhancements.SweatsEnhancements;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import org.lwjgl.opengl.GL11;
import java.awt.Color;

public final class SlayerRender {
    private SlayerRender(){}
    public static Color color(String config,Color fallback){
        try {String[] t=config.split(":");int n=t.length;
            return new Color(Integer.parseInt(t[n-3]),Integer.parseInt(t[n-2]),Integer.parseInt(t[n-1]),Integer.parseInt(t[n-4]));
        }catch(RuntimeException e){return fallback;}
    }
    public static void begin(){GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glDepthMask(false);GL11.glLineWidth(2.5f);}
    public static void end(){GL11.glPopMatrix();GL11.glPopAttrib();}
    private static void set(Color c){GL11.glColor4f(c.getRed()/255f,c.getGreen()/255f,c.getBlue()/255f,c.getAlpha()/255f);}
    private static double camX(float tick){Entity e=SweatsEnhancements.mc.getRenderViewEntity();return e.lastTickPosX+(e.posX-e.lastTickPosX)*tick;}
    private static double camY(float tick){Entity e=SweatsEnhancements.mc.getRenderViewEntity();return e.lastTickPosY+(e.posY-e.lastTickPosY)*tick;}
    private static double camZ(float tick){Entity e=SweatsEnhancements.mc.getRenderViewEntity();return e.lastTickPosZ+(e.posZ-e.lastTickPosZ)*tick;}
    public static void entity(Entity e,float tick,Color c){if(e==null||e.isDead||e.worldObj!=SweatsEnhancements.mc.theWorld)return;
        double x=e.lastTickPosX+(e.posX-e.lastTickPosX)*tick;
        double y=e.lastTickPosY+(e.posY-e.lastTickPosY)*tick;
        double z=e.lastTickPosZ+(e.posZ-e.lastTickPosZ)*tick;
        AxisAlignedBB bb=e.getEntityBoundingBox().offset(x-e.posX-camX(tick),y-e.posY-camY(tick),z-e.posZ-camZ(tick)).expand(.05,.05,.05);
        set(c);RenderGlobal.drawSelectionBoundingBox(bb);
    }
    public static void beacon(BlockPos pos,float tick,Color color,boolean box,boolean tracer){if(pos==null)return;
        double x=camX(tick),y=camY(tick),z=camZ(tick);set(color);
        if(box)RenderGlobal.drawSelectionBoundingBox(new AxisAlignedBB(pos.getX()-x,pos.getY()-y,pos.getZ()-z,pos.getX()+1-x,pos.getY()+2-y,pos.getZ()+1-z));
        if(tracer){Entity camera=SweatsEnhancements.mc.getRenderViewEntity();GL11.glBegin(GL11.GL_LINES);
            GL11.glVertex3d(camera.posX-x,camera.posY+camera.getEyeHeight()-y,camera.posZ-z);
            GL11.glVertex3d(pos.getX()+.5-x,pos.getY()+.5-y,pos.getZ()+.5-z);GL11.glEnd();}
    }
}
