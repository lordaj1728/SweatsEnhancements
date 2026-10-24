import com.sweatsenhancements.config.LegacyConfigMigration;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class LegacyConfigMigrationTest {
    private static void ok(boolean b, String msg) { if (!b) throw new AssertionError(msg); }
    private static void put(File file, String value) throws Exception {
        file.getParentFile().mkdirs();
        Files.write(file.toPath(), value.getBytes(StandardCharsets.UTF_8));
    }
    private static String get(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
    public static void main(String[] args) throws Exception {
        File root = new File("cfg-migration");
        File old = new File(root, "fpe");
        File now = new File(root, "sweatsenhancements");
        File settings = new File(now, "sweatsenhancements.properties");
        put(new File(old, "fpe.properties"), "legacy-settings");
        put(new File(old, "protected-items.properties"), "uuid.0123456789abcdef=protected");
        put(new File(old, "chatfilters.txt"), "legacy-chatfilter");
        LegacyConfigMigration.run(root);
        ok(get(settings).equals("legacy-settings"), "settings migrated");
        ok(get(new File(now, "protected-items.properties")).contains("uuid.0123456789abcdef"), "safeguards migrated");
        ok(get(new File(now, "chatfilters.txt")).equals("legacy-chatfilter"), "chat filters migrated");
        put(settings, "new-settings");
        LegacyConfigMigration.run(root);
        ok(get(settings).equals("new-settings"), "newer settings not overwritten");
        ok(get(new File(old, "fpe.properties")).equals("legacy-settings"), "legacy file preserved");
        System.out.println("LegacyConfigMigrationTest OK");
    }
}
