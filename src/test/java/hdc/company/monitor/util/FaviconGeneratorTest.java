package hdc.company.monitor.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class FaviconGeneratorTest {

    @Test
    void shouldGenerateFaviconFiles(@TempDir Path tempDir) throws Exception {
        System.setProperty("java.awt.headless", "true");
        FaviconGenerator.generateAll(tempDir);

        Path ico = tempDir.resolve("favicon.ico");
        Path png16 = tempDir.resolve("favicon-16x16.png");
        Path png32 = tempDir.resolve("favicon-32x32.png");

        assertTrue(Files.exists(ico), "Favicon ICO should be generated");
        assertTrue(Files.exists(png16), "16x16 PNG should be generated");
        assertTrue(Files.exists(png32), "32x32 PNG should be generated");
        assertTrue(Files.size(ico) > 0, "Favicon ICO file should not be empty");
        assertTrue(Files.size(png16) > 0, "16x16 PNG file should not be empty");
        assertTrue(Files.size(png32) > 0, "32x32 PNG file should not be empty");
    }
}
