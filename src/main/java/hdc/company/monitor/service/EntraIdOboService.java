package hdc.company.monitor.service;

import hdc.company.monitor.config.EntraIdProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;

@Service
public class EntraIdOboService {

    private static final Logger logger = LoggerFactory.getLogger(EntraIdOboService.class);

    private final EntraIdProperties properties;
    private final RestTemplate restTemplate;

    public EntraIdOboService(EntraIdProperties properties) {
        this.properties = properties;
        this.restTemplate = new RestTemplate();
        this.restTemplate.getMessageConverters().add(0, new MappingJackson2HttpMessageConverter());
    }

    /**
     * Performs the Microsoft EntraID On-Behalf-Of (OBO) flow to exchange a user access token
     * for a token specific to the configured PHP API scope.
     *
     * @param userAccessToken The access token obtained during user login (assertion).
     * @return The new API-specific access token, or null if exchange fails.
     */
    public String getOboToken(String userAccessToken) {
        if (!properties.isConfigured() || userAccessToken == null || userAccessToken.isBlank()) {
            logger.warn("Entra ID is not configured or user access token is missing, cannot perform OBO flow");
            return null;
        }

        String tenantId = properties.getTenantId();
        String url = String.format("https://login.microsoftonline.com/%s/oauth2/v2.0/token", tenantId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
        map.add("client_id", properties.getClientId());
        map.add("client_secret", properties.getClientSecret());
        map.add("assertion", userAccessToken);
        map.add("scope", properties.getPhpApiScope());
        map.add("requested_token_use", "on_behalf_of");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        try {
            logger.info("Requesting OBO token for scope: {} from {}", properties.getPhpApiScope(), url);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String oboToken = (String) response.getBody().get("access_token");
                if (oboToken != null) {
                    logger.info("Successfully obtained OBO token");
                    return oboToken;
                }
                logger.warn("OBO response successful but access_token is missing. Body: {}", response.getBody());
            } else {
                logger.error("Failed to get OBO token. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            logger.error("Error during OBO token exchange with {}: {}", url, e.getMessage(), e);
        }

        return null;
    }
}
