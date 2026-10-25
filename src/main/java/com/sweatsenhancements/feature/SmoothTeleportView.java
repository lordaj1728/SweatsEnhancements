/*
 * Copyright (C) 2022 NotEnoughUpdates contributors.
 * SweatsEnhancements adaptation (2026) of teleport smoothing from NEU 2.4.0
 * CustomItemEffects.java and ItemOverlays.java, commit c47010a.
 * Changes: SweatsEnhancements settings and item IDs, separate ability timing, confirmed-packet
 * guards and world cleanup, removed unrelated overlays and effects.
 * This modified file is LGPL-3.0-or-later. See
 * THIRD-PARTY-NEU-LGPL-3.0.txt and THIRD-PARTY-GPL-3.0.txt.
 */
package com.sweatsenhancements.feature;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import net.minecraft.block.material.Material;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.BlockPos;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.util.vector.Vector3f;

/** Optional visual-only smoothing; the actual player is always at the server's position. */
public final class SmoothTeleportView {
    public static final int NONE = 0, NORMAL = 1, ETHERWARP = 2, SHADOW_WARP = 3;
    private static final long USE_WINDOW_MS = 1000;
    private static long lastUse, pendingUse, lastRenderTime;
    private static int animationLeft, activeDuration, activeType;
    private static Vector3f view;
    private static Object world;

    /** Classification does not depend on config; useful for offline metadata tests. */
    public static int type(ItemStack stack, boolean sneaking) {
        if (stack == null || !stack.hasTagCompound()) return NONE;
        NBTTagCompound root = stack.getTagCompound();
        if (!root.hasKey("ExtraAttributes", 10)) return NONE;
        NBTTagCompound ea = root.getCompoundTag("ExtraAttributes");
        String id = ea.getString("id");
        if ("ETHERWARP_CONDUIT".equals(id)) return ETHERWARP;
        if ("ASPECT_OF_THE_VOID".equals(id) || "ASPECT_OF_THE_END".equals(id))
            return sneaking && ea.hasKey("ethermerge") ? ETHERWARP : NORMAL;
        if ("ASPECT_OF_THE_LEECH_1".equals(id) || "ASPECT_OF_THE_LEECH_2".equals(id)
                || "ASPECT_OF_THE_LEECH_3".equals(id)) return NORMAL;
        if (!"HYPERION".equals(id) && !"ASTRAEA".equals(id)
                && !"SCYLLA".equals(id) && !"VALKYRIE".equals(id)) return NONE;
        if (ea.hasKey("ability_scroll", 9)) {
            NBTTagList scrolls = ea.getTagList("ability_scroll", 8);
            for (int i = 0; i < scrolls.tagCount(); i++)
                if ("SHADOW_WARP_SCROLL".equals(scrolls.getStringTagAt(i))) return SHADOW_WARP;
        }
        // Some adapted item metadata uses a boolean tag instead of the scroll list.
        return ea.hasKey("shadow_warp") && ea.getBoolean("shadow_warp") ? SHADOW_WARP : NONE;
    }

    public static boolean isTeleportItem(ItemStack stack) { return type(stack, false) != NONE; }

    private static boolean allowed(int type) {
        if (SweatsEnhancements.cfg == null) return false;
        if (type == SHADOW_WARP) return SweatsEnhancements.cfg.getBool("smoothTeleport.hyperion", false);
        return (type == NORMAL || type == ETHERWARP) && SweatsEnhancements.cfg.getBool(Cfg.key("MS223"), false);
    }

    public static boolean enabled() { return allowed(NORMAL) || allowed(SHADOW_WARP); }

    public static int timeFor(int type) {
        if (SweatsEnhancements.cfg == null) return 0;
        String key = type == ETHERWARP ? "smoothTeleport.etherwarpMs" : "smoothTeleport.normalMs";
        int fallback = type == ETHERWARP ? 50 : 125;
        return Math.max(0, Math.min(500, SweatsEnhancements.cfg.getInt(key, fallback)));
    }

    public static Vector3f getCurrentPosition() {
        if (!allowed(activeType) || SweatsEnhancements.mc == null || SweatsEnhancements.mc.theWorld != world
                || SweatsEnhancements.mc.gameSettings.thirdPersonView != 0 || animationLeft <= 0) return null;
        return view;
    }

