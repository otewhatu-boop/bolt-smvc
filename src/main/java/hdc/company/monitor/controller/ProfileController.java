package hdc.company.monitor.controller;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.Properties;

@Controller
public class ProfileController {

    @GetMapping("/profile")
    public String profile(Principal principal, Model model) {
        model.addAttribute("version", getAppVersion());
        if (principal instanceof OidcUser oidcUser) {
            model.addAttribute("user", oidcUser);
            model.addAttribute("claims", oidcUser.getClaims());
        }
        return "profile";
    }

    private String getAppVersion() {
        try {
            Properties properties = new Properties();
            properties.load(getClass().getClassLoader().getResourceAsStream("version.properties"));
            return properties.getProperty("version", "0.0.0");
        } catch (Exception e) {
            return "0.0.0";
        }
    }
}