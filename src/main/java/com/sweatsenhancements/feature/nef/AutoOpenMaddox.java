/* Adapted from NEF AutoOpenMaddox.java (f80cddf6), LGPL-3.0.
 * NEF's [OPEN MENU] trigger and 10-second auto/semi-auto methods retained.
 * SweatsEnhancements validates the server's clickable /openmaddox command to avoid unrelated links. */
package com.sweatsenhancements.feature.nef;
import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.feature.nef.exact.SlayerSettingsStore;
import com.sweatsenhancements.config.Cfg;
import com.sweatsenhancements.feature.DungeonRunHud;
import net.minecraft.event.ClickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.util.List;


public class AutoOpenMaddox {

    private static boolean auto() { return SweatsEnhancements.cfg != null && SlayerSettingsStore.get().slayerMaddoxCalling == 0; }
    private static boolean semi() { return SweatsEnhancements.cfg != null && SlayerSettingsStore.get().slayerMaddoxCalling == 1 && !auto(); }
    private static boolean active() { return SweatsEnhancements.mc != null && SweatsEnhancements.mc.thePlayer != null
            && SweatsEnhancements.mc.theWorld != null && !DungeonRunHud.isInDungeon(); }
    private static final String maddoxCommand = "/openmaddox";
    private static long lastMaddoxTime = 0;
    private static boolean opened = false;

    @SubscribeEvent
    public void onChatRecieve(ClientChatReceivedEvent e) {
        if (!active() || (!auto() && !semi()) || e == null || e.message == null || e.type == 2) return;
        List<IChatComponent> siblings = e.message.getSiblings();
        for (IChatComponent sibling : siblings) {
            if (sibling.getUnformattedText().contains("[OPEN MENU]")) {
                ClickEvent click = sibling.getChatStyle() == null ? null : sibling.getChatStyle().getChatClickEvent();
                if (click == null || !maddoxCommand.equalsIgnoreCase(click.getValue())) continue;
                lastMaddoxTime = System.currentTimeMillis() / 1000;
                opened = false;
                if (auto())
                    Minecraft.getMinecraft().thePlayer.sendChatMessage(maddoxCommand);
                else if (semi())
                    Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.DARK_GREEN + "Open chat then click anywhere on-screen to open Maddox"));
            }
        }
    }

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent e) {
        if (active() && semi() && e.gui instanceof GuiChat && System.currentTimeMillis() / 1000 - lastMaddoxTime < 10 && (!opened)) {
            GuiScreen.drawRect(0, 0, Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight, new Color(0, 0, 0, 56).getRGB());
            String m = "Click to Open Maddox Batphone";
            FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
            fr.drawString(m, Minecraft.getMinecraft().displayWidth - (fr.getStringWidth(m) / 2), Minecraft.getMinecraft().displayHeight / 2, -1);

        }
    }

    @SubscribeEvent
    public void onMouseInputPost(GuiScreenEvent.MouseInputEvent.Post event) {
        if (opened || !active()) return;
        if (Mouse.getEventButton() == 0 && event.gui instanceof GuiChat
                && semi()
                && System.currentTimeMillis() / 1000 - lastMaddoxTime < 10) {
            Minecraft.getMinecraft().thePlayer.sendChatMessage(maddoxCommand);
            opened = true;
        }
    }

}
