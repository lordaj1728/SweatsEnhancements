package com.sweatsenhancements.feature;

import java.util.ArrayList;
import java.util.List;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * The HUD: a watermark and a few small readouts, each of which can be dragged wherever the player
 * wants it. See HudLayout for where they go and HudEditor for the dragging.
 *
 * Everything here reads the client's own state - the clock, the tick rate, the player's fishing
 * hook - so none of it depends on parsing anything the server sends.
 */
public class HudTools {

    public static final int LINE_HEIGHT = 10;
    private static final int AUTO_X = 2;
    private static final int AUTO_TOP = 2;

    // TPS: count ticks in a rolling second, then average the last five seconds
    private static long ticksThisSecond = 0L;
    private static long secondStart = 0L;
    private static final double[] recent = new double[5];
    private static int recentIndex = 0;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        try {
            if (event.phase != TickEvent.Phase.END) return;

            long now = System.currentTimeMillis();
            if (secondStart == 0L) secondStart = now;
            ticksThisSecond++;
            if (now - secondStart >= 1000L) {
                recent[recentIndex] = ticksThisSecond;
                recentIndex = (recentIndex + 1) % recent.length;
                ticksThisSecond = 0L;
                secondStart = now;
            }
        } catch (Throwable t) {
            // never let a tick handler break the game loop
        }
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Text event) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null || mc.fontRendererObj == null) return;

            for (Placed placed : place(mc, false)) {
                if ("DU016".equals(placed.element.key)) continue; // rendered after ALL, with map textures
                draw(mc, placed);
            }
        } catch (Throwable t) {
            // rendering hooks must never throw
        }
    }

    @SubscribeEvent
    public void onMapOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null || mc.fontRendererObj == null) return;
            for (Placed placed : place(mc, false)) {
                if ("DU016".equals(placed.element.key)) draw(mc, placed);
            }
        } catch (Throwable t) { System.out.println("[SweatsEnhancements] map HUD event failed: " + t); }
    }

    /** an element, its text, and where it goes this frame */
    public static final class Placed {
        public final HudLayout.Element element;
        public final List<HudLayout.Line> lines;
        public int x;
        public int y;
        /** a size of its own, for an element that is drawn by another panel - the scoreboard */
        public int fixedWidth;
        public int fixedHeight;

        Placed(HudLayout.Element element, List<HudLayout.Line> lines, int x, int y) {
            this.element = element;
            this.lines = lines;
            this.x = x;
            this.y = y;
        }

        public int width(Minecraft mc) {
            if (fixedWidth > 0) return fixedWidth;
            return Math.max(1, Math.round(rawWidth(mc) * HudLayout.scalePercent(element) / 100f));
        }

        private int rawWidth(Minecraft mc) {
            if ("MS201".equals(element.key)) return PetCard.width(mc, lines);
            int w = 0;
            for (HudLayout.Line line : lines) {
                w = Math.max(w, mc.fontRendererObj.getStringWidth(line.text));
            }
            return "DU020".equals(element.key) ? w+4 : w;
        }

        public int height() {
            if (fixedHeight > 0) return fixedHeight;
            int raw = "MS201".equals(element.key) ? PetCard.HEIGHT : lines.size() * ("DU020".equals(element.key) ? 11 : LINE_HEIGHT);
            return Math.max(1, Math.round(raw * HudLayout.scalePercent(element) / 100f));
        }

        public boolean covers(Minecraft mc, int mouseX, int mouseY) {
            return mouseX >= x - 1 && mouseX <= x + width(mc) + 1
                    && mouseY >= y - 1 && mouseY <= y + height();
        }
    }

    /**
     * Works out what to draw and where.
     *
     * Pinned elements draw at their stored position. Everything else stacks downward from the top
     * left, skipping elements that are switched off or have nothing to say, so the default layout
     * has no gaps.
     *
     * @param includeDisabled used by the editor, which shows switched-off elements greyed out so
     *                        they can still be positioned before being turned on
     */
    public static List<Placed> place(Minecraft mc, boolean includeDisabled) {
        List<Placed> out = new ArrayList<Placed>();
        int autoY = AUTO_TOP;

        for (HudLayout.Element element : HudLayout.ELEMENTS) {
            try {
                // The panel renders itself in CustomScoreboard. Its text is an editor
                // preview only; drawing it here put a second copy over the live panel.
                if (element.panel && !includeDisabled) continue;
                boolean on = includeDisabled || isEnabled(element);
                if (!on && !includeDisabled) continue;

                List<HudLayout.Line> lines = lines(mc, element, false);
                if (lines.isEmpty() && !includeDisabled) continue;
                if (lines.isEmpty()) lines = sample(element);

                if (element.panel) {
                    // The scoreboard draws itself, where its own settings say. Here it is only
                    // wrapped in a rectangle of its real size, at its real position, so it can be
                    // grabbed: the panel is drawn under the editor's own drawing in game and the
                    // player sees it move as they drag.
                    int left = com.sweatsenhancements.feature.CustomScoreboard.boxWidth > 0
                            ? com.sweatsenhancements.feature.CustomScoreboard.boxLeft : 4;
                    int top = com.sweatsenhancements.feature.CustomScoreboard.boxHeight > 0
                            ? com.sweatsenhancements.feature.CustomScoreboard.boxTop : 40;
                    Placed panel = new Placed(element, lines, left, top);
                    // the rectangle is the panel's own box, not the width of the preview text: it is
                    // what the player sees and what they mean to grab
                    panel.fixedWidth = com.sweatsenhancements.feature.CustomScoreboard.boxWidth > 0
                            ? com.sweatsenhancements.feature.CustomScoreboard.boxWidth : 130;
                    panel.fixedHeight = com.sweatsenhancements.feature.CustomScoreboard.boxHeight > 0
                            ? com.sweatsenhancements.feature.CustomScoreboard.boxHeight : 120;
                    out.add(panel);
                    continue;
                }

                int x;
                int y;
                if (HudLayout.isPinned(element)) {
                    x = HudLayout.x(element);
                    y = HudLayout.y(element);
                } else {
                    x = AUTO_X;
                    y = autoY;
                    if (isEnabled(element)) autoY += Math.max(1, Math.round(
                            ("DU016".equals(element.key) ? DungeonMapHud.HEIGHT : "MS201".equals(element.key) ? PetCard.HEIGHT : lines.size() * ("DU020".equals(element.key) ? 11 : LINE_HEIGHT))
                                    * HudLayout.scalePercent(element) / 100f));
                }
                Placed placed = new Placed(element, lines, x, y);
                if ("DU016".equals(element.key)) {
                    placed.fixedWidth = Math.round(DungeonMapHud.WIDTH * HudLayout.scalePercent(element) / 100f);
                    placed.fixedHeight = Math.round(DungeonMapHud.HEIGHT * HudLayout.scalePercent(element) / 100f);
                }
                out.add(placed);
            } catch (Throwable t) {
                // one bad element must not take the rest of the HUD with it
            }
        }
        return out;
    }

    public static boolean isEnabled(HudLayout.Element element) {
        if (element.panel) return SweatsEnhancements.cfg.getBool(Cfg.key("MS085"), true);
        if (element.feature == null) return SweatsEnhancements.cfg.getBool(Cfg.HUD_WATERMARK, true);
        return SweatsEnhancements.cfg.getBool(Cfg.key(element.feature), false);
    }

    /**
     * The text an element shows right now, or the text it would show with nothing going on.
     * The sample is what the editor draws for elements that have nothing to report - a bobber
     * timer when no line is cast, for example - so they can still be dragged into place.
     */
    public static List<HudLayout.Line> lines(Minecraft mc, HudLayout.Element element, boolean sample) {
        List<HudLayout.Line> out = new ArrayList<HudLayout.Line>();
        String key = element.key;

        if ("scoreboard".equals(key)) {
            for (String text : com.sweatsenhancements.feature.CustomScoreboard.previewLines()) {
                out.add(new HudLayout.Line(text, 0xFFFFFF));
            }
        } else if ("watermark".equals(key)) {
            out.add(new HudLayout.Line("SweatsEnhancements " + SweatsEnhancements.VERSION, 0x55CC55));
        } else if ("MS028".equals(key)) {
            out.add(new HudLayout.Line("TPS " + String.format("%.2f", SweatsEnhancements.hud.tpsValue()), 0xffcc55));
        } else if ("MS226".equals(key)) {
            out.add(new HudLayout.Line("CPS " + ClickRateHud.cps(), 0x55DDFF));
        } else if ("DU001".equals(key)) {
            out.addAll(DungeonRunHud.lines());
        } else if ("DU005".equals(key)) {
            out.addAll(DungeonBlessings.lines());
        } else if ("DU010".equals(key)) {
            if (SweatsEnhancements.dungeonVisuals != null) out.addAll(SweatsEnhancements.dungeonVisuals.timerLines());
        } else if ("DU016".equals(key)) {
            if (DungeonMapHud.available(mc) || DungeonScoreHud.active() || DungeonRunHud.isInDungeon())
                out.add(new HudLayout.Line("Dungeon Map", 0xffffff));
        } else if ("DU020".equals(key)) {
            out.addAll(DungeonScoreHud.lines());
        } else if ("DU014".equals(key)) {
            if (SweatsEnhancements.dungeonInfo != null) out.addAll(SweatsEnhancements.dungeonInfo.timerLines());
        } else if ("MS201".equals(key)) {
            CurrentPetHud.Pet pet = SweatsEnhancements.currentPet == null ? null : SweatsEnhancements.currentPet.pet();
            if (pet != null) {
                out.add(new HudLayout.Line("§7[Lvl " + pet.level + "] " + pet.colour + pet.name, 0xffffff));
                if (!pet.status.isEmpty()) out.add(new HudLayout.Line(
                        pet.maxLevel ? "§b§lMAX LEVEL" : "§e" + pet.status, 0xffffff));
            }
        }
        return out;
    }

    /** The lines the editor shows for an element, live text when there is any. */
    public static List<HudLayout.Line> sample(HudLayout.Element element) {
        List<HudLayout.Line> out = new ArrayList<HudLayout.Line>();
        if ("scoreboard".equals(element.key)) {
            for (String text : com.sweatsenhancements.feature.CustomScoreboard.previewLines()) {
                out.add(new HudLayout.Line(text, 0xFFFFFF));
            }
        } else if ("MS028".equals(element.key)) {
            out.add(new HudLayout.Line("TPS 20.00", 0xffcc55));
        } else if ("DU001".equals(element.key)) {
            out.add(new HudLayout.Line("§bDungeon §f02:17", 0xffffff));
            out.add(new HudLayout.Line("§eCleared §f18%", 0xffffff));
        } else if ("DU010".equals(element.key)) {
            out.add(new HudLayout.Line("§cBlood 01:20", 0xffffff));
        } else if ("DU016".equals(element.key)) {
            out.add(new HudLayout.Line("Dungeon Map", 0xffffff));
        } else if ("DU020".equals(element.key)) {
            out.add(new HudLayout.Line("§7Total score: §a300 §6(S+)", 0xffffff));
            out.add(new HudLayout.Line("", 0xffffff));
            out.add(new HudLayout.Line("§7Secrets: §a85% §7/ §a85% §7/ §a85%", 0xffffff));
        } else if ("DU014".equals(element.key)) {
            out.add(new HudLayout.Line("§6Terra elapsed §f00:30", 0xffffff));
        } else if ("DU005".equals(element.key)) {
            java.util.Map<String,Integer> sample = new java.util.LinkedHashMap<String,Integer>();
            sample.put("power", 6); sample.put("time", 5); sample.put("wisdom", 7);
            sample.put("stone", 10); sample.put("life", 36);
            out.addAll(DungeonBlessings.format(sample, false));
        } else if ("MS201".equals(element.key)) {
            out.add(new HudLayout.Line("§7[Lvl 100] §6Baby Yeti", 0xffffff));
            out.add(new HudLayout.Line("§b§lMAX LEVEL", 0xffffff));
        } else if ("watermark".equals(element.key)) {
            out.add(new HudLayout.Line("SweatsEnhancements " + SweatsEnhancements.VERSION, 0x55CC55));
        }
        return out;
    }

    public static void draw(Minecraft mc, Placed placed) {
        // Scale around the saved top-left anchor; editor hitboxes use the same dimensions.
        float scale = HudLayout.scalePercent(placed.element) / 100f;
        org.lwjgl.opengl.GL11.glPushMatrix();
        try {
            org.lwjgl.opengl.GL11.glTranslatef(placed.x, placed.y, 0f);
            org.lwjgl.opengl.GL11.glScalef(scale, scale, 1f);
            org.lwjgl.opengl.GL11.glTranslatef(-placed.x, -placed.y, 0f);
            drawUnscaled(mc, placed);
        } finally {
            org.lwjgl.opengl.GL11.glPopMatrix();
        }
    }

    private static void drawUnscaled(Minecraft mc, Placed placed) {
        if ("DU016".equals(placed.element.key)) {
            DungeonMapHud.draw(mc, placed.x, placed.y);
            return;
        }
        if ("MS201".equals(placed.element.key)) {
            CurrentPetHud.Pet pet = SweatsEnhancements.currentPet == null ? null : SweatsEnhancements.currentPet.pet();
            PetCard.draw(mc, placed.x, placed.y, placed.lines,
                    pet == null ? "baby_yeti" : pet.iconKey());
            return;
        }
        if ("DU020".equals(placed.element.key)) {
            int width=0;
            for (HudLayout.Line line:placed.lines)
                width=Math.max(width,mc.fontRendererObj.getStringWidth(line.text));
            net.minecraft.client.gui.Gui.drawRect(placed.x,placed.y,placed.x+width+4,
                    placed.y+placed.lines.size()*11,0x96000000);
            for (int i=0;i<placed.lines.size();i++)
                mc.fontRendererObj.drawString(placed.lines.get(i).text,placed.x+2,
                        placed.y+i*11,0xFFFFFFFF);
            return;
        }
        int y = placed.y;
        for (HudLayout.Line line : placed.lines) {
            mc.fontRendererObj.drawStringWithShadow(line.text, placed.x, y, line.colour);
            y += LINE_HEIGHT;
        }
    }

    private static double tps() {
        double sum = 0.0;
        int n = 0;
        for (double v : recent) {
            if (v > 0) {
                sum += v;
                n++;
            }
        }
        return n == 0 ? 20.0 : Math.min(20.0, sum / n);
    }

    public static double tpsValue() {
        return tps();
    }
}
