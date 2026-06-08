package hdc.company.monitor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import hdc.company.monitor.service.EntraIdOboService;
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
    private final EntraIdOboService oboService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProfileController(OAuth2AuthorizedClientRepository authorizedClientRepository,
                             EntraIdOboService oboService) {
        this.authorizedClientRepository = authorizedClientRepository;
        this.oboService = oboService;
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @GetMapping("/profile")
    public String profile(Authentication authentication, HttpServletRequest request, Model model) {
        model.addAttribute("version", getAppVersion());

        if (authentication instanceof OAuth2AuthenticationToken oauth2Authentication
                && oauth2Authentication.getPrincipal() instanceof OidcUser oidcUser) {
            model.addAttribute("user", oidcUser);
            model.addAttribute("claims", normalizeClaims(oidcUser.getClaims()));
            model.addAttribute("idTokenJson", prettyPrintJson(normalizeClaims(oidcUser.getIdToken().getClaims())));
            model.addAttribute("idTokenValue", oidcUser.getIdToken().getTokenValue());

            OAuth2AuthorizedClient authorizedClient = authorizedClientRepository.loadAuthorizedClient(
                    oauth2Authentication.getAuthorizedClientRegistrationId(), oauth2Authentication, request);
            if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
                OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
                model.addAttribute("accessTokenJson", introspectToken(accessToken));
                model.addAttribute("accessTokenValue", accessToken.getTokenValue());
                model.addAttribute("accessTokenScopes", accessToken.getScopes());

                // Perform OBO exchange for the profile screen introspection
                String oboTokenValue = oboService.getOboToken(accessToken.getTokenValue());
                if (oboTokenValue != null) {
                    model.addAttribute("oboTokenValue", oboTokenValue);
                    String oboClaimsJson = parseJwtClaims(oboTokenValue);
                    if (oboClaimsJson != null) {
                        model.addAttribute("oboTokenJson", oboClaimsJson);
                    } else {
                        model.addAttribute("oboTokenJson", "{\"info\": \"Token is not a parseable JWT\"}");
                    }
                }
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
            return prettyPrintJson(normalizeClaims(jwt.getJWTClaimsSet().getClaims()));
        } catch (Exception ex) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> normalizeClaims(Map<String, Object> claims) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (claims == null) return out;
        for (Map.Entry<String, Object> e : claims.entrySet()) {
            out.put(e.getKey(), normalizeClaimValue(e.getValue()));
        }
        return out;
    }

    private Object normalizeClaimValue(Object value) {
        if (value == null) return null;
        if (value instanceof java.util.Date) {
            return ((java.util.Date) value).toInstant().toString();
        }
        if (value instanceof java.time.Instant) {
            return value.toString();
        }
        if (value instanceof Map) {
            // recursive normalize
            return normalizeClaims((Map<String, Object>) value);
        }
        if (value instanceof Iterable) {
            java.util.List<Object> list = new java.util.ArrayList<>();
            for (Object o : (Iterable<?>) value) list.add(normalizeClaimValue(o));
            return list;
        }
        return value;
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
