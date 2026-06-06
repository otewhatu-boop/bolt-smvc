package hdc.company.monitor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.lang.reflect.Array;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Controller
public class ProfileController {

    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);

    private final OAuth2AuthorizedClientRepository authorizedClientRepository;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public ProfileController(OAuth2AuthorizedClientRepository authorizedClientRepository) {
        this.authorizedClientRepository = authorizedClientRepository;
    }

    @GetMapping("/profile")
    public String profile(Authentication authentication, HttpServletRequest request, Model model) {
        model.addAttribute("version", getAppVersion());

        if (authentication instanceof OAuth2AuthenticationToken oauth2Authentication
                && oauth2Authentication.getPrincipal() instanceof OidcUser oidcUser) {
            model.addAttribute("user", oidcUser);
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
            logger.warn("Unable to render token claims as JSON; falling back to string values", ex);
            try {
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(normalizeClaimValue(value));
            } catch (Exception fallbackEx) {
                logger.error("Fallback JSON conversion failed", fallbackEx);
                String message = fallbackEx.getMessage();
                return String.format("{\"error\":\"unable to render token claims\", \"message\": \"%s\"}",
                        message == null ? "unknown" : message.replace("\"", "\\\""));
            }
        }
    }

    private Object normalizeClaimValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                normalized.put(String.valueOf(entry.getKey()), normalizeClaimValue(entry.getValue()));
            }
            return normalized;
        }
        if (value instanceof Collection<?> collection) {
            List<Object> normalized = new ArrayList<>();
            for (Object item : collection) {
                normalized.add(normalizeClaimValue(item));
            }
            return normalized;
        }
        if (value.getClass().isArray()) {
            List<Object> normalized = new ArrayList<>();
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                normalized.add(normalizeClaimValue(Array.get(value, i)));
            }
            return normalized;
        }
        if (value instanceof Date date) {
            return Instant.ofEpochMilli(date.getTime()).toString();
        }
        return value;
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