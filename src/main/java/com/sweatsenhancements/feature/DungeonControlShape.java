package com.sweatsenhancements.feature;

import net.minecraft.util.EnumFacing;

/** Per-facing client selection box calculations, kept outside transformed Minecraft classes. */
public final class DungeonControlShape {
    private DungeonControlShape() { }
    public static float[] bounds(EnumFacing attached, float thickness) {
        if (attached == null) return null;
        float[] b = {0,0,0,1,1,1};
        switch (attached) {
            case UP: b[4] = thickness; break;
            case DOWN: b[1] = 1-thickness; break;
            case NORTH: b[2] = 1-thickness; break;
            case SOUTH: b[5] = thickness; break;
            case WEST: b[0] = 1-thickness; break;
            case EAST: b[3] = thickness; break;
            default: return null;
        }
        return b;
    }
}
