/**
 * One row of a `/sweatsenhancements dump` tab-list entry, read the way the client reads it.
 *
 * The dump writes each entry as "<key>  |  <text>  |  ping N  |  mode SURVIVAL". The text is not
 * safe to split on '|': the server builds the election's vote bars out of them, so a row like
 * "Scorpius: |||||||||| (98.3%)" has eleven more bars inside what is really one field. Cutting at
 * the last field instead gives the row the client would draw.
 *
 * The client draws an entry's display name, or the entry's own name when the server set none -
 * which is how this server sends its `CLICK` padding rows.
 */
public class DumpRows {

    public static boolean looksLikeRow(String line) {
        return line != null && line.indexOf('|') >= 0;
    }

    /** {name, display} or null when the line is not an entry. */
    public static String[] of(String line) {
        int tail = line.lastIndexOf("|  ping ");
        String body = tail < 0 ? line : line.substring(0, tail);
        int bar = body.indexOf('|');
        if (bar < 0) return null;
        String name = body.substring(0, bar).trim();
        String display = body.substring(bar + 1).trim();
        return new String[] { name, display.isEmpty() ? name : display };
    }
}
