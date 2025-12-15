package hdc.company.monitor.controller;

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