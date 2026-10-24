package com.sweatsenhancements.gui;
import com.sweatsenhancements.feature.nef.exact.Position;
import com.sweatsenhancements.feature.nef.exact.SlayerSettingsStore;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import java.io.IOException;

/** Edit NEF's persisted overlay positions by dragging the preview; Esc saves. */
public final class SlayerPositionEditor extends GuiScreen {
    private final boolean health;
    private int lastX,lastY;
    private boolean dragging;
    public SlayerPositionEditor(boolean health){this.health=health;}
    private Position pos(){return health?SlayerSettingsStore.get().bossSettings.slayerBossHPPos:
        SlayerSettingsStore.get().slayerOverlaySettings.slayerOverlayPos;}
    @Override public void drawScreen(int mouseX,int mouseY,float partialTicks){
        drawDefaultBackground();super.drawScreen(mouseX,mouseY,partialTicks);
        drawCenteredString(fontRendererObj,"Drag the Slayer overlay. Press Esc to save.",width/2,Math.max(8,height/6),0xFFFFFFFF);
        Position p=pos();if(p==null)return;
        String[] sample=health?new String[]{"§cVoidgloom Seraph §7❤ 12M"}:
            new String[]{"§4Voidgloom Seraph§r Slayer","§7Level: §a7","§dRNGesus Meter§7:§d 48%","§7Slay the boss!"};
        int w=0;for(String line:sample)w=Math.max(w,fontRendererObj.getStringWidth(line));
        ScaledResolution r=new ScaledResolution(mc);int x=p.getAbsX(r,w),y=p.getAbsY(r,sample.length*11);
        drawRect(x-3,y-3,x+w+4,y+sample.length*11+2,0x992D7EFF);
        for(String line:sample){fontRendererObj.drawStringWithShadow(line,x,y,0xFFFFFFFF);y+=11;}
    }
    @Override protected void mouseClicked(int x,int y,int button) throws IOException {
        if(button==0){dragging=true;lastX=x;lastY=y;}else super.mouseClicked(x,y,button);
    }
    @Override protected void mouseClickMove(int x,int y,int button,long held){
        if(dragging&&button==0&&pos()!=null){ScaledResolution r=new ScaledResolution(mc);
            pos().moveX(x-lastX,180,r);pos().moveY(y-lastY,50,r);lastX=x;lastY=y;}
    }
    @Override protected void mouseReleased(int x,int y,int button){dragging=false;SlayerSettingsStore.save();super.mouseReleased(x,y,button);}
    @Override public void onGuiClosed(){SlayerSettingsStore.save();super.onGuiClosed();}
}
