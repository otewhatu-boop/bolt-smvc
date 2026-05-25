package hdc.company.monitor.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringJUnitConfig
@ContextConfiguration(classes = {SecurityConfig.class, EntraIdProperties.class})
@ActiveProfiles("prod")
@TestPropertySource(properties = {
        "entra.client.id=test-client",
        "entra.client.secret=test-secret",
        "entra.tenant.id=test-tenant",
        "entra.redirect.uri=http://localhost/login/oauth2/code/entra"
})
public class SecurityConfigTest {

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Test
    void shouldCreateEntraClientRegistration() {
        assertNotNull(clientRegistrationRepository, "ClientRegistrationRepository should be created");

        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId("entra");
        assertNotNull(registration, "Entra registration should be available");
        assertEquals("test-client", registration.getClientId());
        assertEquals("http://localhost/login/oauth2/code/entra", registration.getRedirectUri());
    }

    @Test
    void shouldCreateSecurityFilterChain() {
        assertNotNull(securityFilterChain, "SecurityFilterChain should be configured");
    }
}
