package com.sweatsenhancements.chat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

/**
 * What the mod tells the player directly, and the log behind it.
 *
 * Written because a feature that quietly does nothing and a feature that is switched off look
 * exactly the same from the outside. A render hook that throws used to be swallowed on purpose -
 * which is right for the frame, and wrong for finding out why nothing is on the screen. So the
 * first failure is now said out loud, in chat and in the log with a stack trace, and the count
 * keeps going up where /se can read it.
 *
 * "once" is per key per session: one line the first time something happens, nothing after that.
 */
public final class Report {

    private static final Set<String> SAID = new HashSet<String>();
    private static final Map<String, Long> LAST = new HashMap<String, Long>();
    private static final Map<String, Integer> COUNT = new HashMap<String, Integer>();

    /** how long the same failure stays quiet, in milliseconds */
    private static final long QUIET_MS = 5000L;

    private Report() {
    }

    /** One chat line, if there is a player to receive it. Never throws. */
    public static void say(String text) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.thePlayer != null) {
                mc.thePlayer.addChatComponentMessage(new ChatComponentText(String.valueOf(text)));
            }
        } catch (Throwable t) {
            // chat is a nicety, never a requirement
        }
    }

    /** Says something the first time this key is used, and nothing after that. */
    public static void once(String key, String text) {
        synchronized (SAID) {
            if (!SAID.add(key)) return;
        }
        say(text);
    }

    /** True when this key has already been said. */
    public static boolean said(String key) {
        synchronized (SAID) {
            return SAID.contains(key);
        }
    }

    /** Warn in yellow the first time, once per key. */
    public static void warnOnce(String key, String text) {
        once(key, EnumChatFormatting.YELLOW + "[SweatsEnhancements] " + EnumChatFormatting.GRAY + text);
    }

    /** Note in grey the first time, once per key. */
    public static void noteOnce(String key, String text) {
        once(key, EnumChatFormatting.GREEN + "[SweatsEnhancements] " + EnumChatFormatting.GRAY + text);
    }

    /** Something cannot be done: red, first time, and the reason in chat. */
    public static void blockedOnce(String key, String text) {
        once(key, EnumChatFormatting.RED + "[SweatsEnhancements] " + EnumChatFormatting.GRAY + text);
    }

    /**
     * Records a failure. The first one is printed with its stack trace and said in chat; after
     * that the same key only reaches the log once every five seconds, so a hook that fails sixty
     * times a second cannot fill the log up.
     */
    public static void fail(String key, String what, Throwable t) {
        int seen;
        synchronized (COUNT) {
            Integer was = COUNT.get(key);
            seen = (was == null ? 0 : was.intValue()) + 1;
            COUNT.put(key, Integer.valueOf(seen));
        }
        long now = System.currentTimeMillis();
        synchronized (LAST) {
            Long last = LAST.get(key);
            if (last != null && now - last.longValue() < QUIET_MS) return;
            LAST.put(key, Long.valueOf(now));
        }
        System.out.println("[SweatsEnhancements] " + what + " failed (" + seen + " times): " + t);
        t.printStackTrace();
        if (seen == 1) {
            blockedOnce("fail:" + key, what + " failed: " + t);
        }
    }

    /** How many times a key has failed since launch. */
    public static int failures(String key) {
        synchronized (COUNT) {
            Integer n = COUNT.get(key);
            return n == null ? 0 : n.intValue();
        }
    }

    /** Forgets the once-per-session lines and the failure counts. */
    public static void forget() {
        synchronized (SAID) {
            SAID.clear();
        }
        synchronized (LAST) {
            LAST.clear();
        }
        synchronized (COUNT) {
            COUNT.clear();
        }
    }
}
