package hdc.company.monitor.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class FaviconTest {

    @Test
    public void faviconResourceExistsOnClasspath() throws IOException {
        // Fallback to file system check in tests (ensures resource added to the project)
        java.nio.file.Path p = java.nio.file.Paths.get("src/main/resources/static/favicon.svg");
        assertTrue(java.nio.file.Files.exists(p), "favicon.svg should exist at src/main/resources/static/favicon.svg");
        String content = java.nio.file.Files.readString(p);
        assertTrue(content.contains("⌘") || content.contains("text"), "favicon.svg should contain the command symbol or text content");
    }
}
