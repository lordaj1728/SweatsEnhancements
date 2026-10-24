package net.minecraft.client;

/**
 * Stand-in for the client, only so the config library can build its widgets outside the game and
 * so the scoreboard's state machine can be driven without one.
 *
 * The member names are the production ones - a stub that renames them fails with a
 * NoSuchFieldError / NoSuchMethodError part way through, naming the member it wanted.
 */
public class Minecraft {
    private static final Minecraft INSTANCE = new Minecraft();

    /** the client's font, in the production name the config library uses */
    public net.minecraft.client.gui.FontRenderer field_71466_p = new net.minecraft.client.gui.FontRenderer();
    public net.minecraft.client.gui.FontRenderer fontRendererObj = field_71466_p;
    public Object field_71462_r = null;

    /**
     * The world the panel reads the scoreboard from, and the player it reports to.
     *
     * The world's type is the production one - the real field is declared as WorldClient, and a
     * stub that widens it to World fails with NoSuchFieldError: theWorld, because field resolution
     * wants the exact type.
     */
    public net.minecraft.client.multiplayer.WorldClient theWorld = null;
    public net.minecraft.client.entity.EntityPlayerSP thePlayer = null;

    public static Minecraft func_71410_x() { return INSTANCE; }
    public static Minecraft getMinecraft() { return INSTANCE; }
}
