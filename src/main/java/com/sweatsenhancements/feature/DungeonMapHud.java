package com.sweatsenhancements.feature;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.world.storage.MapData;

/**
 * NEF-style dungeon map: draw the received vanilla map with Minecraft's own
 * MapItemRenderer. SweatsEnhancements adaptation only selects its tagged dungeon map and uses
 * the existing movable HUD placement. No room fingerprint, grid or puzzle data.
 * See THIRD-PARTY-NEF-NOTICE.txt (LGPL-3.0-or-later).
 */
public final class DungeonMapHud {
    public static final int WIDTH=128, HEIGHT=128;
    private DungeonMapHud() { }
    public static ItemStack find(Minecraft mc) {
        if(mc==null||mc.thePlayer==null||mc.theWorld==null)return null;
        ItemStack[] inventory=mc.thePlayer.inventory.mainInventory;
        if(inventory==null)return null;
        for(ItemStack stack:inventory)
            if(stack!=null&&stack.getItem() instanceof ItemMap&&DungeonFloorSixInfo.dungeonMap(stack))return stack;
        return null;
    }
    public static boolean available(Minecraft mc){return find(mc)!=null;}
    public static void draw(Minecraft mc,int x,int y){
        if(mc==null||mc.thePlayer==null||mc.theWorld==null||mc.entityRenderer==null
                ||mc.entityRenderer.getMapItemRenderer()==null||SweatsEnhancements.cfg==null
                ||!SweatsEnhancements.cfg.getBool(Cfg.key("DU016"),false)||!DungeonRunHud.isInDungeon())return;
        ItemStack item=find(mc);if(item==null)return;
        MapData data=((ItemMap)item.getItem()).getMapData(item,mc.theWorld);
        if(data==null||data.colors==null||data.colors.length<128*128)return;
        GlStateManager.pushMatrix();
        try{
            GlStateManager.translate(x,y,0f);
            GlStateManager.enableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            mc.entityRenderer.getMapItemRenderer().renderMap(data,false);
        }finally{
            GlStateManager.enableTexture2D();
            GlStateManager.enableAlpha();
            GlStateManager.depthMask(true);
            GlStateManager.color(1f,1f,1f,1f);
            GlStateManager.popMatrix();
        }
    }
    public static String status(Minecraft mc){
        ItemStack stack=find(mc);if(stack==null)return "no tagged dungeon map in inventory";
        MapData data=((ItemMap)stack.getItem()).getMapData(stack,mc.theWorld);
        return data==null?"map data unavailable":"received map #"+stack.getItemDamage()
                +"; vanilla pixels="+(data.colors==null?0:data.colors.length)
                +"; markers="+(data.mapDecorations==null?0:data.mapDecorations.size());
    }
}
