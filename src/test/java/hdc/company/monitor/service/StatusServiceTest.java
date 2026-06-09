package hdc.company.monitor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hdc.company.monitor.model.SystemStatusItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatusServiceTest {

    private MockEnvironment environment;
    private StatusService statusService;

    @Mock
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        environment = new MockEnvironment();
    }

    @Test
    void whenNotConfigured_isConfiguredReturnsFalse() {
        statusService = new StatusService(environment, restTemplate);
        assertFalse(statusService.isConfigured());
        assertTrue(statusService.getSystemStatusList().isEmpty());
        assertEquals(List.of(StatusService.STATUS_API_URL_ENV), statusService.getMissingConfiguration());
    }

    @Test
    void whenEnvConfigured_isConfiguredReturnsTrue() {
        environment.setProperty(StatusService.STATUS_API_URL_ENV, "http://localhost/api");
        statusService = new StatusService(environment, restTemplate);
        assertTrue(statusService.isConfigured());
        assertTrue(statusService.getMissingConfiguration().isEmpty());
    }

    @Test
    void getErrorMessage_returnsLastErrorMessage() {
        statusService = new StatusService(environment, restTemplate);
        assertFalse(statusService.hasError());
        assertNull(statusService.getErrorMessage());
    }

    @Test
    void getSystemStatusList_whenSuccessful_returnsList() {
        String baseUrl = "http://localhost/api";
        environment.setProperty(StatusService.STATUS_API_URL_ENV, baseUrl);
        statusService = new StatusService(environment, restTemplate);

        String expectedUrl = baseUrl + "/" + StatusService.STATUS_API_PATH;
        ObjectMapper mapper = new ObjectMapper();
        JsonNode itemsNode = mapper.createArrayNode()
            .add(mapper.createObjectNode().put("system_id", "sys1").put("status", "UP").put("updated_at", "2023-01-01T00:00:00Z"))
            .add(mapper.createObjectNode().put("system_id", "sys2").put("status", "DOWN").put("updated_at", "2023-01-01T00:00:01Z"));

        when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.GET), any(), eq(JsonNode.class)))
            .thenReturn(new ResponseEntity<>(itemsNode, HttpStatus.OK));

        List<SystemStatusItem> result = statusService.getSystemStatusList("test-token");

        assertEquals(2, result.size());
        assertEquals("sys1", result.get(0).getSystemId());
        assertEquals("sys2", result.get(1).getSystemId());
        assertFalse(statusService.hasError());
    }

    @Test
    void getSystemStatusList_whenWrappedSuccessful_returnsList() {
        String baseUrl = "http://localhost/api";
        environment.setProperty(StatusService.STATUS_API_URL_ENV, baseUrl);
        statusService = new StatusService(environment, restTemplate);

        String expectedUrl = baseUrl + "/" + StatusService.STATUS_API_PATH;
        ObjectMapper mapper = new ObjectMapper();
        JsonNode wrappedNode = mapper.createObjectNode()
            .set("response_body", mapper.createArrayNode()
                .add(mapper.createObjectNode().put("system_id", "sys1").put("test_case", "tc1").put("status", "pass").put("timestamp", "2023-01-01 00:00:00")));

        when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.GET), any(), eq(JsonNode.class)))
            .thenReturn(new ResponseEntity<>(wrappedNode, HttpStatus.OK));

        List<SystemStatusItem> result = statusService.getSystemStatusList("test-token");

        assertEquals(1, result.size());
        assertEquals("sys1", result.get(0).getSystemId());
        assertEquals("tc1", result.get(0).getTestCase());
        assertEquals("pass", result.get(0).getStatus());
        assertEquals("2023-01-01 00:00:00", result.get(0).getUpdatedAt());
        assertFalse(statusService.hasError());
    }

    @Test
    void getSystemStatusList_whenApiReturnsError_returnsEmptyListAndSetsError() {
        String baseUrl = "http://localhost/api";
        environment.setProperty(StatusService.STATUS_API_URL_ENV, baseUrl);
        statusService = new StatusService(environment, restTemplate);

        String expectedUrl = baseUrl + "/" + StatusService.STATUS_API_PATH;
        when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.GET), any(), eq(JsonNode.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.NOT_FOUND));

        List<SystemStatusItem> result = statusService.getSystemStatusList("test-token");

        assertTrue(result.isEmpty());
        assertTrue(statusService.hasError());
        assertTrue(statusService.getErrorMessage().contains("404"));
    }

    @Test
    void getSystemStatusList_whenExceptionOccurs_returnsEmptyListAndSetsError() {
        String baseUrl = "http://localhost/api";
        environment.setProperty(StatusService.STATUS_API_URL_ENV, baseUrl);
        statusService = new StatusService(environment, restTemplate);

        String expectedUrl = baseUrl + "/" + StatusService.STATUS_API_PATH;
        when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.GET), any(), eq(JsonNode.class)))
            .thenThrow(new RuntimeException("Connection refused"));

        List<SystemStatusItem> result = statusService.getSystemStatusList("test-token");

        assertTrue(result.isEmpty());
        assertTrue(statusService.hasError());
        assertTrue(statusService.getErrorMessage().contains("Connection refused"));
    }
}
