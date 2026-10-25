package com.sweatsenhancements.feature;

import java.util.ArrayDeque;
import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Mouse;

/** Counts only local, in-world left-button DOWN transitions in the last rolling second. */
public final class ClickRateHud {
    private static final ArrayDeque<Long> CLICKS = new ArrayDeque<Long>();

    public static synchronized int count(long now) {
        while (!CLICKS.isEmpty() && (now < CLICKS.peekFirst() || now - CLICKS.peekFirst() >= 1000L))
            CLICKS.removeFirst();
        return CLICKS.size();
    }

    public static synchronized void record(long now) {
        count(now);
        if (CLICKS.size() < 128) CLICKS.addLast(now);
    }

    public static synchronized void reset() { CLICKS.clear(); }
    public static int cps() { return count(System.currentTimeMillis()); }

    @SubscribeEvent
    public void onMouse(InputEvent.MouseInputEvent event) {
        try {
            if (SweatsEnhancements.cfg == null || !SweatsEnhancements.cfg.getBool(Cfg.key("MS226"), false)
                    || SweatsEnhancements.mc == null || SweatsEnhancements.mc.theWorld == null || SweatsEnhancements.mc.currentScreen != null) return;
            if (Mouse.getEventButton() == 0 && Mouse.getEventButtonState()) record(System.currentTimeMillis());
        } catch (Throwable t) { System.out.println("[SweatsEnhancements] CPS counter failed: " + t); }
    }
}
