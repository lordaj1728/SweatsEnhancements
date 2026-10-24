package com.sweatsenhancements.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/** Non-destructive, per-file migration from the previous config namespace.
 * Never removes or overwrites an old or new user file. Safe to run on every launch. */
public final class LegacyConfigMigration {
    private LegacyConfigMigration() {}

    public static void run(File configDir) {
        File old = new File(configDir, "fpe");
        File destination = new File(configDir, "sweatsenhancements");
        if (!old.isDirectory()) return;
        try {
            copyIfMissing(new File(old, "fpe.properties"),
                    new File(destination, "sweatsenhancements.properties"));
            copyIfMissing(new File(old, "protected-items.properties"),
                    new File(destination, "protected-items.properties"));
            copyIfMissing(new File(old, "chatfilters.txt"),
                    new File(destination, "chatfilters.txt"));
        } catch (IOException e) {
            System.err.println("[SweatsEnhancements] legacy settings migration failed: " + e);
        }
    }

    private static void copyIfMissing(File source, File target) throws IOException {
        if (!source.isFile() || target.exists()) return;
        if (!target.getParentFile().exists() && !target.getParentFile().mkdirs())
            throw new IOException("Could not create " + target.getParentFile());
        Files.copy(source.toPath(), target.toPath());
    }
}
