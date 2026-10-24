/* Adapted from NEF BlazeAttunements.java (f80cddf6), LGPL-3.0.
 * Upstream armor-stand pairing, attunement whitelist and entity-type guards retained.
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
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.event.world.WorldEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BlazeAttunements {
    public static final Map<EntityLivingBase,String> blazeEntity=new HashMap<EntityLivingBase,String>();
    public static final Pattern COLOR_PATTERN=Pattern.compile("ASHEN|SPIRIT|CRYSTAL|AURIC");
    private long lastUpdateTime;
    @SubscribeEvent public void onRenderEntity(RenderLivingEvent.Pre event) {
        if (SweatsEnhancements.cfg == null || (!SlayerSettingsStore.get().blazeAttunementSettings.slayerBlazeAttunementOutline
                && !SlayerSettingsStore.get().blazeAttunementSettings.slayerBlazeAttunementFill)) return;
        long currentTime=System.currentTimeMillis();
        if (currentTime-lastUpdateTime>20) {blazeEntity.clear();lastUpdateTime=currentTime;}
        if (SweatsEnhancements.mc==null || SweatsEnhancements.mc.theWorld==null || SweatsEnhancements.mc.thePlayer==null || DungeonRunHud.isInDungeon()
                || !SlayerData.isSlayerActive()) return;
        EntityLivingBase entity=event.entity;
        if (entity==null || entity.isInvisible() || entity==SweatsEnhancements.mc.thePlayer) return;
        List<Entity> armorStands=SweatsEnhancements.mc.theWorld.getEntitiesWithinAABB(EntityArmorStand.class,
                entity.getEntityBoundingBox().offset(0,2.0,0).expand(1.0,1.0,1.0));
        for (Entity armorStand:armorStands) if (armorStand instanceof EntityArmorStand) {
            String displayName=armorStand.getDisplayName().getUnformattedText();
            Matcher matcher=COLOR_PATTERN.matcher(displayName);
            if (matcher.find()) {
                String attunement=matcher.group().toUpperCase();
                boolean isValidEntity=entity instanceof EntityBlaze || entity instanceof EntityPigZombie ||
                        (entity instanceof EntitySkeleton && ((EntitySkeleton)entity).getSkeletonType()==1);
                if (isValidEntity) {
                    boolean allowed=false;
                    if (entity instanceof EntitySkeleton && ((EntitySkeleton)entity).getSkeletonType()==1)
                        allowed=attunement.equals("SPIRIT") || attunement.equals("CRYSTAL");
                    else if (entity instanceof EntityPigZombie)
                        allowed=attunement.equals("ASHEN") || attunement.equals("AURIC");
                    else if (entity instanceof EntityBlaze) allowed=true;
                    if (allowed) {blazeEntity.put(entity,attunement);return;}
                }
            }
        }
    }
    @SubscribeEvent public void onRenderEntityModel(RenderEntityModelEvent event) {
        if (event==null || event.getEntity()==null || SweatsEnhancements.cfg==null) return;
        String attunement=blazeEntity.get(event.getEntity());
        if (attunement==null)return;
        java.awt.Color color="SPIRIT".equals(attunement)?java.awt.Color.WHITE:
                "CRYSTAL".equals(attunement)?java.awt.Color.CYAN:
                "AURIC".equals(attunement)?java.awt.Color.YELLOW:java.awt.Color.DARK_GRAY;
        if (SlayerSettingsStore.get().blazeAttunementSettings.slayerBlazeAttunementOutline) OutlineUtils.outlineEntity(event,6f,color,true);
        if (SlayerSettingsStore.get().blazeAttunementSettings.slayerBlazeAttunementFill) EntityHighlightUtils.renderEntityOutline(event,color);
    }
    @SubscribeEvent public void unload(WorldEvent.Unload event) {
        if(event.world!=null && event.world.isRemote) blazeEntity.clear();
    }
}
