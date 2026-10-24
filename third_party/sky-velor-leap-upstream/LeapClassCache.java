package com.arena.pekaclient.feature;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Short-lived, identity-keyed cache for Spirit Leap class observations.
 *
 * A missing scoreboard/tab update does not immediately erase a class, but a cache entry expires and is pruned
 * after its player leaves the current roster. The cache is owned by one LeapScreen, so it cannot leak across menus.
 */
public final class LeapClassCache {

    /** A class observation is trusted for at most this long without a confirming source. */
    public static final long DEFAULT_TTL_MS = 30000L;
    /** Brief grace for container updates that temporarily omit a teammate before a roster replacement arrives. */
    public static final long PARTY_ABSENCE_GRACE_MS = 4000L;

    private final long ttlMs;
    private final long absenceGraceMs;
    private final Map<String, Entry> entries = new HashMap<String, Entry>();
    private final Map<String, Long> absentSince = new HashMap<String, Long>();

    public LeapClassCache() {
        this(DEFAULT_TTL_MS, PARTY_ABSENCE_GRACE_MS);
    }

    public LeapClassCache(long ttlMs, long absenceGraceMs) {
        this.ttlMs = Math.max(0L, ttlMs);
        this.absenceGraceMs = Math.max(0L, absenceGraceMs);
    }

    /**
     * Applies a fresh observation, or returns a recent cached value if the current source is temporarily missing.
     * A null/empty identity is never cached.
     */
    public synchronized Resolution resolve(String identityKey, LeapClass detected, String source, long nowMs) {
        if (identityKey == null || identityKey.trim().isEmpty()) {
            return new Resolution(null, "none", false, "no stable player identity");
        }
        Entry previous = entries.get(identityKey);
        if (detected != null && previous != null && nowMs - previous.confirmedAtMs > ttlMs) {
            entries.remove(identityKey);
            absentSince.remove(identityKey);
            previous = null;
        }
        if (detected != null) {
            String normalizedSource = source == null || source.trim().isEmpty() ? "unknown" : source;
            if (previous != null && sourcePriority(normalizedSource) < sourcePriority(previous.source)) {
                if (previous.value != detected) {
                    return new Resolution(previous.value, "cache (" + previous.source + ")", false,
                            "conflicting lower-priority " + normalizedSource + " class " + detected.label()
                                    + " ignored in favour of recent " + previous.source + " class "
                                    + previous.value.label());
                }
                // A lower-priority source agrees, so it can refresh the short TTL without downgrading provenance.
                entries.put(identityKey, new Entry(previous.value, previous.source, nowMs));
                absentSince.remove(identityKey);
                return new Resolution(previous.value, "cache (" + previous.source + ")", false,
                        "same class confirmed by lower-priority " + normalizedSource + "; retained "
                                + previous.source + " provenance");
            }
            boolean valueChanged = previous == null || previous.value != detected;
            boolean sourceChanged = previous != null && !normalizedSource.equals(previous.source);
            entries.put(identityKey, new Entry(detected, normalizedSource, nowMs));
            absentSince.remove(identityKey);
            String reason;
            if (previous == null) {
                reason = "fresh " + normalizedSource + " detection";
            } else if (valueChanged) {
                reason = "replaced " + previous.value.label() + " with " + detected.label()
                        + " from " + normalizedSource;
            } else if (sourceChanged) {
                reason = "same class confirmed by " + normalizedSource + " instead of " + previous.source;
            } else {
                reason = "class reconfirmed by " + normalizedSource;
            }
            return new Resolution(detected, normalizedSource, valueChanged || sourceChanged, reason);
        }

        if (previous == null) {
            return new Resolution(null, "none", false, "no class source available");
        }
        long ageMs = nowMs - previous.confirmedAtMs;
        if (ageMs <= ttlMs) {
            return new Resolution(previous.value, "cache (" + previous.source + ")", false,
                    "temporarily retained; current scoreboard/tab/lore data is unavailable");
        }
        entries.remove(identityKey);
        absentSince.remove(identityKey);
        return new Resolution(null, "none", true,
                "cleared after cached class expired without a fresh observation (" + ageMs + " ms)");
    }

    /**
     * Marks cached identities absent from the current menu and removes them after the party-change grace period.
     * Identity keys for the remaining cards are left intact, even if their class source is briefly missing.
     */
    public synchronized void retainCurrentPlayers(Collection<String> identityKeys, long nowMs) {
        Set<String> current = new HashSet<String>();
        if (identityKeys != null) {
            for (String key : identityKeys) {
                if (key != null && !key.trim().isEmpty()) {
                    current.add(key);
                }
            }
        }

        Iterator<Map.Entry<String, Entry>> iterator = entries.entrySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next().getKey();
            if (current.contains(key)) {
                absentSince.remove(key);
                continue;
            }
            Long firstAbsent = absentSince.get(key);
            if (firstAbsent == null) {
                absentSince.put(key, nowMs);
            } else if (nowMs - firstAbsent >= absenceGraceMs) {
                iterator.remove();
                absentSince.remove(key);
            }
        }
    }

    private static int sourcePriority(String source) {
        if ("scoreboard".equals(source)) {
            return 3;
        }
        if ("tab".equals(source)) {
            return 2;
        }
        if ("lore".equals(source)) {
            return 1;
        }
        return 0;
    }

    /** Number of currently cached player identities; exposed for pure-logic tests. */
    public synchronized int size() {
        return entries.size();
    }

    private static final class Entry {
        final LeapClass value;
        final String source;
        final long confirmedAtMs;

        Entry(LeapClass value, String source, long confirmedAtMs) {
            this.value = value;
            this.source = source;
            this.confirmedAtMs = confirmedAtMs;
        }
    }

    /** Result of applying a current observation to the cache. */
    public static final class Resolution {
        public final LeapClass value;
        public final String source;
        public final boolean changed;
        public final String reason;

        private Resolution(LeapClass value, String source, boolean changed, String reason) {
            this.value = value;
            this.source = source;
            this.changed = changed;
            this.reason = reason;
        }
    }
}
