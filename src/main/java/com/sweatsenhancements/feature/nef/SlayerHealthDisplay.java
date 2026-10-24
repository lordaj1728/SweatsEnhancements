/* Adapted from NEF SlayerHealthDisplay.java (f80cddf6), LGPL-3.0.
 * Nearest named boss, Inferno attunement and minion-phase HP search retained.
 * SweatsEnhancements uses the on-screen position at upper centre rather than NEF's position editor.
 */
package com.sweatsenhancements.feature.nef;
import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.feature.nef.exact.SlayerData;
import com.sweatsenhancements.feature.nef.exact.Slayer;
import com.sweatsenhancements.feature.nef.exact.SlayerSettingsStore;
import com.sweatsenhancements.config.Cfg;
import com.sweatsenhancements.feature.DungeonRunHud;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public final class SlayerHealthDisplay {
    private String displayText="";
    private boolean isDemonlordFight;
    private final List<MinionInfo> minionInfos=new ArrayList<MinionInfo>();
    private int ticks;
    private static final String[] SLAYER_BOSSES={"Revenant Horror","Atoned Horror","Sven Packmaster","Tarantula Broodfather","Voidgloom Seraph","Inferno Demonlord"};
    private static class MinionInfo {
        final String attunement,hp;final Boolean attackable;
        MinionInfo(String attunement,String hp,Boolean attackable){this.attunement=attunement;this.hp=hp;this.attackable=attackable;}
    }
    @SubscribeEvent public void onClientTick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END || SweatsEnhancements.mc==null || SweatsEnhancements.mc.theWorld==null || SweatsEnhancements.mc.thePlayer==null)return;
        if(++ticks%10!=0)return;
        if(SweatsEnhancements.cfg==null || !SlayerSettingsStore.get().bossSettings.slayerBossHP || DungeonRunHud.isInDungeon()
                || !SlayerData.isBossActive()) {
            displayText="";isDemonlordFight=false;minionInfos.clear();return;
        }
        List<Entity> entities=SweatsEnhancements.mc.theWorld.getLoadedEntityList();
        Entity closestBoss=null;double closestDistance=Double.MAX_VALUE;String bossNameFound="";
        for(Entity entity:entities) {
            if(!(entity instanceof EntityArmorStand) || !entity.hasCustomName())continue;
            String entityName=entity.getCustomNameTag();
            for(String bossName:SLAYER_BOSSES) if(entityName.contains(bossName)) {
                double distance=SweatsEnhancements.mc.thePlayer.getDistanceToEntity(entity);
                if(distance<closestDistance){closestDistance=distance;closestBoss=entity;bossNameFound=entityName;}
                break;
            }
        }
        if(closestBoss!=null) {
            if(bossNameFound.contains("Inferno Demonlord")) {
                isDemonlordFight=true;minionInfos.clear();
                String attunement=findAttunementNear(entities,closestBoss,4.0);
                displayText=bossNameFound+(attunement.isEmpty()?"":" "+colorCode(attunement)+"["+attunement+"]§r");
            } else {isDemonlordFight=false;minionInfos.clear();displayText=bossNameFound;}
        } else if(isDemonlordFight) {displayText="";updateMinionInfos(entities);}
        else {displayText="";minionInfos.clear();}
    }
    private String findAttunementNear(List<Entity> entities,Entity reference,double maxDist) {
        double maxDistSq=maxDist*maxDist;
        for(Entity e:entities) {
            if(!(e instanceof EntityArmorStand) || e==reference || !e.hasCustomName())continue;
            if(reference.getDistanceSqToEntity(e)>maxDistSq)continue;
            String name=((EntityArmorStand)e).getDisplayName().getUnformattedText();
            Matcher m=BlazeAttunements.COLOR_PATTERN.matcher(name);
            if(m.find())return m.group().toUpperCase();
        }
        return "";
    }
    private void updateMinionInfos(List<Entity> entities) {
        minionInfos.clear();
        for(Entity e:entities) {
            boolean isWither=e instanceof EntitySkeleton && ((EntitySkeleton)e).getSkeletonType()==1;
            boolean isPigZombie=e instanceof EntityPigZombie;
            if((!isWither && !isPigZombie) || SweatsEnhancements.mc.thePlayer.getDistanceSqToEntity(e)>50)continue;
            EntityLivingBase minion=(EntityLivingBase)e;
            String attunement=BlazeAttunements.blazeEntity.get(minion);
            boolean attackable=attunement!=null;
            String hp="";
            for(Entity stand:entities) {
                if(!(stand instanceof EntityArmorStand))continue;
                if(stand.getDistanceSqToEntity(minion)>25.0)continue;
                String name=stand.getDisplayName().getUnformattedText();
                if(name.contains("❤")){hp=name;break;}
            }
            if(attunement==null && hp.isEmpty())continue;
            minionInfos.add(new MinionInfo(attunement!=null?attunement:"",hp,attackable));
        }
    }
    private String colorCode(String attunement) {
        if("SPIRIT".equals(attunement))return "§f";
        if("ASHEN".equals(attunement))return "§8";
        if("CRYSTAL".equals(attunement))return "§b";
        if("AURIC".equals(attunement))return "§e";
        return "§r";
    }
    @SubscribeEvent public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if(event.type!=RenderGameOverlayEvent.ElementType.ALL || SweatsEnhancements.cfg==null || !SlayerSettingsStore.get().bossSettings.slayerBossHP
                || SweatsEnhancements.mc==null || SweatsEnhancements.mc.fontRendererObj==null)return;
        if(!displayText.isEmpty())draw(event,displayText,25);
        else for(int i=0;i<minionInfos.size();i++) {
            MinionInfo info=minionInfos.get(i);
            draw(event,(info.attunement.isEmpty()?"":"["+info.attunement+"] ")+(info.attackable?"ATTACK":"IMMUNE")+" "+info.hp,25+i*11);
        }
    }
    private void draw(RenderGameOverlayEvent.Post event,String text,int y) {
        int width=SweatsEnhancements.mc.fontRendererObj.getStringWidth(text);
        com.sweatsenhancements.feature.nef.exact.Position position=SlayerSettingsStore.get().bossSettings.slayerBossHPPos;
        if(position!=null)SweatsEnhancements.mc.fontRendererObj.drawStringWithShadow(text,position.getAbsX(event.resolution,width),position.getAbsY(event.resolution,11)+(y-25),0xFF5555);
    }
    @SubscribeEvent public void unload(WorldEvent.Unload event) {
        if(event.world!=null && event.world.isRemote) {displayText="";isDemonlordFight=false;minionInfos.clear();ticks=0;}
    }
}
