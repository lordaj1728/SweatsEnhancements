package net.minecraft.client.gui;

/** Stand-in for the client's text box, in the production names the config library calls. */
public class GuiTextField {
    private String text = "";
    private int cursor;
    private int maxLength = 32;
    private boolean enabled = true;
    private boolean visible = true;
    private boolean focused;
    private boolean background = true;

    public GuiTextField(int id, FontRenderer font, int x, int y, int width, int height) { }

    public String func_146179_b() { return text; }
    public void func_146180_a(String value) { this.text = value == null ? "" : value; }
    public int func_146186_n() { return cursor; }
    public void func_146190_e(int position) { this.cursor = position; }
    public void func_146195_b(boolean value) { this.enabled = value; }
    public int func_146198_h() { return maxLength; }
    public void func_146199_i(int position) { this.cursor = position; }
    public boolean func_146201_a(char typed, int keyCode) { return true; }
    public void func_146203_f(int value) { this.maxLength = value; }
    public void func_146205_d(boolean value) { this.visible = value; }
    public String func_146207_c() { return text; }
    public void func_146193_g(int colour) { }
    public void func_146185_a(boolean value) { this.background = value; }
    public void func_146189_e(boolean value) { this.visible = value; }
    public void func_146194_f() { }
    public void func_146195_a(boolean value) { this.focused = value; }
    public boolean func_146206_l() { return focused; }
    public void func_146177_a(int position) { }
}
