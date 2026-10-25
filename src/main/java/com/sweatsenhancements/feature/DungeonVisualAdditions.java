package com.sweatsenhancements.feature;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.entity.Entity;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/** Independent, bounded, visual-only dungeon cues. No targeting or automatic interaction. */
public final class DungeonVisualAdditions {
    private final List<Entity> bosses = new ArrayList<Entity>();
    private final List<Entity> minions = new ArrayList<Entity>();
    private final List<EntityItem> bows = new ArrayList<EntityItem>();
    private Object world;
    private final List<EntityLivingBase> living = new ArrayList<EntityLivingBase>();
    private final List<EntityArmorStand> bossLabels = new ArrayList<EntityArmorStand>();
    private final List<EntityArmorStand> minionLabels = new ArrayList<EntityArmorStand>();
    private long lastScan;

    private static String clean(String s) {
        String t=EnumChatFormatting.getTextWithoutFormattingCodes(s == null ? "" : s);
        return t == null ? "" : t.trim().toLowerCase(Locale.ROOT);
    }
    private static String strippedBossLabel(String name) {
        // Server boss labels can start with a star before the actual name.
        return clean(name).replaceFirst("^[★✮✰☆✯\\s]+", "");
    }
    public static int bossFloor(String name) {
        String s=strippedBossLabel(name);
        if (s.equals("bonzo") || s.startsWith("bonzo ") || s.startsWith("bonzo❤")) return 1;
        if (s.equals("scarf") || s.startsWith("scarf ") || s.startsWith("scarf❤")) return 2;
        if (s.equals("the professor") || s.startsWith("the professor ") || s.equals("professor") || s.startsWith("professor ")) return 3;
        return 0;
    }
    /** Server NPC player names can be private-use glyphs rather than readable names. */
    public static boolean syntheticActorName(String name) {
        String value=clean(name);
        if (value.isEmpty()) return false;
        for (int i=0;i<value.length();) {
            int cp=value.codePointAt(i);
            if (Character.getType(cp)!=Character.PRIVATE_USE) return false;
            i+=Character.charCount(cp);
        }
        return true;
    }
    /** Bonzo's remote Wither supplies boss-bar state, not a physical target box. */
    public static boolean remoteBossbarType(Class<?> type) {
        return type!=null && EntityWither.class.isAssignableFrom(type);
    }
    public static boolean scarfMinion(String name) {
        String s=clean(name);
        // The F2 server puts symbols and class icons before the minion name and
        // displays abbreviated health (e.g. 566.9k) after it. Match that observed
        // armor-stand label, not a guessed bare "Undead Mage" entity name.
        if (!s.contains("❤")) return false;
        for (String role:new String[] {"priest", "mage", "archer", "warrior"})
            if (s.matches(".*(?:^|[^a-z])undead " + role + "(?:[^a-z]|$).*")) return true;
        return false;
    }
    public static boolean spiritBow(String name) { return clean(name).equals("spirit bow"); }
    public static boolean bossMessage(String text) {
        String s=clean(text);
        return s.matches("\\[boss\\] (?:the watcher|bonzo|scarf|the professor|thorn|livid|sadan|necron|storm|goldor|maxor): .+");
    }
    private boolean on(String id) { return SweatsEnhancements.cfg != null && SweatsEnhancements.cfg.getBool(Cfg.key(id),false); }
    private boolean active() {
        return DungeonRunHud.isInDungeon() && SweatsEnhancements.mc != null && SweatsEnhancements.mc.theWorld != null
                && SweatsEnhancements.mc.thePlayer != null;
    }
    private void clear() {
        bosses.clear(); minions.clear(); bows.clear(); living.clear();
        bossLabels.clear(); minionLabels.clear(); lastScan=0;
    }
    /** Labels are not physical mobs. Reject camera-following stands and unrelated bystanders. */
    public static double pairingDistanceSq(double dx,double dy,double dz) {
        if (Math.abs(dy)>3 || dx*dx+dz*dz>.65) return Double.POSITIVE_INFINITY;
        return dx*dx+dz*dz+Math.pow(Math.max(0,Math.abs(dy)-2.5),2);
    }
    public static boolean canPairActor(boolean player, String actorName, boolean boss, int floor) {
        return !player || syntheticActorName(actorName) || (boss && bossFloor(actorName)==floor);
    }
    private EntityLivingBase paired(EntityArmorStand label, int floor, boolean boss) {
        EntityLivingBase best=null; double distance=1.0;
        for (EntityLivingBase mob:living) {
            // Player-shaped boss NPCs can be used only if their own name also identifies
            // the boss. Do not attach the label to a teammate walking under it.
            if (!canPairActor(mob instanceof net.minecraft.entity.player.EntityPlayer,
                    mob.getName(), boss, floor)) continue;
            double d=pairingDistanceSq(mob.posX-label.posX,mob.posY-label.posY,mob.posZ-label.posZ);
            if (d<distance) { best=mob; distance=d; }
        }
        return best;
    }
    /** Calculate the target's interpolated box in view space; never use mouse-over coordinates. */
    public static AxisAlignedBB relativeBox(AxisAlignedBB bounds,
            double entityX,double entityY,double entityZ,double interpolatedX,double interpolatedY,double interpolatedZ,
            double cameraX,double cameraY,double cameraZ) {
        return bounds.offset(interpolatedX-entityX-cameraX,interpolatedY-entityY-cameraY,interpolatedZ-entityZ-cameraZ);
    }
    private void scan() {
        if (!active()) { clear(); return; }
        if (world != SweatsEnhancements.mc.theWorld) { clear(); world=SweatsEnhancements.mc.theWorld; }
        long now=System.currentTimeMillis();
        if (now-lastScan<250) return;
        lastScan=now;
        bosses.clear(); minions.clear(); bows.clear(); living.clear(); bossLabels.clear(); minionLabels.clear();
        int floor=DungeonScoreHud.floor()%10;
        // Snapshot only nearby loaded entities. If the server does not identify an entity,
        // leave it unhighlighted rather than guessing from texture, location or costume.
        for (Entity e:SweatsEnhancements.mc.theWorld.loadedEntityList) {
            if (e==null || e.isDead || e.getDistanceSqToEntity(SweatsEnhancements.mc.thePlayer)>4096) continue;
            if (e instanceof EntityItem && on("DU024")) {
                EntityItem item=(EntityItem)e;
                if (item.getEntityItem()!=null && spiritBow(item.getEntityItem().getDisplayName())) bows.add(item);
            }
            // The F1 dump proved a named Wither 15 blocks away is a moving boss-bar
            // carrier. Never pick it as Bonzo (or as the mob below a name stand).
            if (remoteBossbarType(e.getClass())) continue;
            if (!(e instanceof EntityLivingBase) || e==SweatsEnhancements.mc.thePlayer) continue;
            String name=e.getName();
            if (e instanceof EntityArmorStand) {
                if (on("DU022") && floor>0 && bossFloor(name)==floor) bossLabels.add((EntityArmorStand)e);
                if (on("DU023") && floor==2 && scarfMinion(name)) minionLabels.add((EntityArmorStand)e);
                continue; // Never box a floating label at its own position.
            }
            living.add((EntityLivingBase)e);
            if (on("DU022") && floor>0 && bossFloor(name)==floor) bosses.add(e);
            if (on("DU023") && floor==2 && scarfMinion(name)) minions.add(e);
        }
        for (EntityArmorStand label:bossLabels) {
            EntityLivingBase mob=paired(label,floor,true);
            if (mob!=null && !bosses.contains(mob)) bosses.add(mob);
        }
        for (EntityArmorStand label:minionLabels) {
            EntityLivingBase mob=paired(label,floor,false);
            if (mob!=null && !minions.contains(mob)) minions.add(mob);
        }
    }
    /** Adapted from NEF RenderUtils hitbox edges (LGPL-3.0); explicit GL matrix isolation. */
    private static void outline(Entity e,float partial,double x,double y,double z,float r,float g,float b) {
        AxisAlignedBB raw=e.getEntityBoundingBox();
        if (raw==null || e instanceof EntityArmorStand) return;
        double ex=e.lastTickPosX+(e.posX-e.lastTickPosX)*partial;
        double ey=e.lastTickPosY+(e.posY-e.lastTickPosY)*partial;
        double ez=e.lastTickPosZ+(e.posZ-e.lastTickPosZ)*partial;
        AxisAlignedBB bb=relativeBox(raw,e.posX,e.posY,e.posZ,ex,ey,ez,x,y,z).expand(.07,.07,.07);
        double[][] v={{bb.minX,bb.minY,bb.minZ},{bb.maxX,bb.minY,bb.minZ},
                {bb.maxX,bb.maxY,bb.minZ},{bb.minX,bb.maxY,bb.minZ},
                {bb.minX,bb.minY,bb.maxZ},{bb.maxX,bb.minY,bb.maxZ},
                {bb.maxX,bb.maxY,bb.maxZ},{bb.minX,bb.maxY,bb.maxZ}};
        int[][] edges={{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},
                {0,4},{1,5},{2,6},{3,7}};
        // NEF RenderUtils-style buffered coloured edges, not immediate-mode GL11 lines.
        // The latter can be rendered in an unexpected camera transform by other mods.
        Tessellator tessellator=Tessellator.getInstance();
        WorldRenderer buffer=tessellator.getWorldRenderer();
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        int red=Math.round(r*255f), green=Math.round(g*255f), blue=Math.round(b*255f);
        for (int[] edge:edges) {
            double[] a=v[edge[0]],c=v[edge[1]];
            buffer.pos(a[0],a[1],a[2]).color(red,green,blue,255).endVertex();
            buffer.pos(c[0],c[1],c[2]).color(red,green,blue,255).endVertex();
        }
        tessellator.draw();
    }
    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        if (!active() || (!on("DU022") && !on("DU023") && !on("DU024"))) return;
        try {
            scan();
            Entity camera=SweatsEnhancements.mc.getRenderViewEntity(); if (camera==null) return;
            float t=event.partialTicks;
            double x=camera.lastTickPosX+(camera.posX-camera.lastTickPosX)*t;
            double y=camera.lastTickPosY+(camera.posY-camera.lastTickPosY)*t;
            double z=camera.lastTickPosZ+(camera.posZ-camera.lastTickPosZ)*t;
            int previousMode=GL11.glGetInteger(GL11.GL_MATRIX_MODE);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT|GL11.GL_CURRENT_BIT|GL11.GL_LINE_BIT|GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
            GL11.glPushMatrix();
            try {
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glDepthMask(false);
                GL11.glLineWidth(3f);
                for (Entity e:bosses) if (!e.isDead && e.worldObj==SweatsEnhancements.mc.theWorld) outline(e,t,x,y,z,1f,.3f,.2f);
                for (Entity e:minions) if (!e.isDead && e.worldObj==SweatsEnhancements.mc.theWorld) outline(e,t,x,y,z,.95f,.7f,.3f);
                for (EntityItem e:bows) if (!e.isDead && e.worldObj==SweatsEnhancements.mc.theWorld) outline(e,t,x,y,z,.35f,1f,.9f);
            } finally { GL11.glPopMatrix(); GL11.glPopAttrib(); GL11.glMatrixMode(previousMode); }
        } catch (Throwable t) { System.out.println("[SweatsEnhancements] dungeon visual additions failed: "+t); }
    }
    /** Include the actual selected entity in /se dump; logs alone cannot identify it. */
    public String targetStatus() {
        StringBuilder result=new StringBuilder();
        result.append("enabled=").append(on("DU022"));
        result.append(" floor=").append(DungeonScoreHud.floor());
        result.append(" selected=").append(bosses.size());
        result.append(" labels=").append(bossLabels.size());
        result.append(" minions=").append(minions.size());
        result.append(" minionLabels=").append(minionLabels.size());
        result.append(" scanAgeMs=").append(lastScan==0?"never":System.currentTimeMillis()-lastScan);
        int shown=0;
        for (Entity e:bosses) {
            if (shown++>=3) break;
            result.append(" [id=").append(e.getEntityId()).append(" ").append(e.getClass().getSimpleName());
            result.append(" name=").append(e.getName());
            result.append(" pos=").append(String.format(Locale.ROOT,"%.1f/%.1f/%.1f",e.posX,e.posY,e.posZ)).append("]");
        }
        return result.toString();
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!active() || !on("DU025") || event==null || event.isCanceled() || event.type!=0 || event.message==null) return;
        if (bossMessage(event.message.getUnformattedText())) event.setCanceled(true);
    }
    @SubscribeEvent
    public void onUnload(WorldEvent.Unload event) { if (event.world!=null && event.world.isRemote) { clear(); world=null; } }
}
