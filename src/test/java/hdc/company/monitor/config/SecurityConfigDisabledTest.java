package hdc.company.monitor.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringJUnitConfig
@ContextConfiguration(classes = {SecurityConfig.class, EntraIdProperties.class})
@ActiveProfiles("prod")
@TestPropertySource(properties = {
        "entra.client.id=",
        "entra.client.secret=",
        "entra.tenant.id="
})
public class SecurityConfigDisabledTest {

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Test
    void shouldCreateDummyClientRegistrationRepositoryWhenEntraIdIsMissing() {
        assertNotNull(clientRegistrationRepository, "ClientRegistrationRepository should still be created");
        var registration = clientRegistrationRepository.findByRegistrationId("entra");
        assertNotNull(registration, "A dummy Entra registration should be available when EntraID is not configured");
        assertEquals("dummy-client-id", registration.getClientId(), "Dummy registration should have placeholder client ID");
    }

    @Test
    void shouldCreateSecurityFilterChainWhenEntraIdIsMissing() {
        assertNotNull(securityFilterChain, "SecurityFilterChain should still be configured when EntraID is disabled");
    }
}
