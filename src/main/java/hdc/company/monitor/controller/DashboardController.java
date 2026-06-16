package hdc.company.monitor.controller;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.core.Authentication;
import hdc.company.monitor.model.ServiceResponse;
import hdc.company.monitor.model.SystemStatusItem;
import hdc.company.monitor.service.EntraIdOboService;
import hdc.company.monitor.service.StatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletRequest;

import java.security.Principal;
import java.util.Properties;

@Controller
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

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
        String apiAccessToken = getApiAccessToken(principal, request);

        ServiceResponse<SystemStatusItem> statusResponse = statusService.getSystemStatusList(apiAccessToken);
        model.addAttribute("systemStatusList", statusResponse.getData());

        if (principal instanceof Authentication && apiAccessToken == null) {
            model.addAttribute("statusFetchError", "Failed to obtain OBO token for PHP API. Ensure Admin Consent is granted for scope: " + oboService.getPhpApiScope());
        } else {
            model.addAttribute("statusFetchError", statusResponse.getErrorMessage());
        }
        model.addAttribute("statusConfigMissing", statusService.getMissingConfiguration());
        if (principal instanceof OidcUser oidcUser) {
            model.addAttribute("userName", oidcUser.getFullName());
            model.addAttribute("userEmail", oidcUser.getEmail());
        }
        return "dashboard";
    }

    @PostMapping("/dashboard/delete")
    public String deleteStatus(@RequestParam("systemId") String systemId,
                               @RequestParam(value = "testCase", required = false) String testCase,
                               Principal principal, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        String apiAccessToken = getApiAccessToken(principal, request);
        ServiceResponse<Void> response = statusService.deleteSystemStatus(systemId, testCase, apiAccessToken);
        if (response.hasError()) {
            redirectAttributes.addFlashAttribute("errorMessage", response.getErrorMessage());
        } else {
            redirectAttributes.addFlashAttribute("successMessage", response.getMessage());
        }
        return "redirect:/dashboard";
    }

    private String getApiAccessToken(Principal principal, HttpServletRequest request) {
        try {
            if (principal instanceof Authentication authentication) {
                OAuth2AuthorizedClient authorizedClient = authorizedClientRepository.loadAuthorizedClient(
                    "entra", authentication, request);
                if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
                    return oboService.getOboToken(authorizedClient.getAccessToken().getTokenValue());
                }
            }
        } catch (Exception ex) {
            logger.error("Failed to retrieve or exchange tokens", ex);
        }
        return null;
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
