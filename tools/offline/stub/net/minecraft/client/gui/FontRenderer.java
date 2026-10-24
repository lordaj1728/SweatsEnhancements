package net.minecraft.client.gui;

import java.util.ArrayList;
import java.util.List;

/** Stand-in for the client's font, with the measurements the config library asks for offline. */
public class FontRenderer {
    public int FONT_HEIGHT = 9;

    public int getStringWidth(String text) {
        if (text == null) return 0;
        int width = 0;
        boolean code = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (code) { code = false; continue; }
            if (c == '\u00a7') { code = true; continue; }
            width += 6;
        }
        return width;
    }

    public int getStringWidth(String text, boolean shadow) { return getStringWidth(text); }
    /** the production name of the same method */
    public int func_78256_a(String text) { return getStringWidth(text); }
    public int drawString(String text, int x, int y, int colour) { return 0; }
    public int drawString(String text, float x, float y, int colour, boolean shadow) { return 0; }
    public int drawStringWithShadow(String text, int x, int y, int colour) { return 0; }
    public int drawStringWithShadow(String text, float x, float y, int colour) { return 0; }
    public void drawSplitString(String text, int x, int y, int width, int colour) { }
    public int drawString(String text, int x, int y, int colour, boolean shadow) { return 0; }

    public List<String> listFormattedStringToWidth(String text, int width) {
        List<String> out = new ArrayList<String>();
        out.add(text);
        return out;
    }

    public String trimStringToWidth(String text, int width) { return text; }
    public String trimStringToWidth(String text, int width, boolean reverse) { return text; }
    public int splitStringWidth(String text, int width) { return FONT_HEIGHT; }
    public int getWordWrappedHeight(String text, int width) { return FONT_HEIGHT; }
    public boolean getUnicodeFlag() { return false; }
}
