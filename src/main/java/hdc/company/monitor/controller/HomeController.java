package hdc.company.monitor.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.ServletContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Controller
public class HomeController {
    
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("message", "Monitor Centre");
        model.addAttribute("version", getAppVersion());
        // if the user is authenticated, show their username
        // SecurityContextHolder can be used in views directly via Thymeleaf extras if configured,
        // but for simplicity we'll leave principal handling to the view layer for now.
        return "home";
    }

    @PostMapping("/login")
    public String login() {
        // Spring Security handles authentication; POST to /login will be processed by the filter
        return "redirect:/";
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("version", getAppVersion());
        return "login";
    }

    @GetMapping(value = "/favicon.svg", produces = "image/svg+xml")
    public ResponseEntity<Resource> favicon() {
        Resource res = new ClassPathResource("/static/favicon.svg");
        if (!res.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("image/svg+xml")).body(res);
    }

    @Autowired
    private ServletContext servletContext;

    @GetMapping(value = "/favicon.ico", produces = "image/x-icon")
    public ResponseEntity<?> faviconIco() throws IOException {
        // Try classpath first
        Resource res = new ClassPathResource("/static/favicon.ico");
        if (res.exists()) {
            return ResponseEntity.ok().contentType(MediaType.parseMediaType("image/x-icon")).body(res);
        }
        // Fallback to servlet context resource (webapp root)
        try (var is = servletContext.getResourceAsStream("/favicon.ico")) {
            if (is == null) {
                return ResponseEntity.notFound().build();
            }
            InputStreamResource ir = new InputStreamResource(is);
            return ResponseEntity.ok().contentType(MediaType.parseMediaType("image/x-icon")).body(ir);
        }
    }

    @GetMapping(value = "/favicon-32x32.png", produces = "image/png")
    public ResponseEntity<?> favicon32() throws IOException {
        Resource res = new ClassPathResource("/static/favicon-32x32.png");
        if (res.exists()) {
            return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(res);
        }
        try (var is = servletContext.getResourceAsStream("/favicon-32x32.png")) {
            if (is == null) {
                return ResponseEntity.notFound().build();
            }
            InputStreamResource ir = new InputStreamResource(is);
            return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(ir);
        }
    }

    private String getAppVersion() {
        Properties properties = new Properties();
        try (InputStream input = getClass().getResourceAsStream("/version.properties")) {
            if (input == null) {
                return "x.x.x";
            }
            properties.load(input);
            return properties.getProperty("version", "0.0.0");
        } catch (IOException e) {
            e.printStackTrace();
            return "Error retrieving version";
        }
    }
}