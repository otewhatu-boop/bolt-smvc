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
public class ManageController {

    private final StatusService statusService;
    private final EntraIdOboService oboService;
    private final OAuth2AuthorizedClientRepository authorizedClientRepository;

    public ManageController(StatusService statusService,
                               EntraIdOboService oboService,
                               OAuth2AuthorizedClientRepository authorizedClientRepository) {
        this.statusService = statusService;
        this.oboService = oboService;
        this.authorizedClientRepository = authorizedClientRepository;
    }

    @GetMapping("/manage")
    public String manage(Principal principal, HttpServletRequest request, Model model) {
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
            // Token extraction or OBO exchange failed
        }

        model.addAttribute("productList", statusService.getProductList(apiAccessToken));

        if (initialAccessToken != null && apiAccessToken == null) {
            model.addAttribute("productFetchError", "Failed to obtain OBO token for PHP API. Ensure Admin Consent is granted for scope: " + oboService.getPhpApiScope());
        } else {
            model.addAttribute("productFetchError", statusService.getErrorMessage());
        }
        model.addAttribute("statusConfigMissing", statusService.getMissingConfiguration());
        if (principal instanceof OidcUser oidcUser) {
            model.addAttribute("userName", oidcUser.getFullName());
            model.addAttribute("userEmail", oidcUser.getEmail());
        }
        return "manage";
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
