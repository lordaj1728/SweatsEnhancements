package com.sweatsenhancements.feature;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

/** Pet text card with a vanilla item icon; no redistributed third-party portraits. */
public final class PetCard {
    public static final int HEIGHT = 28;
    private static final int TEXT_X = 30;
    private PetCard() { }
    public static int width(Minecraft mc, List<HudLayout.Line> lines) {
        int w=0;
        for(HudLayout.Line line:lines)w=Math.max(w,mc.fontRendererObj.getStringWidth(line.text));
        return Math.max(78,TEXT_X+w+6);
    }
    public static void draw(Minecraft mc,int x,int y,List<HudLayout.Line> lines,String iconKey){
        try{
            GlStateManager.pushMatrix();
            mc.getRenderItem().renderItemAndEffectIntoGUI(new ItemStack(Items.name_tag),x+5,y+5);
        }catch(Throwable ignored){ /* Always retain readable text. */ }
        finally{GlStateManager.popMatrix();}
        for(int i=0;i<lines.size();i++){
            HudLayout.Line line=lines.get(i);
            mc.fontRendererObj.drawStringWithShadow(line.text,x+TEXT_X,y+2+i*12,line.colour);
        }
        GlStateManager.color(1f,1f,1f,1f);
    }
}
