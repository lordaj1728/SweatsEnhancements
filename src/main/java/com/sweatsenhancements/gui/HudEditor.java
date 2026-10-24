package com.sweatsenhancements.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.feature.HudLayout;
import com.sweatsenhancements.feature.HudTools;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.EnumChatFormatting;

/**
 * The HUD editor: pick an element up and put it where you want it.
 *
 * Every element is drawn in place and can be dragged. Switched-off elements appear greyed so they
 * can be positioned before being turned on. Dragging something pins it; R puts everything back in
 * the automatic stack.
 *
 * This is the reference way of doing it - move it on screen rather than type coordinates.
 */
public class HudEditor extends GuiScreen {

    private HudTools.Placed dragging;
    /** whatever the cursor was over on the last frame, used by the reset key */
    private HudTools.Placed hovered;
    private int grabX;
    private int grabY;
    private List<HudTools.Placed> placed = new ArrayList<HudTools.Placed>();

    @Override
    public void onGuiClosed() {
        SweatsEnhancements.cfg.save();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        try {
            drawDefaultBackground();
            Minecraft mc = Minecraft.getMinecraft();

            placed = HudTools.place(mc, true);
            hovered = null;

            for (HudTools.Placed p : placed) {
                boolean on = HudTools.isEnabled(p.element);
                boolean underCursor = p.covers(mc, mouseX, mouseY);
                if (underCursor) hovered = p;
                boolean held = p == dragging;
                int border = held ? 0xFF7FD47F : (underCursor ? 0xAAFFFFFF : 0x55FFFFFF);

                Gui.drawRect(p.x - 3, p.y - 2, p.x + p.width(mc) + 3, p.y + p.height() + 1, 0x66000000);
                Gui.drawRect(p.x - 3, p.y - 2, p.x + p.width(mc) + 3, p.y - 1, border);
                Gui.drawRect(p.x - 3, p.y + p.height(), p.x + p.width(mc) + 3, p.y + p.height() + 1, border);

                if (!p.element.panel) {
                    HudTools.draw(mc, p);
                } else {
                    // The scoreboard renders itself with its own scale; only its box is edited.
                }

                if (!on) {
                    mc.fontRendererObj.drawStringWithShadow(EnumChatFormatting.DARK_GRAY + "(off)",
                            p.x, p.y + p.height() + 3, 0xFFFFFF);
                }
            }

            drawChrome(mouseX, mouseY);
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] HUD editor draw failed: " + t);
        }
    }

    private void drawChrome(int mouseX, int mouseY) {
        String title = EnumChatFormatting.GOLD + "HUD editor";
        drawCenteredString(fontRendererObj, title, this.width / 2, 6, 0xFFFFFF);
        drawCenteredString(fontRendererObj, EnumChatFormatting.GRAY
                + "drag to move • hover + scroll to resize (scoreboard included)", this.width / 2, 18, 0xFFFFFF);

        // R acts on whatever is under the cursor, which is what the cursor is already pointing at.
        // Resetting everything stays available, but on shift so it cannot happen by accident.
        boolean shift = org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LSHIFT)
                || org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RSHIFT);
        String hint = EnumChatFormatting.GRAY + "R" + EnumChatFormatting.DARK_GRAY
                + (hovered == null ? " reset (hover an element)" : " reset " + hovered.element.label)
                + EnumChatFormatting.GRAY + "   Shift+R" + EnumChatFormatting.DARK_GRAY + " reset all"
                + EnumChatFormatting.GRAY + "   Esc" + EnumChatFormatting.DARK_GRAY + " done";
        drawCenteredString(fontRendererObj, hint, this.width / 2, this.height - 22, 0xFFFFFF);

        String moved = HudLayout.pinnedCount() > 0
                ? EnumChatFormatting.GRAY + String.valueOf(HudLayout.pinnedCount()) + " element(s) moved"
                : EnumChatFormatting.DARK_GRAY + "nothing moved, elements stack top-left";
        drawCenteredString(fontRendererObj, moved, this.width / 2, this.height - 12, 0xFFFFFF);

        // name whatever is under the cursor, so a greyed element is still identifiable
        for (HudTools.Placed p : placed) {
            if (p.covers(Minecraft.getMinecraft(), mouseX, mouseY)) {
                String label = EnumChatFormatting.WHITE + p.element.label
                        + EnumChatFormatting.DARK_GRAY + (p.element.feature == null
                        ? "  (no switch)" : "  " + p.element.feature);
                drawString(fontRendererObj, label, mouseX + 8, mouseY - 12, 0xFFFFFF);
                break;
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        try {
            int wheel = org.lwjgl.input.Mouse.getEventDWheel();
            if (wheel == 0 || dragging != null) return;
            Minecraft client = Minecraft.getMinecraft();
            int x = org.lwjgl.input.Mouse.getEventX() * width / client.displayWidth;
            int y = height - org.lwjgl.input.Mouse.getEventY() * height / client.displayHeight - 1;
            // Last placed is on top when hitboxes overlap.
            for (int i = placed.size() - 1; i >= 0; i--) {
                HudTools.Placed target = placed.get(i);
                if (!target.covers(client, x, y)) continue;
                HudLayout.resize(target.element, wheel);
                SweatsEnhancements.cfg.save();
                break;
            }
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] HUD resize failed: " + t);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            // topmost first, so an element lying on top of another is the one you grab
            for (int i = placed.size() - 1; i >= 0; i--) {
                HudTools.Placed p = placed.get(i);
                if (!p.covers(mc, mouseX, mouseY)) continue;

                // the scoreboard keeps its position in its own settings, not in a HUD coordinate,
                // so nothing is pinned for it here - see the drag below
                if (!p.element.panel && !HudLayout.isPinned(p.element)) {
                    // an element that is still auto-stacked has no stored position yet; pin it
                    // where it currently sits so it does not jump the moment it is picked up
                    HudLayout.set(p.element, p.x, p.y);
                }
                dragging = p;
                grabX = mouseX - p.x;
                grabY = mouseY - p.y;
                return;
            }
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] HUD editor click failed: " + t);
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        try {
            if (dragging == null) return;
            Minecraft mc = Minecraft.getMinecraft();
            int x = clampX(mouseX - grabX, dragging.width(mc));
            int y = clampY(mouseY - grabY, dragging.height());
            dragging.x = x;
            dragging.y = y;
            if (dragging.element.panel) {
                com.sweatsenhancements.feature.CustomScoreboard.moveTo(x, y);
            } else {
                HudLayout.set(dragging.element, x, y);
            }
        } catch (Throwable t) {
            // a failed drag is not worth a crash
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        try {
            if (dragging != null) {
                SweatsEnhancements.cfg.save();
                dragging = null;
            }
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] HUD editor release failed: " + t);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        try {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                this.mc.displayGuiScreen(null);
                return;
            }
            if (keyCode == Keyboard.KEY_R) {
                if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
                    HudLayout.resetAll();
                    com.sweatsenhancements.feature.CustomScoreboard.resetPosition();
                } else if (hovered != null && hovered.element.panel) {
                    // the scoreboard goes back to the right edge, upright centre - the position it
                    // draws in before anyone moves it
                    com.sweatsenhancements.feature.CustomScoreboard.resetPosition();
                } else if (hovered != null) {
                    HudLayout.unpin(hovered.element);
                }
                SweatsEnhancements.cfg.save();
                return;
            }
            super.keyTyped(typedChar, keyCode);
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] HUD editor key failed: " + t);
        }
    }

    private int clampX(int x, int width) {
        int max = Math.max(0, this.width - width - 1);
        return x < 0 ? 0 : (x > max ? max : x);
    }

    private int clampY(int y, int height) {
        int max = Math.max(0, this.height - height - 1);
        return y < 0 ? 0 : (y > max ? max : y);
    }
}
