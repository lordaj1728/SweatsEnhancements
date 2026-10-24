package com.sweatsenhancements.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import com.sweatsenhancements.config.Feature;
import com.sweatsenhancements.config.Features;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.EnumChatFormatting;

/**
 * The config menu.
 *
 * Layout is the usual one for this kind of mod - category sidebar on the left, searchable
 * feature list with switches on the right - written from scratch rather than taken from any
 * other mod.
 *
 * The important thing it has to communicate: most of these features are not built yet. A
 * feature is either WORKING (implemented, switch does something) or PLANNED (registered so it
 * can be browsed, switch does nothing). The two look different on purpose, and the working
 * count is in the header so there is no guessing.
 */
public class ConfigMenu extends GuiScreen {

    private static final int SIDEBAR_W = 118;
    private static final int ROW_H = 13;
    private static final int TOP = 52;
    private static final int PAD = 6;
    private static final int SWITCH_W = 22;
    private static final int SWITCH_H = 10;

    private static final int FILTER_ALL = 0;
    private static final int FILTER_WORKING = 1;
    private static final int FILTER_PLANNED = 2;
    private static final String[] FILTER_LABELS = {"All", "Working", "Planned"};

    private GuiTextField search;
    private int catIndex = 0;
    private int filter = FILTER_ALL;
    private int scroll = 0;
    private int rowsVisible = 1;

