package hdc.company.monitor.util;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class GenerateFaviconTest {

    @Test
    public void generatorProducesFiles() throws Exception {
        Path out = Path.of("src/main/webapp");
        FaviconGenerator.generateAll(out);

        assertTrue(Files.exists(out.resolve("favicon-32x32.png")), "32x32 PNG should exist");
        assertTrue(Files.exists(out.resolve("favicon-16x16.png")), "16x16 PNG should exist");
        assertTrue(Files.exists(out.resolve("favicon.ico")), "ICO should exist");
    }
}
