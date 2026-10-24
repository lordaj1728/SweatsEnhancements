package com.arena.pekaclient.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

/**
 * Low-level 2D primitives built on Minecraft's own Tessellator / WorldRenderer.
 *
 * Only fixed-function GL 1.x features are used (blend, texture enable, alpha test,
 * scissor). No shaders, FBOs, VBOs, stencil or extensions, so it runs on desktop GL
 * and on Android launchers such as PojavLauncher (GL4ES).
 *
 * Rounded shapes are triangle fans with a 1-px analytic feather for anti-aliasing.
 * All scratch buffers are static, so drawing does not allocate.
 */
public final class UIRender {

    public static final int MAX_SEG = 8;
    private static final int MAX_POINTS = 4 * (MAX_SEG + 1);

    /** Unit-circle vectors per corner, precomputed for every segment count. */
    private static final float[][] UX = new float[MAX_SEG + 1][];
    private static final float[][] UY = new float[MAX_SEG + 1][];

    private static final float[] PX = new float[MAX_POINTS];
    private static final float[] PY = new float[MAX_POINTS];
    private static final float[] QX = new float[MAX_POINTS];
    private static final float[] QY = new float[MAX_POINTS];
    private static final int[] PC = new int[MAX_POINTS];

    /** Global opacity multiplier for everything drawn (used for fades). */
    public static float alphaMul = 1f;
    /** Max arc subdivisions per quarter circle (curve quality). */
    public static int segCap = MAX_SEG;
    /** Whether soft shadows are drawn. */
    public static boolean shadows = true;

    private static boolean batchMode;
    private static boolean batchOpen;

    static {
        for (int s = 1; s <= MAX_SEG; s++) {
            int n = 4 * (s + 1);
            UX[s] = new float[n];
            UY[s] = new float[n];
            for (int g = 0; g < 4; g++) {
                for (int i = 0; i <= s; i++) {
                    double a = Math.toRadians(180.0 + 90.0 * g + 90.0 * i / s);
                    UX[s][g * (s + 1) + i] = (float) Math.cos(a);
                    UY[s][g * (s + 1) + i] = (float) Math.sin(a);
                }
            }
        }
    }

    private UIRender() {
    }

    // ------------------------------------------------------------------ frame state

