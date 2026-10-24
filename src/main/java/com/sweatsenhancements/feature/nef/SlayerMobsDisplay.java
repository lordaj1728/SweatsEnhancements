/* Adapted from NEF SlayerMobsDisplay.java (f80cddf6), LGPL-3.0.
 * The NEF RenderLiving.Pre armor-stand pairing and boss/miniboss name arrays are retained.
 * World-space box rendering is delegated to SweatsEnhancements until NEF's model-render callback is ported.
 */
package com.sweatsenhancements.feature.nef;
import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.feature.nef.exact.SlayerData;
import com.sweatsenhancements.feature.nef.exact.SlayerSettingsStore;
import com.sweatsenhancements.feature.nef.exact.SlayerRender;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import com.sweatsenhancements.feature.nef.exact.SlayerSettingsStore;
import com.sweatsenhancements.config.Cfg;
import com.sweatsenhancements.feature.DungeonRunHud;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SlayerMobsDisplay {
    public final Set<EntityLivingBase> slayerEntity=new HashSet<EntityLivingBase>();
    public final Set<EntityLivingBase> slayerMiniEntity=new HashSet<EntityLivingBase>();
    private long lastUpdateTime;
    private static final String[] BOSSES={"Revenant Horror","Atoned Horror","Sven Packmaster","Tarantula Broodfather","Voidgloom Seraph"};
    private static final String[][] MINIS={
        {"Deformed Revenant","Revenant Champion","Revenant Sycophant","Atoned Revenant","Atoned Champion"},
        {"Pack enforcer","Sven Alpha","Sven Follower","Sven Champion"},
        {"Tarantula Vermin","Tarantula Beast","Mutant Tarantula"},
        {"Voidling Devotee","Voidling Radical","Voidcrazed Maniac"},
        {"Flare Demon","Kindleheart Demon","Burningsoul Demon"}
    };
    @SubscribeEvent public void onRenderEntity(RenderLivingEvent.Pre event) {
        if (SweatsEnhancements.cfg==null || SweatsEnhancements.mc==null || SweatsEnhancements.mc.theWorld==null || SweatsEnhancements.mc.thePlayer==null || DungeonRunHud.isInDungeon() || !SlayerData.isSlayerActive()) return;
        if (!SlayerSettingsStore.get().bossSettings.slayerBosses && !SlayerSettingsStore.get().bossSettings.slayerBossesOutline
                && !SlayerSettingsStore.get().minibossSettings.slayerMinibosses) return;
        long currentTime=System.currentTimeMillis();
        if (currentTime-lastUpdateTime>20) {slayerEntity.clear();slayerMiniEntity.clear();lastUpdateTime=currentTime;}
        EntityLivingBase entity=event.entity;
        if (entity==null || entity.isInvisible() || entity instanceof EntityPlayer) return;
        List<Entity> armorStands=SweatsEnhancements.mc.theWorld.getEntitiesWithinAABB(EntityArmorStand.class,
                entity.getEntityBoundingBox().offset(0,2.0,0).expand(1.0,1.0,1.0));
        for (Entity armorStand:armorStands) {
            if (!(armorStand instanceof EntityArmorStand) || armorStand.getName()==null) continue;
            for (String name:BOSSES) if (armorStand.getName().contains(name)
                    && (SlayerSettingsStore.get().bossSettings.slayerBosses || SlayerSettingsStore.get().bossSettings.slayerBossesOutline)) {
                slayerEntity.add(entity);return;
            }
            for (String[] minibosses:MINIS) for (String name:minibosses)
                if (armorStand.getName().contains(name) && SlayerSettingsStore.get().minibossSettings.slayerMinibosses) {
                    slayerMiniEntity.add(entity);return;
                }
        }
    }
    @SubscribeEvent public void onRenderEntityModel(RenderEntityModelEvent event) {
        if (event==null || event.getEntity()==null || SweatsEnhancements.cfg==null) return;
        if (!SlayerSettingsStore.get().bossSettings.slayerBossesOutline || !slayerEntity.contains(event.getEntity())) return;
        OutlineUtils.outlineEntity(event,4.0f,new java.awt.Color(0xFF5555),true);
    }
    @SubscribeEvent public void renderWorld(RenderWorldLastEvent event) {
        if(SweatsEnhancements.mc==null || SweatsEnhancements.mc.theWorld==null || SweatsEnhancements.mc.thePlayer==null || DungeonRunHud.isInDungeon() || !SlayerData.isSlayerActive())return;
        boolean boss=SlayerSettingsStore.get().bossSettings.slayerBosses && !SlayerSettingsStore.get().bossSettings.slayerBossesOutline;
        boolean mini=SlayerSettingsStore.get().minibossSettings.slayerMinibosses;
        if(!boss&&!mini)return;
        java.awt.Color bossColor=SlayerRender.color(SlayerSettingsStore.get().bossSettings.slayerBossColor,new java.awt.Color(92,154,255));
        java.awt.Color miniColor=SlayerRender.color(SlayerSettingsStore.get().minibossSettings.slayerColor,new java.awt.Color(92,154,255));
        SlayerRender.begin();try{
            if(boss)for(EntityLivingBase e:slayerEntity)SlayerRender.entity(e,event.partialTicks,bossColor);
            if(mini)for(EntityLivingBase e:slayerMiniEntity)SlayerRender.entity(e,event.partialTicks,miniColor);
        }finally{SlayerRender.end();}
    }
    @SubscribeEvent public void unload(WorldEvent.Unload e) {if(e.world!=null && e.world.isRemote) {slayerEntity.clear();slayerMiniEntity.clear();}}
}
