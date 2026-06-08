package hdc.company.monitor.controller;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.core.Authentication;
import hdc.company.monitor.service.EntraIdOboService;
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
    private final EntraIdOboService oboService;
    private final OAuth2AuthorizedClientRepository authorizedClientRepository;

    public DashboardController(StatusService statusService,
                               EntraIdOboService oboService,
                               OAuth2AuthorizedClientRepository authorizedClientRepository) {
        this.statusService = statusService;
        this.oboService = oboService;
        this.authorizedClientRepository = authorizedClientRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, HttpServletRequest request, Model model) {
        model.addAttribute("version", getAppVersion());
        String initialAccessToken = null;
        String apiAccessToken = null;
        try {
            if (principal instanceof Authentication authentication) {
                OAuth2AuthorizedClient authorizedClient = authorizedClientRepository.loadAuthorizedClient(
                    "entra", authentication, request);
                if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
                    initialAccessToken = authorizedClient.getAccessToken().getTokenValue();
                    apiAccessToken = oboService.getOboToken(initialAccessToken);
                }
            }
        } catch (Exception ex) {
            // Token extraction or OBO exchange failed, will proceed
        }

        // If OBO exchange failed or returned null, fall back to initial access token
        // to ensure an Authorization header is sent if possible.
        String tokenToUse = (apiAccessToken != null) ? apiAccessToken : initialAccessToken;

        model.addAttribute("systemStatusList", statusService.getSystemStatusList(tokenToUse));
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
