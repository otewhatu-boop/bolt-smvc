package hdc.company.monitor.service;

import hdc.company.monitor.model.SystemStatusItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StatusServiceTest {

    private MockEnvironment environment;
    private StatusService statusService;

    @BeforeEach
    void setUp() {
        environment = new MockEnvironment();
    }

    @Test
    void whenNotConfigured_isConfiguredReturnsFalse() {
        statusService = new StatusService(environment);
        assertFalse(statusService.isConfigured());
        assertTrue(statusService.getSystemStatusList().isEmpty());
        assertEquals(List.of(StatusService.STATUS_API_URL_ENV), statusService.getMissingConfiguration());
    }

    @Test
    void whenEnvConfigured_isConfiguredReturnsTrue() {
        environment.setProperty(StatusService.STATUS_API_URL_ENV, "http://localhost/api");
        statusService = new StatusService(environment);
        assertTrue(statusService.isConfigured());
        assertTrue(statusService.getMissingConfiguration().isEmpty());
    }

    @Test
    void whenPropertyConfigured_isConfiguredReturnsTrue() {
        environment.setProperty(StatusService.STATUS_API_URL_PROPERTY, "http://localhost/api");
        statusService = new StatusService(environment);
        assertTrue(statusService.isConfigured());
    }

    @Test
    void getErrorMessage_returnsLastErrorMessage() {
        statusService = new StatusService(environment);
        assertFalse(statusService.hasError());
        assertNull(statusService.getErrorMessage());
    }
}
