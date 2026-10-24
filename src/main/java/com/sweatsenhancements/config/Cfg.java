package com.sweatsenhancements.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * One properties file for everything. Every feature gets a key here and a row in the
 * config menu later; all of them default to off except the probe.
 *
 * Writes are atomic (tmp file then rename) - a half-written config is worse than none.
 */
public class Cfg {

    /** Config key for a feature toggle, e.g. CH030 -> features.CH030 */
    public static String key(String featureId) {
        return "features." + featureId;
    }

    public static final String PROBE_ENABLED   = "probe.enabled";
    public static final String PROBE_CHAT_LINES = "probe.chatLines";
    public static final String PROBE_ENTITY_RADIUS = "probe.entityRadius";
    public static final String HUD_WATERMARK   = "hud.watermark";

    private final File file;
    private final Properties props = new Properties();
    /** set when a value actually changed, cleared by a write out */
    private volatile boolean dirty;
    /** bumped on every change, so a reader can skip work when nothing moved */
    private volatile int version;
    /** when the file was last written, so the flush can be throttled */
    private volatile long lastWriteMs;

    public Cfg(File configDir) {
        File dir = new File(configDir, "sweatsenhancements");
        if (!dir.exists()) dir.mkdirs();
        this.file = new File(dir, "sweatsenhancements.properties");
    }

    public File file() {
        return file;
    }

    public synchronized void load() {
        props.clear();
        defaults();
        if (file.exists()) {
            Reader r = null;
            try {
                r = new InputStreamReader(new FileInputStream(file), "UTF-8");
                props.load(r);
            } catch (Throwable t) {
                System.out.println("[SweatsEnhancements] could not read config, using defaults: " + t);
            } finally {
                close(r);
            }
        }
        save();
    }

    private void defaults() {
        props.setProperty(PROBE_ENABLED, "true");
        props.setProperty(PROBE_CHAT_LINES, "300");
        props.setProperty(PROBE_ENTITY_RADIUS, "24");
        props.setProperty(HUD_WATERMARK, "true");
    }

    /** True when something has changed since the last write out. */
    public boolean isDirty() {
        return dirty;
    }

    /** How many times the settings have changed, so callers can tell whether anything moved. */
    public int version() {
        return version;
    }

    /** How many times the file has actually been written, for /se panels and the offline tests. */
    public static volatile int writes = 0;

    /**
     * Writes the file out, but only when something has changed, and not more often than asked for.
     *
     * Called from the client tick with a second or two between writes: a setting flipped by a
     * command or in the menu is on disk almost at once, and a slider being dragged is written once
     * when the dragging stops - not once per frame, which is what it used to do.
     */
    public void flushIfDirty(long sinceMs) {
        if (!dirty) return;
        long now = System.currentTimeMillis();
        if (now - lastWriteMs < sinceMs) return;
        save();
    }

    public synchronized void save() {
        Writer w = null;
        try {
            File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
            w = new OutputStreamWriter(new FileOutputStream(tmp), "UTF-8");
            props.store(w, "SweatsEnhancements - one key per feature");
            w.close();
            w = null;
            if (file.exists() && !file.delete()) {
                System.out.println("[SweatsEnhancements] could not replace old config");
            }
            if (!tmp.renameTo(file)) {
                System.out.println("[SweatsEnhancements] could not move new config into place");
            }
            dirty = false;
            lastWriteMs = System.currentTimeMillis();
            writes++;
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] could not save config: " + t);
        } finally {
            close(w);
        }
    }

    public synchronized boolean getBool(String key, boolean def) {
        try {
            String v = props.getProperty(key);
            return v == null ? def : Boolean.parseBoolean(v.trim());
        } catch (Throwable t) {
            return def;
        }
    }

    public synchronized int getInt(String key, int def) {
        try {
            String v = props.getProperty(key);
            return v == null ? def : Integer.parseInt(v.trim());
        } catch (Throwable t) {
            return def;
        }
    }

    public synchronized String get(String key, String def) {
        return props.getProperty(key, def);
    }

    /**
     * Writes a value, and writes nothing else.
     *
     * This used to save the file on every call, which is what made the config menu hitch: the menu
     * mirrors every option through here, so one pass was hundreds of disk writes and the menu ran a
     * pass twice a second. A value that has not changed is not even touched, and a value that has
     * is written out once by the next flush - see flushIfDirty.
     */
    public synchronized void set(String key, String value) {
        if (value == null) return;
        String old = props.getProperty(key);
        if (value.equals(old)) return;
        props.setProperty(key, value);
        dirty = true;
        version++;
    }

    public synchronized void setBool(String key, boolean value) {
        set(key, Boolean.toString(value));
    }

    public synchronized void setInt(String key, int value) {
        set(key, Integer.toString(value));
    }

    /** True when the key has ever been written, which is how a moved HUD element is told apart. */
    public synchronized boolean contains(String key) {
        return props.getProperty(key) != null;
    }

    /** Forgets a key, so the value goes back to its default. Used to un-move HUD elements. */
    public synchronized void remove(String key) {
        if (props.remove(key) != null) {
            dirty = true;
            version++;
        }
    }

    /** Every known key, sorted, for /se config list. */
    public synchronized List<String> keys() {
        List<String> out = new ArrayList<String>();
        for (Object o : props.keySet()) out.add(String.valueOf(o));
        Collections.sort(out);
        return out;
    }

    private static void close(java.io.Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Throwable ignored) {
            }
        }
    }
}
