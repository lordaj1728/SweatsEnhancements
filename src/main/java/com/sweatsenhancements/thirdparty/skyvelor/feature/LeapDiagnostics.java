/* Adapted from Sky VeLor c1b56668155e31fca7a8d3c82058496d0e77d97e; MIT licence.
 * Package integration for SweatsEnhancements. Upstream originals and licence are distributed with source. */
package com.sweatsenhancements.thirdparty.skyvelor.feature;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Opt-in, transition-only Spirit Leap diagnostics; disabled unless -Dsweatsenhancements.debug.leap=true is set. */
public final class LeapDiagnostics {

    private static final boolean ENABLED = Boolean.getBoolean("sweatsenhancements.debug.leap");
    private static final Logger LOG = LogManager.getLogger("SweatsEnhancements");
    private static final Map<String, String> LAST_STATE = new ConcurrentHashMap<String, String>();

    private LeapDiagnostics() {
    }

    /** Writes one diagnostic only when the reported state changes. Never writes to disk or runs per frame by default. */
    public static void record(String identity, String playerName, String normalizedScoreboard,
                               LeapClass detected, LeapClass resolved, String source, String reason) {
        if (!ENABLED) {
            return;
        }
        String key = identity == null || identity.trim().isEmpty() ? "unmatched:" + playerName : identity;
        String marker = detected == null ? "-" : String.valueOf(detected.marker());
        String detectedText = detected == null ? "Unknown" : detected.label();
        String resolvedText = resolved == null ? "Unknown" : resolved.label();
        String normalized = LeapText.normalize(normalizedScoreboard);
        String signature = marker + '|' + detectedText + '|' + resolvedText + '|'
                + safe(source) + '|' + safe(reason) + '|' + normalized;
        String previous = LAST_STATE.put(key, signature);
        if (signature.equals(previous)) {
            return;
        }
        LOG.info("Spirit Leap diagnostic: identity='" + key + "' player='" + safe(playerName)
                + "' normalizedScoreboard='" + normalized + "' classMarker=" + marker
                + " detected='" + detectedText + "' resolved='" + resolvedText
                + "' source='" + safe(source) + "' reason='" + safe(reason) + "'");
    }

    private static String safe(String value) {
        return value == null || value.trim().isEmpty() ? "-" : LeapText.normalize(value);
    }
}
