package hdc.company.monitor.controller;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.core.Authentication;
import hdc.company.monitor.model.ProductItem;
import hdc.company.monitor.model.ServiceResponse;
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
public class ManageController {

    private static final Logger logger = LoggerFactory.getLogger(ManageController.class);

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
            logger.error("Failed to retrieve or exchange tokens for Manage page", ex);
        }

        ServiceResponse<ProductItem> productResponse = statusService.getProductList(apiAccessToken);
        model.addAttribute("productList", productResponse.getData());

        if (initialAccessToken != null && apiAccessToken == null) {
            model.addAttribute("productFetchError", "Failed to obtain OBO token for PHP API. Ensure Admin Consent is granted for scope: " + oboService.getPhpApiScope());
        } else {
            model.addAttribute("productFetchError", productResponse.getErrorMessage());
        }
        model.addAttribute("statusConfigMissing", statusService.getMissingConfiguration());
        if (principal instanceof OidcUser oidcUser) {
            model.addAttribute("userName", oidcUser.getFullName());
            model.addAttribute("userEmail", oidcUser.getEmail());
        }
        return "manage";
    }

    @PostMapping("/manage/create")
    public String createProduct(@RequestParam("productName") String productName,
                                @RequestParam("productDescription") String productDescription,
                                @RequestParam(value = "testCase", required = false) String testCase,
                                Principal principal, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        String apiAccessToken = getApiAccessToken(principal, request);
        ProductItem newItem = new ProductItem(productName, productDescription, testCase);
        ServiceResponse<Void> response = statusService.createProduct(newItem, apiAccessToken);
        if (response.hasError()) {
            redirectAttributes.addFlashAttribute("errorMessage", response.getErrorMessage());
        } else {
            redirectAttributes.addFlashAttribute("successMessage", response.getMessage());
        }
        return "redirect:/manage";
    }

    @PostMapping("/manage/update")
    public String updateProduct(@RequestParam("productName") String productName,
                                @RequestParam("productDescription") String productDescription,
                                @RequestParam(value = "testCase", required = false) String testCase,
                                Principal principal, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        String apiAccessToken = getApiAccessToken(principal, request);
        ServiceResponse<Void> response = statusService.updateProduct(productName, productDescription, testCase, apiAccessToken);
        if (response.hasError()) {
            redirectAttributes.addFlashAttribute("errorMessage", response.getErrorMessage());
        } else {
            redirectAttributes.addFlashAttribute("successMessage", response.getMessage());
        }
        return "redirect:/manage";
    }

    @PostMapping("/manage/delete")
    public String deleteProduct(@RequestParam("productName") String productName,
                                Principal principal, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        String apiAccessToken = getApiAccessToken(principal, request);
        ServiceResponse<Void> response = statusService.deleteProduct(productName, apiAccessToken);
        if (response.hasError()) {
            redirectAttributes.addFlashAttribute("errorMessage", response.getErrorMessage());
        } else {
            redirectAttributes.addFlashAttribute("successMessage", response.getMessage());
        }
        return "redirect:/manage";
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