    private final List<Feature> view = new ArrayList<Feature>();

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        search = new GuiTextField(0, fontRendererObj, PAD + SIDEBAR_W + 8, TOP - 24, 150, 16);
        search.setMaxStringLength(48);
        search.setEnableBackgroundDrawing(false);
        rebuild();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        SweatsEnhancements.cfg.save();
    }

    /** Recomputes the visible list for the current category, search text and filter. */
    private void rebuild() {
        view.clear();
        List<String> cats = Features.categories();
        if (cats.isEmpty()) return;
        if (catIndex >= cats.size()) catIndex = 0;

        String q = search == null ? "" : search.getText().trim().toLowerCase();
        for (Feature f : Features.inCategory(cats.get(catIndex))) {
            if (filter == FILTER_WORKING && !f.isLive()) continue;
            if (filter == FILTER_PLANNED && f.isLive()) continue;
            if (!q.isEmpty() && !(f.name.toLowerCase().contains(q) || f.id.toLowerCase().contains(q))) continue;
            view.add(f);
        }
        clampScroll();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (search != null) search.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        try {
            int working = countWorking();
            int total = Features.all().size();

            // ---- header
            drawCenteredString(fontRendererObj, EnumChatFormatting.GOLD + "SweatsEnhancements "
                    + EnumChatFormatting.GRAY + "v" + SweatsEnhancements.VERSION, this.width / 2, 6, 0xFFFFFF);
            String counts = EnumChatFormatting.GREEN + String.valueOf(working) + " working"
                    + EnumChatFormatting.GRAY + "  ·  "
                    + EnumChatFormatting.DARK_GRAY + String.valueOf(total - working) + " planned"
                    + EnumChatFormatting.GRAY + "  ·  " + total + " total";
            drawCenteredString(fontRendererObj, counts, this.width / 2, 19, 0xFFFFFF);
            drawCenteredString(fontRendererObj, EnumChatFormatting.DARK_GRAY
                            + "green switches work now - grey ones are not built yet",
                    this.width / 2, 31, 0xFFFFFF);

            // ---- search
            drawString(fontRendererObj, EnumChatFormatting.GRAY + "Search", PAD, TOP - 20, 0xFFFFFF);
            Gui.drawRect(PAD + SIDEBAR_W + 6, TOP - 25, PAD + SIDEBAR_W + 162, TOP - 7, 0x66000000);
            search.drawTextBox();
            if (search.getText().isEmpty() && !search.isFocused()) {
                drawString(fontRendererObj, EnumChatFormatting.DARK_GRAY + "type to filter...",
                        PAD + SIDEBAR_W + 10, TOP - 20, 0xFFFFFF);
            }

            // ---- category sidebar
            List<String> cats = Features.categories();
            int cy = TOP;
            for (int i = 0; i < cats.size(); i++) {
                String cat = cats.get(i);
                List<Feature> inCat = Features.inCategory(cat);
                int live = 0;
                for (Feature f : inCat) if (f.isLive()) live++;

                boolean sel = i == catIndex;
                boolean hover = mouseX >= PAD && mouseX <= PAD + SIDEBAR_W
                        && mouseY >= cy && mouseY < cy + ROW_H;
                if (sel) Gui.drawRect(PAD, cy, PAD + SIDEBAR_W, cy + ROW_H - 1, 0x77304A30);
                else if (hover) Gui.drawRect(PAD, cy, PAD + SIDEBAR_W, cy + ROW_H - 1, 0x33FFFFFF);

                String badge = live > 0 ? EnumChatFormatting.GREEN + "" + live : "";
                String label = EnumChatFormatting.GRAY + trim(cat, 13);
                drawString(fontRendererObj, label, PAD + 4, cy + 3, 0xFFFFFF);
                drawString(fontRendererObj, badge + EnumChatFormatting.DARK_GRAY + "/" + inCat.size(),
                        PAD + SIDEBAR_W - 30, cy + 3, 0xFFFFFF);
                cy += ROW_H;
            }

            // ---- feature list
            int left = PAD + SIDEBAR_W + 6;
            int right = this.width - PAD;
            int rows = (this.height - TOP - 30) / ROW_H;
            rowsVisible = Math.max(1, rows);
            clampScroll();
            Gui.drawRect(left, TOP - 4, right, this.height - 28, 0x40000000);

            if (view.isEmpty()) {
                drawString(fontRendererObj, EnumChatFormatting.DARK_GRAY + "nothing matches here",
                        left + 6, TOP + 4, 0xFFFFFF);
            }

            for (int r = 0; r < rows; r++) {
                int idx = scroll + r;
                if (idx >= view.size()) break;
                Feature f = view.get(idx);
                int ry = TOP + r * ROW_H;
                boolean on = SweatsEnhancements.cfg.getBool(Cfg.key(f.id), f.isLive());
                boolean live = f.isLive();
                boolean hover = mouseX >= left && mouseX < right && mouseY >= ry && mouseY < ry + ROW_H;

                if (hover) Gui.drawRect(left, ry, right, ry + ROW_H - 1, 0x33FFFFFF);
                else if (r % 2 == 1) Gui.drawRect(left, ry, right, ry + ROW_H - 1, 0x18FFFFFF);

                // switch on the right
                int swX = right - SWITCH_W - 6;
                drawSwitch(swX, ry + 1, on, live);

                String id = EnumChatFormatting.DARK_AQUA + f.id;
                String name = live ? EnumChatFormatting.WHITE + trim(f.name, 74)
                        : EnumChatFormatting.DARK_GRAY + trim(f.name, 74);
                drawString(fontRendererObj, id, left + 4, ry + 2, 0xFFFFFF);
                drawString(fontRendererObj, name, left + 42, ry + 2, 0xFFFFFF);
                if (!live) {
                    String tag = EnumChatFormatting.DARK_GRAY + "planned";
                    drawString(fontRendererObj, tag, swX - fontRendererObj.getStringWidth(tag) - 6, ry + 2, 0xFFFFFF);
                } else if (on) {
                    String tag = EnumChatFormatting.GREEN + "active";
                    drawString(fontRendererObj, tag, swX - fontRendererObj.getStringWidth(tag) - 6, ry + 2, 0xFFFFFF);
                }
            }

            drawScrollbar(left, right, view.size(), rows);

            // ---- footer: filters + bulk actions
            int fy = this.height - 24;
            int fx = left;
            for (int i = 0; i < FILTER_LABELS.length; i++) {
                int w = fontRendererObj.getStringWidth(FILTER_LABELS[i]) + 14;
                boolean sel = i == filter;
                Gui.drawRect(fx, fy, fx + w, fy + 14, sel ? 0x77304A30 : 0x44000000);
                drawString(fontRendererObj, (sel ? EnumChatFormatting.WHITE : EnumChatFormatting.GRAY)
                        + FILTER_LABELS[i], fx + 7, fy + 3, 0xFFFFFF);
                fx += w + 4;
            }
            int bx = right - 132;
            drawButton("enable all", bx, fy);
            drawButton("disable all", bx + 68, fy);

            // ---- tooltip
            if (mouseY >= TOP && mouseX >= left && mouseX < right) {
                int idx = scroll + (mouseY - TOP) / ROW_H;
                if (idx >= 0 && idx < view.size()) {
                    Feature f = view.get(idx);
                    List<String> tip = new ArrayList<String>();
                    tip.add(EnumChatFormatting.AQUA + f.id + " " + EnumChatFormatting.GRAY + f.category);
                    for (String line : fontRendererObj.listFormattedStringToWidth(f.name, 210)) {
                        tip.add(EnumChatFormatting.WHITE + line);
                    }
                    tip.add("");
                    tip.add(f.isLive()
                            ? EnumChatFormatting.GREEN + "working - this switch does something"
                            : EnumChatFormatting.DARK_GRAY + "planned - not built yet, switch is remembered");
                    drawHoveringText(tip, mouseX, mouseY);
                }
            }
        } catch (Throwable t) {
            drawString(fontRendererObj, EnumChatFormatting.RED + "SweatsEnhancements config error: " + t, PAD, this.height - 40, 0xFFFFFF);
        }
    }

    private void drawSwitch(int x, int y, boolean on, boolean live) {
        int track = on ? (live ? 0xFF3FA34F : 0xFF3A5A3A) : 0xFF555555;
        Gui.drawRect(x, y, x + SWITCH_W, y + SWITCH_H, track);
        int knobX = on ? x + SWITCH_W - SWITCH_H - 1 : x + 1;
        Gui.drawRect(knobX, y + 1, knobX + SWITCH_H - 2, y + SWITCH_H - 1, 0xFFE8E8E8);
    }

    private void drawButton(String label, int x, int y) {
        int w = 64;
        Gui.drawRect(x, y, x + w, y + 14, 0x55000000);
        drawString(fontRendererObj, EnumChatFormatting.GRAY + label, x + 6, y + 3, 0xFFFFFF);
    }

    private void drawScrollbar(int left, int right, int total, int rows) {
        if (total <= rows) return;
        int barX = right - 3;
        int barTop = TOP - 4;
        int barH = this.height - 28 - barTop;
        Gui.drawRect(barX, barTop, barX + 2, barTop + barH, 0x55000000);
        float frac = (float) rows / (float) total;
        int knob = Math.max(10, (int) (barH * frac));
        int knobY = barTop + (int) ((barH - knob) * ((float) scroll / (float) Math.max(1, total - rows)));
        Gui.drawRect(barX, knobY, barX + 2, knobY + knob, 0xAA7FD47F);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        try {
            if (search != null) search.mouseClicked(mouseX, mouseY, mouseButton);

            // category sidebar
            List<String> cats = Features.categories();
            int cy = TOP;
            for (int i = 0; i < cats.size(); i++) {
                if (mouseX >= PAD && mouseX <= PAD + SIDEBAR_W && mouseY >= cy && mouseY < cy + ROW_H) {
                    catIndex = i;
                    scroll = 0;
                    rebuild();
                    return;
                }
                cy += ROW_H;
            }

            // footer buttons
            int fy = this.height - 24;
            int left = PAD + SIDEBAR_W + 6;
            int right = this.width - PAD;
            int fx = left;
            for (int i = 0; i < FILTER_LABELS.length; i++) {
                int w = fontRendererObj.getStringWidth(FILTER_LABELS[i]) + 14;
                if (mouseX >= fx && mouseX < fx + w && mouseY >= fy && mouseY < fy + 14) {
                    filter = i;
                    scroll = 0;
                    rebuild();
                    return;
                }
                fx += w + 4;
            }
            int bx = right - 132;
            if (mouseY >= fy && mouseY < fy + 14) {
                if (mouseX >= bx && mouseX < bx + 64) {
                    setAll(true);
                    return;
                }
                if (mouseX >= bx + 68 && mouseX < bx + 132) {
                    setAll(false);
                    return;
                }
            }

            // feature rows
            if (mouseX < left || mouseX >= right || mouseY < TOP) return;
            int row = (mouseY - TOP) / ROW_H;
            int idx = scroll + row;
            if (idx < 0 || idx >= view.size()) return;
            Feature f = view.get(idx);
            boolean now = SweatsEnhancements.cfg.getBool(Cfg.key(f.id), f.isLive());
            SweatsEnhancements.cfg.setBool(Cfg.key(f.id), !now);
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] config click failed: " + t);
        }
    }

    private void setAll(boolean value) {
        try {
            for (Feature f : view) {
                SweatsEnhancements.cfg.setBool(Cfg.key(f.id), value);
            }
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] bulk toggle failed: " + t);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        try {
            int wheel = Mouse.getEventDWheel();
            if (wheel != 0 && !search.isFocused()) {
                scroll -= Integer.signum(wheel) * 2;
                clampScroll();
            }
            super.handleMouseInput();
        } catch (Throwable t) {
            // ignore
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        try {
            if (search != null && search.isFocused()) {
                if (keyCode == Keyboard.KEY_ESCAPE) {
                    search.setFocused(false);
                    return;
                }
                if (keyCode == Keyboard.KEY_RETURN) {
                    search.setFocused(false);
                    return;
                }
                if (search.textboxKeyTyped(typedChar, keyCode)) {
                    rebuild();
                    return;
                }
            }
            if (keyCode == Keyboard.KEY_ESCAPE) {
                this.mc.displayGuiScreen(null);
                return;
            }
            if (keyCode == Keyboard.KEY_UP) {
                scroll--;
                clampScroll();
                return;
            }
            if (keyCode == Keyboard.KEY_DOWN) {
                scroll++;
                clampScroll();
                return;
            }
            if (keyCode == Keyboard.KEY_PRIOR) {
                scroll -= rowsVisible;
                clampScroll();
                return;
            }
            if (keyCode == Keyboard.KEY_NEXT) {
                scroll += rowsVisible;
                clampScroll();
                return;
            }
            super.keyTyped(typedChar, keyCode);
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] config key failed: " + t);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void clampScroll() {
        int max = Math.max(0, view.size() - rowsVisible);
        if (scroll < 0) scroll = 0;
        if (scroll > max) scroll = max;
    }

    private static int countWorking() {
        int n = 0;
        for (Feature f : Features.all()) if (f.isLive()) n++;
        return n;
    }

    private static String trim(String s, int max) {
        return s.length() <= max ? s : s.substring(0, Math.max(0, max - 1)) + "\u2026";
    }
}
