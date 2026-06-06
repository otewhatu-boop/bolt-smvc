package hdc.company.monitor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

@Controller
public class ProfileController {

    private final OAuth2AuthorizedClientRepository authorizedClientRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProfileController(OAuth2AuthorizedClientRepository authorizedClientRepository) {
        this.authorizedClientRepository = authorizedClientRepository;
    }

    @GetMapping("/profile")
    public String profile(Authentication authentication, HttpServletRequest request, Model model) {
        model.addAttribute("version", getAppVersion());

        if (authentication instanceof OAuth2AuthenticationToken oauth2Authentication
                && oauth2Authentication.getPrincipal() instanceof OidcUser oidcUser) {
            model.addAttribute("user", oidcUser);
            model.addAttribute("claims", oidcUser.getClaims());
            model.addAttribute("idTokenJson", prettyPrintJson(oidcUser.getIdToken().getClaims()));
            model.addAttribute("idTokenValue", oidcUser.getIdToken().getTokenValue());

            OAuth2AuthorizedClient authorizedClient = authorizedClientRepository.loadAuthorizedClient(
                    oauth2Authentication.getAuthorizedClientRegistrationId(), oauth2Authentication, request);
            if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
                OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
                model.addAttribute("accessTokenJson", introspectToken(accessToken));
                model.addAttribute("accessTokenValue", accessToken.getTokenValue());
                model.addAttribute("accessTokenScopes", accessToken.getScopes());
            }
        }

        return "profile";
    }

    private String introspectToken(OAuth2AccessToken accessToken) {
        String tokenValue = accessToken.getTokenValue();
        String jwtClaims = parseJwtClaims(tokenValue);
        if (jwtClaims != null) {
            return jwtClaims;
        }

        Map<String, Object> tokenInfo = new LinkedHashMap<>();
        tokenInfo.put("tokenType", accessToken.getTokenType().getValue());
        tokenInfo.put("issuedAt", accessToken.getIssuedAt());
        tokenInfo.put("expiresAt", accessToken.getExpiresAt());
        tokenInfo.put("scopes", accessToken.getScopes());
        tokenInfo.put("tokenValue", tokenValue);
        return prettyPrintJson(tokenInfo);
    }

    private String parseJwtClaims(String token) {
        try {
            JWT jwt = JWTParser.parse(token);
            return prettyPrintJson(jwt.getJWTClaimsSet().getClaims());
        } catch (Exception ex) {
            return null;
        }
    }

    private String prettyPrintJson(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception ex) {
            return "{\"error\":\"unable to render token claims\"}";
        }
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