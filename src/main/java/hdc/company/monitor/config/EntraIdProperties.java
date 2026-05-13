package hdc.company.monitor.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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

    public String getClientId() {
        return clientId;
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
        String message = String.format("Azure EntraID config: clientIdSet=%s, tenantIdSet=%s, redirectUri=%s",
                hasText(clientId), hasText(tenantId), redirectUri);
        System.out.println(message);
        logger.info(message);
        if (!isConfigured()) {
            String warning = "Azure EntraID is not fully configured. Required environment variables: ENTRA_ID_CLIENT_ID, ENTRA_ID_CLIENT_SECRET, ENTRA_ID_TENANT_ID";
            System.out.println(warning);
            logger.warn(warning);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
