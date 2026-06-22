package hdc.company.monitor.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class EntraIdPropertiesTest {

    private EntraIdProperties properties;

    @BeforeEach
    void setUp() {
        properties = new EntraIdProperties();
    }

    @Test
    void isConfigured_returnsFalse_whenAllPropertiesEmpty() {
        assertFalse(properties.isConfigured());
    }

    @Test
    void isConfigured_returnsTrue_whenAllThreePropertiesSet() {
        set("clientId", "client-123");
        set("clientSecret", "secret-456");
        set("tenantId", "tenant-789");

        assertTrue(properties.isConfigured());
    }

    @Test
    void isConfigured_returnsFalse_whenClientIdBlank() {
        set("clientSecret", "secret-456");
        set("tenantId", "tenant-789");

        assertFalse(properties.isConfigured());
    }

    @Test
    void isConfigured_returnsFalse_whenClientSecretBlank() {
        set("clientId", "client-123");
        set("tenantId", "tenant-789");

        assertFalse(properties.isConfigured());
    }

    @Test
    void isConfigured_returnsFalse_whenTenantIdBlank() {
        set("clientId", "client-123");
        set("clientSecret", "secret-456");

        assertFalse(properties.isConfigured());
    }

    @Test
    void isConfigured_returnsFalse_whenValuesAreWhitespaceOnly() {
        set("clientId", "   ");
        set("clientSecret", "\t");
        set("tenantId", " \n ");

        assertFalse(properties.isConfigured());
    }

    @Test
    void logConfigurationStatus_runsWithoutError_whenNotConfigured() {
        // Should not throw and should complete (exercises the @PostConstruct path).
        assertDoesNotThrow(() -> properties.logConfigurationStatus());
    }

    @Test
    void logConfigurationStatus_runsWithoutError_whenConfigured() {
        set("clientId", "client-123");
        set("clientSecret", "shhh");
        set("tenantId", "tenant-789");

        assertDoesNotThrow(() -> properties.logConfigurationStatus());
    }

    @Test
    void getters_returnValues_populatedByReflection() {
        set("clientId", "client-123");
        set("clientSecret", "secret-456");
        set("tenantId", "tenant-789");
        set("redirectUri", "http://localhost/redirect");
        set("phpApiScope", "api://php/test");

        assertEquals("client-123", properties.getClientId());
        assertEquals("secret-456", properties.getClientSecret());
        assertEquals("tenant-789", properties.getTenantId());
        assertEquals("http://localhost/redirect", properties.getRedirectUri());
        assertEquals("api://php/test", properties.getPhpApiScope());
    }

    private void set(String field, Object value) {
        ReflectionTestUtils.setField(properties, field, value);
    }
}
