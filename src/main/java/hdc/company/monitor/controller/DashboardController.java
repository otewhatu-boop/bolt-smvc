package hdc.company.monitor.controller;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import hdc.company.monitor.service.StatusService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.Properties;

@Controller
public class DashboardController {

    private final StatusService statusService;

    public DashboardController(StatusService statusService) {
        this.statusService = statusService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        model.addAttribute("version", getAppVersion());
        model.addAttribute("systemStatusList", statusService.getSystemStatusList());
        model.addAttribute("statusConfigMissing", statusService.getMissingConfiguration());
        model.addAttribute("statusFetchError", statusService.getErrorMessage());
        if (principal instanceof OidcUser oidcUser) {
            model.addAttribute("userName", oidcUser.getFullName());
            model.addAttribute("userEmail", oidcUser.getEmail());
        }
        return "dashboard";
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