package hdc.company.monitor.controller;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import hdc.company.monitor.service.StatusService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.http.HttpServletRequest;

import java.security.Principal;
import java.util.Properties;

@Controller
public class DashboardController {

    private final StatusService statusService;
    private final OAuth2AuthorizedClientRepository authorizedClientRepository;

    public DashboardController(StatusService statusService, OAuth2AuthorizedClientRepository authorizedClientRepository) {
        this.statusService = statusService;
        this.authorizedClientRepository = authorizedClientRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, HttpServletRequest request, Model model) {
        model.addAttribute("version", getAppVersion());
<<<<<<< HEAD
        
=======

>>>>>>> main
        String accessToken = null;
        try {
            if (principal != null) {
                OAuth2AuthorizedClient authorizedClient = authorizedClientRepository.loadAuthorizedClient(
                    "entra", SecurityContextHolder.getContext().getAuthentication(), request);
                if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
                    accessToken = authorizedClient.getAccessToken().getTokenValue();
                }
            }
        } catch (Exception ex) {
            // Token extraction failed, will proceed without token
        }
<<<<<<< HEAD
        
=======

>>>>>>> main
        model.addAttribute("systemStatusList", statusService.getSystemStatusList(accessToken));
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