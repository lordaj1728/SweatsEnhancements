package com.sweatsenhancements.feature;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** Restore last GUI pointer position after vanilla recentres it while ungrabbing the mouse. */
public final class NoCursorReset {
    private int x, y;
    private boolean remembered;
    private GuiScreen target;

    private boolean enabled() { return SweatsEnhancements.cfg != null && SweatsEnhancements.cfg.getBool(Cfg.key("DU003"), false); }

    @SubscribeEvent
    public void onOpen(GuiOpenEvent event) {
        try {
            if (!enabled() || SweatsEnhancements.mc == null || !Mouse.isCreated()) return;
            GuiScreen old = SweatsEnhancements.mc.currentScreen;
            GuiScreen next = event.gui;
            if (old != null && !(old instanceof net.minecraft.client.gui.GuiChat)) {
                x = Mouse.getX(); y = Mouse.getY(); remembered = true;
            }
            if (next != null && remembered && !(next instanceof net.minecraft.client.gui.GuiChat)) target = next;
            else target = null;
        } catch (Throwable t) { System.out.println("[SweatsEnhancements] cursor capture failed: " + t); }
    }

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || target == null) return;
        try {
            if (enabled() && Mouse.isCreated() && Display.isCreated()
                    && SweatsEnhancements.mc != null && SweatsEnhancements.mc.currentScreen == target && !Mouse.isGrabbed())
                Mouse.setCursorPosition(Math.max(0, Math.min(Display.getWidth() - 1, x)),
                        Math.max(0, Math.min(Display.getHeight() - 1, y)));
        } catch (Throwable t) { System.out.println("[SweatsEnhancements] cursor restore failed: " + t); }
        finally { target = null; }
    }

    @SubscribeEvent
    public void onUnload(WorldEvent.Unload event) {
        if (event.world != null && event.world.isRemote) { remembered = false; target = null; }
    }
}
