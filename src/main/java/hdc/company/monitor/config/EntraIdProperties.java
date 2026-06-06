package hdc.company.monitor.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class EntraIdProperties {

    private static final Logger logger = LoggerFactory.getLogger(EntraIdProperties.class);

    @Value("${entra.client.id:}")
    private String clientId;

    @Value("${entra.client.secret:}")
    private String clientSecret;

    @Value("${entra.tenant.id:}")
    private String tenantId;

    @Value("${entra.redirect.uri:http://localhost:8080/smvc/login/oauth2/code/entra}")
    private String redirectUri;

    @Value("${entra.api.scope:6b7af4a5-5be2-4f0b-91c4-b71cb9c04129/.default}")
    private String apiScope;

    public String getClientId() {
        return clientId;
    }

    public String getApiScope() {
        return apiScope;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public boolean isConfigured() {
        return hasText(clientId)
                && hasText(clientSecret)
                && hasText(tenantId);
    }

    @PostConstruct
    public void logConfigurationStatus() {
        String clientSecretHash = hasText(clientSecret) ? sha256(clientSecret) : "NOT SET";
        String message = String.format(
            "Azure EntraID config: clientId=%s, tenantId=%s, clientSecretSHA256=%s, redirectUri=%s, isConfigured=%s",
            clientId, tenantId, clientSecretHash, redirectUri, isConfigured());
        System.out.println(message);
        logger.info(message);
        if (!isConfigured()) {
            String warning = String.format(
                "Azure EntraID is not fully configured. Required environment variables: ENTRA_ID_CLIENT_ID=%s, ENTRA_ID_CLIENT_SECRET=%s, ENTRA_ID_TENANT_ID=%s",
                hasText(clientId) ? "SET" : "NOT SET",
                hasText(clientSecret) ? "SET" : "NOT SET",
                hasText(tenantId) ? "SET" : "NOT SET");
            System.out.println(warning);
            logger.warn(warning);
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-256 algorithm not available", e);
            return "ERROR";
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
