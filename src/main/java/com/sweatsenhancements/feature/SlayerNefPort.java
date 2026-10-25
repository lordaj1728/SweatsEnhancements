package com.sweatsenhancements.feature;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import java.util.*;
import net.minecraft.block.BlockBeacon;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.*;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.*;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

/** Adapted from NEF's Slayer family: independently gated and bounded to observed server cues. */
public final class SlayerNefPort {
    private static final String[] BOSSES={"Revenant Horror","Atoned Horror","Sven Packmaster","Tarantula Broodfather","Voidgloom Seraph","Inferno Demonlord"};
    private static final String[] MINIS={"Deformed Revenant","Revenant Champion","Revenant Sycophant","Atoned Revenant","Atoned Champion","Pack Enforcer","Sven Alpha","Sven Follower","Sven Champion","Tarantula Vermin","Tarantula Beast","Mutant Tarantula","Voidling Devotee","Voidling Radical","Voidcrazed Maniac","Flare Demon","Kindleheart Demon","Burningsoul Demon"};
    private static final String[] ATTUNEMENTS={"ASHEN","SPIRIT","CRYSTAL","AURIC"};
    private final SlayerBasics basics;
    private Object world;
    private int ticks;
    private long maddoxExpires,lastPillarSound,beaconSeen;
    private boolean maddoxOpened;
    private String lastPillar="",bossHealth="";
    private BlockPos beacon;
    private final List<Target> targets=new ArrayList<Target>();
    private final Set<Integer> seenMini=new HashSet<Integer>();
    private static String diagnostic="waiting for Slayer Quest sidebar";
    public SlayerNefPort(SlayerBasics basics) {this.basics=basics;}
    private static boolean on(String id) {return SweatsEnhancements.cfg!=null && SweatsEnhancements.cfg.getBool(Cfg.key(id),false);}
    public static boolean anyEnabled() {for(int i=56;i<=70;i++) if(on("SL0"+i)) return true;return false;}
    public static String status() {return diagnostic;}
    private static String plain(String s) {String t=EnumChatFormatting.getTextWithoutFormattingCodes(s==null?"":s);return t==null?"":t.trim();}
    private static String find(String label,String[] names) {String s=plain(label).toLowerCase(Locale.ROOT);for(String n:names) if(s.contains(n.toLowerCase(Locale.ROOT))) return n;return "";}
    public static String bossName(String label) {return find(label,BOSSES);}
    public static String miniName(String label) {return find(label,MINIS);}
    public static String attunement(String label) {return find(label,ATTUNEMENTS);}
    public static int pillarSeconds(String name) {
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("^(\\d{1,2})s \\d{1,3} hits$").matcher(plain(name));
        return m.matches()?Integer.parseInt(m.group(1)):-1;
    }
    private static int colorFor(String name) {return "SPIRIT".equals(name)?0xFFFFFF:"CRYSTAL".equals(name)?0x55FFFF:"AURIC".equals(name)?0xFFFF55:0x666666;}
    private boolean available() {return SweatsEnhancements.mc!=null && SweatsEnhancements.mc.theWorld!=null && SweatsEnhancements.mc.thePlayer!=null && !DungeonRunHud.isInDungeon();}
    private void reset() {world=null;ticks=0;maddoxExpires=0;maddoxOpened=false;lastPillar="";bossHealth="";beacon=null;beaconSeen=0;targets.clear();seenMini.clear();diagnostic="waiting for Slayer Quest sidebar";}
    public void onChat(ClientChatReceivedEvent event) {
        if(event==null || event.message==null || event.type==2 || !available() || (!on("SL056") && !on("SL057"))) return;
        for(IChatComponent part:event.message.getSiblings()) {
            if(!plain(part.getUnformattedText()).contains("[OPEN MENU]")) continue;
            ClickEvent click=part.getChatStyle()==null?null:part.getChatStyle().getChatClickEvent();
            if(click==null || !"/openmaddox".equalsIgnoreCase(click.getValue())) continue;
            maddoxExpires=System.currentTimeMillis()+10000L;maddoxOpened=false;
            if(on("SL056")) {SweatsEnhancements.mc.thePlayer.sendChatMessage("/openmaddox");maddoxOpened=true;}
            else SweatsEnhancements.mc.thePlayer.addChatMessage(new ChatComponentText("§a[SweatsEnhancements] Open chat and left-click to open Maddox (10 seconds)."));
            break;
        }
    }
    public void onMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        if(!on("SL057") || on("SL056") || maddoxOpened || !available() || System.currentTimeMillis()>=maddoxExpires || !(event.gui instanceof GuiChat)) return;
        if(Mouse.getEventButton()==0 && Mouse.getEventButtonState()) {SweatsEnhancements.mc.thePlayer.sendChatMessage("/openmaddox");maddoxOpened=true;event.setCanceled(true);}
    }
    @SubscribeEvent public void onTick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END) return;
        if(!available()) {reset();return;}
        if(world!=SweatsEnhancements.mc.theWorld) {reset();world=SweatsEnhancements.mc.theWorld;}
        if(++ticks<10) return;ticks=0;
        if(!anyEnabled()) return;
        if(!basics.questActive()) {targets.clear();seenMini.clear();bossHealth="";beacon=null;lastPillar="";diagnostic="no observed Slayer Quest sidebar";return;}
        scanEntities();
        if((on("SL063") || on("SL064") || on("SL065")) && basics.bossActive() && basics.questName().toLowerCase(Locale.ROOT).contains("voidgloom"))
            beacon=com.sweatsenhancements.feature.nef.VoidgloomSeraph.current();
        else beacon=null;
        diagnostic="quest="+basics.questName()+" boss="+basics.bossActive()+" targets="+targets.size()+" health="+(!bossHealth.isEmpty())+" beacon="+(beacon!=null);
    }
    private void scanEntities() {
        targets.clear();bossHealth="";
        List<EntityLivingBase> living=new ArrayList<EntityLivingBase>();
        List<EntityArmorStand> labels=new ArrayList<EntityArmorStand>();
        List<EntityArmorStand> pillars=new ArrayList<EntityArmorStand>();
        for(Entity e:SweatsEnhancements.mc.theWorld.loadedEntityList) {
            if(e==null || e.isDead || e.getDistanceSqToEntity(SweatsEnhancements.mc.thePlayer)>48*48) continue;
            if(e instanceof EntityArmorStand) {
                EntityArmorStand stand=(EntityArmorStand)e;
                if(!bossName(stand.getCustomNameTag()).isEmpty() || !miniName(stand.getCustomNameTag()).isEmpty() || !attunement(stand.getCustomNameTag()).isEmpty()) labels.add(stand);
                if(on("SL066") && pillarSeconds(stand.getCustomNameTag())>=0) pillars.add(stand);
            } else if(e instanceof EntityLivingBase && !(e instanceof EntityPlayer)) living.add((EntityLivingBase)e);
        }
        Set<Integer> miniNow=new HashSet<Integer>();
        for(EntityArmorStand label:labels) {
            String name=label.getCustomNameTag(),boss=bossName(name),mini=miniName(name),att=attunement(name);
            if(!boss.isEmpty() && basics.bossActive() && bossHealth.isEmpty() && name.contains("❤")) bossHealth=name;
            EntityLivingBase actor=closest(label,living);
            if(actor==null) continue;
            if(basics.bossActive() && SweatsEnhancements.slayerMobs!=null && SweatsEnhancements.slayerMobs.slayerEntity.contains(actor)
                    && on("SL060")) add(actor,0xFF5555,false);
            if(!mini.isEmpty()) {
                miniNow.add(label.getEntityId());
                if(on("SL062") && SweatsEnhancements.slayerMobs!=null && SweatsEnhancements.slayerMobs.slayerMiniEntity.contains(actor))
                    add(actor,0x5C9AFF,false);
            }
            // NEF BlazeAttunements handles model outlines/fill via the model callback.
        }
        seenMini.clear();seenMini.addAll(miniNow);
    }
    private static EntityLivingBase closest(EntityArmorStand label,List<EntityLivingBase> living) {
        EntityLivingBase best=null;double distance=1.0;
        for(EntityLivingBase e:living) {double dx=e.posX-label.posX,dy=e.posY-label.posY,dz=e.posZ-label.posZ;
            if(Math.abs(dy)>3 || dx*dx+dz*dz>1) continue;
            double d=dx*dx+dz*dz;if(d<distance) {best=e;distance=d;}}
        return best;
    }
    private void add(EntityLivingBase e,int color,boolean filled) {for(Target t:targets) if(t.entity==e)return;targets.add(new Target(e,color,filled));}
    private static final class Target {final EntityLivingBase entity;final int color;final boolean filled;Target(EntityLivingBase e,int c,boolean f){entity=e;color=c;filled=f;}}
    public void renderHud(RenderGameOverlayEvent.Post event) {
        if(event.type!=RenderGameOverlayEvent.ElementType.ALL || !on("SL069") || !available() || !basics.bossActive() || bossHealth.isEmpty()) return;
        int width=SweatsEnhancements.mc.fontRendererObj.getStringWidth(bossHealth);
        SweatsEnhancements.mc.fontRendererObj.drawStringWithShadow(bossHealth,(event.resolution.getScaledWidth()-width)/2,25,0xFFFFFFFF);
    }
    @SubscribeEvent public void renderWorld(RenderWorldLastEvent event) {
        if(!available() || !basics.questActive() || (targets.isEmpty() && beacon==null)) return;
        float tick=event.partialTicks;Entity camera=SweatsEnhancements.mc.getRenderViewEntity();if(camera==null)return;
        double x=camera.lastTickPosX+(camera.posX-camera.lastTickPosX)*tick;
        double y=camera.lastTickPosY+(camera.posY-camera.lastTickPosY)*tick;
        double z=camera.lastTickPosZ+(camera.posZ-camera.lastTickPosZ)*tick;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT|GL11.GL_LINE_BIT|GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT|GL11.GL_CURRENT_BIT);
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glDepthMask(false);GL11.glLineWidth(3f);
            for(Target t:targets) {EntityLivingBase e=t.entity;if(e.isDead || e.worldObj!=SweatsEnhancements.mc.theWorld)continue;
                double ex=e.lastTickPosX+(e.posX-e.lastTickPosX)*tick,ey=e.lastTickPosY+(e.posY-e.lastTickPosY)*tick,ez=e.lastTickPosZ+(e.posZ-e.lastTickPosZ)*tick;
                AxisAlignedBB bb=e.getEntityBoundingBox().offset(ex-e.posX-x,ey-e.posY-y,ez-e.posZ-z).expand(.06,.06,.06);
                color(t.color,1f);RenderGlobal.drawSelectionBoundingBox(bb);
                if(t.filled){color(t.color,.16f);GL11.glBegin(GL11.GL_QUADS);
                    GL11.glVertex3d(bb.minX,bb.minY,bb.minZ);GL11.glVertex3d(bb.maxX,bb.minY,bb.minZ);
                    GL11.glVertex3d(bb.maxX,bb.maxY,bb.minZ);GL11.glVertex3d(bb.minX,bb.maxY,bb.minZ);GL11.glEnd();}
            }
            if(beacon!=null && basics.bossActive()) {
                if(on("SL063")){color(0xAA22FF,1f);RenderGlobal.drawSelectionBoundingBox(new AxisAlignedBB(beacon.getX()-x,beacon.getY()-y,beacon.getZ()-z,beacon.getX()+1-x,beacon.getY()+2-y,beacon.getZ()+1-z));}
                if(on("SL064")){color(0xAA22FF,1f);GL11.glBegin(GL11.GL_LINES);
                    GL11.glVertex3d(camera.posX-x,camera.posY+camera.getEyeHeight()-y,camera.posZ-z);
                    GL11.glVertex3d(beacon.getX()+.5-x,beacon.getY()+.5-y,beacon.getZ()+.5-z);GL11.glEnd();}
            }
        } finally {GL11.glDepthMask(true);GL11.glPopAttrib();}
    }
    private static void color(int rgb,float alpha){GL11.glColor4f(((rgb>>16)&255)/255f,((rgb>>8)&255)/255f,(rgb&255)/255f,alpha);}
    @SubscribeEvent public void onUnload(WorldEvent.Unload event){if(event.world!=null && event.world.isRemote) {reset();com.sweatsenhancements.feature.nef.VoidgloomSeraph.reset();}}
}