    @SubscribeEvent
    public void onUse(PlayerInteractEvent event) {
        try {
            if (event == null || SweatsEnhancements.mc == null || SweatsEnhancements.mc.thePlayer == null || SweatsEnhancements.mc.theWorld == null
                    || event.entityPlayer != SweatsEnhancements.mc.thePlayer || SweatsEnhancements.mc.gameSettings.thirdPersonView != 0) return;
            if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_AIR
                    && event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return;
            int type = type(SweatsEnhancements.mc.thePlayer.getHeldItem(), SweatsEnhancements.mc.thePlayer.isSneaking());
            if (!allowed(type)) return;
            int duration = timeFor(type);
            if (duration == 0) return;
            lastUse = System.currentTimeMillis();
            pendingUse = lastUse;
            activeType = type;
            activeDuration = duration;
            if (lastRenderTime == 0) lastRenderTime = lastUse;
            if (view == null || world != SweatsEnhancements.mc.theWorld) {
                view = new Vector3f((float) SweatsEnhancements.mc.thePlayer.posX,
                        (float) SweatsEnhancements.mc.thePlayer.posY, (float) SweatsEnhancements.mc.thePlayer.posZ);
                world = SweatsEnhancements.mc.theWorld;
            }
        } catch (Throwable t) { System.out.println("[SweatsEnhancements] smooth teleport use failed: " + t); }
    }

    /** Vanilla already moved the player; only add visual animation time for a matching use. */
    public static void confirmed(double ox, double oy, double oz, double nx, double ny, double nz) {
        if (!allowed(activeType) || view == null || SweatsEnhancements.mc == null || world != SweatsEnhancements.mc.theWorld
                || SweatsEnhancements.mc.gameSettings.thirdPersonView != 0) return;
        long now = System.currentTimeMillis();
        double distanceSq = (nx-ox)*(nx-ox) + (ny-oy)*(ny-oy) + (nz-oz)*(nz-oz);
        if (pendingUse == 0 || now < pendingUse || now - pendingUse > USE_WINDOW_MS
                || distanceSq < 0.01 || distanceSq > 1600) return;
        animationLeft = Math.min(activeDuration * 2, animationLeft + Math.min(300, activeDuration));
        pendingUse = 0;
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        try {
            if (!allowed(activeType) || SweatsEnhancements.mc == null || SweatsEnhancements.mc.thePlayer == null || SweatsEnhancements.mc.theWorld != world) {
                clear(); return;
            }
            long now = System.currentTimeMillis();
            long delta = now - lastRenderTime;
            lastRenderTime = now;
            if (delta <= 0 || view == null) return;
            animationLeft = Math.max(0, Math.min(animationLeft, activeDuration * 2));
            if (now - lastUse > USE_WINDOW_MS && animationLeft == 0) {
                view = null; lastUse = pendingUse = 0; return;
            }
            if (animationLeft > 0) {
                int step = (int) Math.min(delta, animationLeft);
                float fraction = (float) step / animationLeft;
                view.x -= (view.x - (float) SweatsEnhancements.mc.thePlayer.posX) * fraction;
                view.y -= (view.y - (float) SweatsEnhancements.mc.thePlayer.posY) * fraction;
                view.z -= (view.z - (float) SweatsEnhancements.mc.thePlayer.posZ) * fraction;
                if (SweatsEnhancements.mc.theWorld.getBlockState(new BlockPos(view.x, view.y, view.z))
                        .getBlock().getMaterial() != Material.air) view.y = (float) Math.ceil(view.y);
                animationLeft -= step;
            } else {
                view.set((float) SweatsEnhancements.mc.thePlayer.posX, (float) SweatsEnhancements.mc.thePlayer.posY,
                        (float) SweatsEnhancements.mc.thePlayer.posZ);
            }
        } catch (Throwable t) { clear(); System.out.println("[SweatsEnhancements] smooth teleport render failed: " + t); }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event != null && event.world != null && event.world.isRemote) clear();
    }

    public static void clear() {
        view = null; animationLeft = activeType = activeDuration = 0;
        lastUse = pendingUse = lastRenderTime = 0; world = null;
    }

    public static double ease(double t) {
        t = Math.max(0, Math.min(1, t));
        return t*t*(3 - 2*t);
    }
}