    /** Saves/sets GL state for a block of custom drawing. Pair with {@link #endFrame()}. */
    public static void beginFrame() {
        batchMode = false;
        batchOpen = false;
        alphaMul = 1f;
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();
        GlStateManager.disableAlpha();
        // UI is 2D: depth testing off, so the world's depth values can never hide a shape.
        GlStateManager.disableDepth();
        // Shapes wind either way (rounded ones and rectangles differ). Culling would hide the rounded ones.
        GlStateManager.disableCull();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    /** Restores the state vanilla GUI code expects. */
    public static void endFrame() {
        flush();
        alphaMul = 1f;
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    /** Draws any pending batched triangles. */
    public static void flush() {
        if (batchOpen) {
            Tessellator.getInstance().draw();
            batchOpen = false;
        }
    }

    /** Scales a control around its centre without changing its layout or hitbox. */
    public static void pushScaleAt(float centerX, float centerY, float scale) {
        flush();
        GlStateManager.pushMatrix();
        GlStateManager.translate(centerX, centerY, 0f);
        GlStateManager.scale(scale, scale, 1f);
        GlStateManager.translate(-centerX, -centerY, 0f);
    }

    /** Flushes geometry while the scale is active, then restores the previous matrix. */
    public static void popTransform() {
        flush();
        GlStateManager.popMatrix();
    }

    /** Groups following primitives into one draw call until {@link #endBatch()}. */
    public static void beginBatch() {
        flush();
        batchMode = true;
    }

    public static void endBatch() {
        batchMode = false;
        flush();
    }

    private static WorldRenderer open() {
        WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
        if (!batchOpen) {
            wr.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
            batchOpen = true;
        }
        return wr;
    }

    private static void close() {
        if (!batchMode) {
            flush();
        }
    }

    private static void vtx(WorldRenderer wr, float x, float y, int c) {
        wr.pos(x, y, 0.0D).color((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, (c >>> 24) & 0xFF).endVertex();
    }

    // ------------------------------------------------------------------ colour helpers

    /** Applies the global alpha multiplier to an ARGB colour. */
    public static int col(int c) {
        if (alphaMul >= 0.999f) {
            return c;
        }
        int a = (int) (((c >>> 24) & 0xFF) * alphaMul);
        return (a << 24) | (c & 0xFFFFFF);
    }

    public static int mix(int a, int b, float t) {
        t = clamp01(t);
        int aa = a >>> 24, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = b >>> 24, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int oa = aa + Math.round((ba - aa) * t);
        int or = ar + Math.round((br - ar) * t);
        int og = ag + Math.round((bg - ag) * t);
        int ob = ab + Math.round((bb - ab) * t);
        return (oa << 24) | (or << 16) | (og << 8) | ob;
    }

    public static int withAlpha(int c, float a) {
        return (Math.round(clamp01(a) * 255f) << 24) | (c & 0xFFFFFF);
    }

    public static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    // ------------------------------------------------------------------ shapes

    public static void rect(float x, float y, float w, float h, int color) {
        if (w <= 0f || h <= 0f) {
            return;
        }
        int c = col(color);
        if ((c >>> 24) == 0) {
            return;
        }
        WorldRenderer wr = open();
        vtx(wr, x, y, c);
        vtx(wr, x, y + h, c);
        vtx(wr, x + w, y + h, c);
        vtx(wr, x, y, c);
        vtx(wr, x + w, y + h, c);
        vtx(wr, x + w, y, c);
        close();
    }

    /**
     * Anti-aliased rounded rectangle with a vertical gradient (top to bottom).
     * @param feather width in pixels of the soft edge; 0 for hard edges. Also used for shadows.
     */
    public static void roundRect(float x, float y, float w, float h, float r,
                                 int top, int bottom, float feather) {
        if (w <= 0f || h <= 0f) {
            return;
        }
        int t = col(top);
        int b = col(bottom);
        if (((t | b) >>> 24) == 0) {
            return;
        }
        float maxR = Math.min(w, h) * 0.5f;
        if (r > maxR) {
            r = maxR;
        }
        if (r < 0f) {
            r = 0f;
        }
        int s = segmentsFor(r);
        int per = s + 1;
        int m = 4 * per;
        float[] ux = UX[s];
        float[] uy = UY[s];
        float invH = 1f / h;
        boolean f = feather > 0f;

        for (int k = 0; k < m; k++) {
            int g = k / per;
            float ccx = (g == 0 || g == 3) ? x + r : x + w - r;
            float ccy = (g < 2) ? y + r : y + h - r;
            float px = ccx + r * ux[k];
            float py = ccy + r * uy[k];
            PX[k] = px;
            PY[k] = py;
            PC[k] = mix(t, b, (py - y) * invH);
            if (f) {
                float rr = r + feather;
                QX[k] = ccx + rr * ux[k];
                QY[k] = ccy + rr * uy[k];
            }
        }

        float cx = x + w * 0.5f;
        float cy = y + h * 0.5f;
        int cc = mix(t, b, 0.5f);
        WorldRenderer wr = open();
        for (int k = 0; k < m; k++) {
            int k2 = (k + 1) % m;
            vtx(wr, cx, cy, cc);
            vtx(wr, PX[k], PY[k], PC[k]);
            vtx(wr, PX[k2], PY[k2], PC[k2]);
        }
        if (f) {
            for (int k = 0; k < m; k++) {
                int k2 = (k + 1) % m;
                int z0 = PC[k] & 0x00FFFFFF;
                int z2 = PC[k2] & 0x00FFFFFF;
                vtx(wr, PX[k], PY[k], PC[k]);
                vtx(wr, PX[k2], PY[k2], PC[k2]);
                vtx(wr, QX[k2], QY[k2], z2);
                vtx(wr, PX[k], PY[k], PC[k]);
                vtx(wr, QX[k2], QY[k2], z2);
                vtx(wr, QX[k], QY[k], z0);
            }
        }
        close();
    }

    /** Rounded rectangle with a 1-px border colour. */
    public static void roundRectBorder(float x, float y, float w, float h, float r,
                                       int top, int bottom, int border) {
        roundRect(x, y, w, h, r, border, border, 1f);
        roundRect(x + 1f, y + 1f, w - 2f, h - 2f, Math.max(0f, r - 1f), top, bottom, 1f);
    }

    private static final float[] RING_OX = new float[4 * (MAX_SEG + 1)];
    private static final float[] RING_OY = new float[4 * (MAX_SEG + 1)];
    private static final float[] RING_IX = new float[4 * (MAX_SEG + 1)];
    private static final float[] RING_IY = new float[4 * (MAX_SEG + 1)];

    /**
     * Rounded outline of thickness {@code t}, drawn inside the rectangle (x, y, w, h).
     * Unlike {@link #roundRectBorder}, this does not fill the interior, so it works with a transparent
     * background. Built as quads between an outer and an inner perimeter. Does not allocate.
     */
    public static void roundRectRing(float x, float y, float w, float h, float r, float t, int color) {
        int c = col(color);
        if ((c >>> 24) == 0 || w <= t * 2f || h <= t * 2f) {
            return;
        }
        float maxR = Math.min(w, h) * 0.5f;
        if (r > maxR) {
            r = maxR;
        }
        if (r < 0f) {
            r = 0f;
        }
        float ri = Math.max(0f, r - t);
        int s = segmentsFor(r);
        int per = s + 1;
        int m = 4 * per;
        float[] ux = UX[s];
        float[] uy = UY[s];
        for (int k = 0; k < m; k++) {
            int g = k / per;
            float ccx = (g == 0 || g == 3) ? x + r : x + w - r;
            float ccy = (g < 2) ? y + r : y + h - r;
            RING_OX[k] = ccx + r * ux[k];
            RING_OY[k] = ccy + r * uy[k];
            RING_IX[k] = ccx + ri * ux[k];
            RING_IY[k] = ccy + ri * uy[k];
        }
        WorldRenderer wr = open();
        for (int k = 0; k < m; k++) {
            int k2 = (k + 1) % m;
            vtx(wr, RING_OX[k], RING_OY[k], c);
            vtx(wr, RING_OX[k2], RING_OY[k2], c);
            vtx(wr, RING_IX[k2], RING_IY[k2], c);
            vtx(wr, RING_OX[k], RING_OY[k], c);
            vtx(wr, RING_IX[k2], RING_IY[k2], c);
            vtx(wr, RING_IX[k], RING_IY[k], c);
        }
        close();
    }

    /** Soft, cheap drop shadow: one feathered rounded rect offset downwards. */
    public static void shadow(float x, float y, float w, float h, float r) {
        if (!shadows) {
            return;
        }
        roundRect(x - 2f, y + 3f, w + 4f, h + 4f, r + 2f, 0x40000000, 0x40000000, 9f);
    }

    public static void circle(float cx, float cy, float r, int color) {
        roundRect(cx - r, cy - r, r * 2f, r * 2f, r, color, color, 1f);
    }

    public static void ring(float cx, float cy, float ro, float ri, int color) {
        int c = col(color);
        if ((c >>> 24) == 0) {
            return;
        }
        final int n = 16;
        WorldRenderer wr = open();
        for (int i = 0; i < n; i++) {
            double a0 = Math.PI * 2.0 * i / n;
            double a1 = Math.PI * 2.0 * (i + 1) / n;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            float o0x = cx + ro * c0, o0y = cy + ro * s0;
            float o1x = cx + ro * c1, o1y = cy + ro * s1;
            float i0x = cx + ri * c0, i0y = cy + ri * s0;
            float i1x = cx + ri * c1, i1y = cy + ri * s1;
            vtx(wr, o0x, o0y, c);
            vtx(wr, o1x, o1y, c);
            vtx(wr, i1x, i1y, c);
            vtx(wr, o0x, o0y, c);
            vtx(wr, i1x, i1y, c);
            vtx(wr, i0x, i0y, c);
        }
        close();
    }

    public static void line(float x1, float y1, float x2, float y2, float th, int color) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-3f) {
            return;
        }
        int c = col(color);
        if ((c >>> 24) == 0) {
            return;
        }
        float nx = -dy / len * th * 0.5f;
        float ny = dx / len * th * 0.5f;
        WorldRenderer wr = open();
        vtx(wr, x1 + nx, y1 + ny, c);
        vtx(wr, x2 + nx, y2 + ny, c);
        vtx(wr, x2 - nx, y2 - ny, c);
        vtx(wr, x1 + nx, y1 + ny, c);
        vtx(wr, x2 - nx, y2 - ny, c);
        vtx(wr, x1 - nx, y1 - ny, c);
        close();
    }

    public static void triangle(float x1, float y1, float x2, float y2, float x3, float y3, int color) {
        int c = col(color);
        if ((c >>> 24) == 0) {
            return;
        }
        WorldRenderer wr = open();
        vtx(wr, x1, y1, c);
        vtx(wr, x2, y2, c);
        vtx(wr, x3, y3, c);
        close();
    }

    private static int segmentsFor(float r) {
        int s = Math.round(r * 0.6f);
        if (s < 1) {
            s = 1;
        }
        if (s > segCap) {
            s = segCap;
        }
        return s;
    }

    // ------------------------------------------------------------------ text

    /**
     * Draws vanilla font text. Positions are rounded for crisp pixels at scale 1.
     * Non-1 scales are applied with a matrix push/pop so the font's own quads stay sharp.
     */
    public static void text(FontRenderer fr, String s, float x, float y, int color, float scale, boolean shadow) {
        if (s == null || s.isEmpty()) {
            return;
        }
        int c = col(color);
        if ((c >>> 24) == 0) {
            return;
        }
        flush();
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        if (scale == 1f) {
            fr.drawString(s, (float) Math.round(x), (float) Math.round(y), c, shadow);
        } else {
            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, 0f);
            GlStateManager.scale(scale, scale, 1f);
            fr.drawString(s, 0f, 0f, c, shadow);
            GlStateManager.popMatrix();
        }
        GlStateManager.disableAlpha();
        GlStateManager.disableTexture2D();
    }

    // ------------------------------------------------------------------ clipping

    /** Clips subsequent drawing to a rectangle given in GUI (scaled) coordinates. */
    public static void scissorBegin(float x, float y, float w, float h, float scaleFactor) {
        flush();
        Minecraft mc = Minecraft.getMinecraft();
        int px = Math.round(x * scaleFactor);
        int py = Math.round(mc.displayHeight - (y + h) * scaleFactor);
        int pw = Math.max(0, Math.round(w * scaleFactor));
        int ph = Math.max(0, Math.round(h * scaleFactor));
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(px, py, pw, ph);
    }

    public static void scissorEnd() {
        flush();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}
