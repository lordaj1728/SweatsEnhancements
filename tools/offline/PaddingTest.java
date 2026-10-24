import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sweatsenhancements.feature.CustomTabList;

/**
 * The padding rows, in the shapes the server sends them, and the Footer switch.
 *
 * The row drawn as `CLICK` has its *entry name* as its text and no display name at all, so the
 * client falls back to the name - and the server decorates it. A non-breaking space draws exactly
 * like a space and is not removed by `trim()`, which is how a row that every drop had been written
 * for survived all of them. These are the shapes; every one of them must come out of the reader,
 * and out of the packed columns, with nothing left.
 */
public class PaddingTest {

    static Object drawn(String label, List<String> rows) throws Exception {
        System.out.println("---- " + label);
        Method read = CustomTabList.class.getDeclaredMethod("readColumns", List.class, java.util.Set.class);
        read.setAccessible(true);
        Object result = read.invoke(null, rows, new java.util.HashSet<String>());
        Method combine = CustomTabList.class.getDeclaredMethod("combine", List.class, int.class);
        combine.setAccessible(true);
        List<?> columns = (List<?>) combine.invoke(null, result, 22);

        int padding = 0;
        for (Object column : columns) {
            Field titleField = column.getClass().getDeclaredField("title");
            titleField.setAccessible(true);
            Field linesField = column.getClass().getDeclaredField("lines");
            linesField.setAccessible(true);
            String title = (String) titleField.get(column);
            List<String> lines = new ArrayList<String>();
            for (Object o : (List<?>) linesField.get(column)) lines.add((String) o);
            if (!title.isEmpty()) System.out.println("  title: " + title.replace('\u00a7', '&'));
            for (String line : lines) {
                System.out.println("  line : " + line.replace('\u00a7', '&'));
                if (isPadding(line)) padding++;
            }
            if (isPadding(title)) padding++;
        }
        System.out.println("  -> " + padding + " padding row(s) still drawn");
        return null;
    }

    /** The same rule the reader uses, written out so the test does not lean on the code it checks. */
    static boolean isPadding(String text) {
        if (text == null) return false;
        StringBuilder letters = new StringBuilder();
        String plain = text.replaceAll("\u00a7.", "");
        for (int i = 0; i < plain.length(); i++) {
            char c = plain.charAt(i);
            if (Character.isLetter(c)) letters.append(Character.toLowerCase(c));
        }
        String word = letters.toString();
        return "click".equals(word) || "clicktoplay".equals(word);
    }

    public static void main(String[] args) throws Exception {
        drawn("the row as the dump carries it, and the shapes the server decorates it with",
                Arrays.asList(
                        "\u00a7a\u00a7lPlayers \u00a7f(3)",
                        "\u00a78[\u00a7b216\u00a78] \u00a7aaditbro \u00a7a\u2726",
                        "\u00a7e\u00a7lCLICK",
                        "\u00a7e\u00a7lCLICK\u00a0",
                        "\u00a7e\u00a7lCLICK\u200b",
                        "\u00a7e\u00a7lCLICK!",
                        "\u00a7e\u00a7lClick To Play",
                        "\u00a7fBank: \u00a76118M/0"));

        drawn("a column whose first entry is a padding row - the shape that became a heading",
                Arrays.asList(
                        "\u00a7e\u00a7lCLICK\u00a0",
                        "\u00a7e\u00a7lElection: \u00a7cOver!",
                        "\u00a7fWinner: \u00a7aPaul",
                        "\u00a7e\u00a7lCLICK\u00a0",
                        "\u00a7fYear: \u00a7d298"));
    }
}
