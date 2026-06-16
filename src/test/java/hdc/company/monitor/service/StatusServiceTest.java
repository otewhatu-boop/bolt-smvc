package hdc.company.monitor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hdc.company.monitor.model.ServiceResponse;
import hdc.company.monitor.model.SystemStatusItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
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
        assertTrue(statusService.getSystemStatusList().getData().isEmpty());
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
    void buildUrl_derivesCorrectUrls_fromRoot() {
        environment.setProperty(StatusService.STATUS_API_URL_ENV, "http://localhost/api/");
        statusService = new StatusService(environment, restTemplate);

        // This is tricky as statusApiUrl is private. We'll test via the effect on getSystemStatusList.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.createArrayNode();
        when(restTemplate.exchange(eq("http://localhost/api/status.php"), eq(HttpMethod.GET), any(), eq(JsonNode.class)))
            .thenReturn(new ResponseEntity<>(node, HttpStatus.OK));

        statusService.getSystemStatusList("token");
        // Verify is implicit by the mock matching the expected URL
    }

    @Test
    void buildUrl_derivesCorrectUrls_fromFullEndpoint() {
        environment.setProperty(StatusService.STATUS_API_URL_ENV, "http://localhost/api/status.php");
        statusService = new StatusService(environment, restTemplate);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.createArrayNode();
        // Base should be http://localhost/api/
        // Product should be http://localhost/api/product
        when(restTemplate.exchange(eq("http://localhost/api/product"), eq(HttpMethod.GET), any(), eq(JsonNode.class)))
            .thenReturn(new ResponseEntity<>(node, HttpStatus.OK));

        statusService.getProductList("token");
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

        ServiceResponse<SystemStatusItem> result = statusService.getSystemStatusList("test-token");

        assertEquals(2, result.getData().size());
        assertEquals("sys1", result.getData().get(0).getSystemId());
        assertEquals("sys2", result.getData().get(1).getSystemId());
        assertFalse(result.hasError());
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

        ServiceResponse<SystemStatusItem> result = statusService.getSystemStatusList("test-token");

        assertEquals(1, result.getData().size());
        assertEquals("sys1", result.getData().get(0).getSystemId());
        assertEquals("tc1", result.getData().get(0).getTestCase());
        assertEquals("pass", result.getData().get(0).getStatus());
        assertEquals("2023-01-01 00:00:00", result.getData().get(0).getUpdatedAt());
        assertFalse(result.hasError());
    }

    @Test
    void getSystemStatusList_whenApiReturnsError_returnsEmptyListAndSetsError() {
        String baseUrl = "http://localhost/api";
        environment.setProperty(StatusService.STATUS_API_URL_ENV, baseUrl);
        statusService = new StatusService(environment, restTemplate);

        String expectedUrl = baseUrl + "/" + StatusService.STATUS_API_PATH;
        when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.GET), any(), eq(JsonNode.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.NOT_FOUND));

        ServiceResponse<SystemStatusItem> result = statusService.getSystemStatusList("test-token");

        assertTrue(result.getData().isEmpty());
        assertTrue(result.hasError());
        assertTrue(result.getErrorMessage().contains("404"));
    }

    @Test
    void getSystemStatusList_whenExceptionOccurs_inDev_returnsDetailedError() {
        String baseUrl = "http://localhost/api";
        environment.setProperty(StatusService.STATUS_API_URL_ENV, baseUrl);
        environment.setProperty(StatusService.APP_ENV_ENV, "development");
        statusService = new StatusService(environment, restTemplate);

        String expectedUrl = baseUrl + "/" + StatusService.STATUS_API_PATH;
        when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.GET), any(), eq(JsonNode.class)))
            .thenThrow(new RuntimeException("Connection refused"));

        ServiceResponse<SystemStatusItem> result = statusService.getSystemStatusList("test-token");

        assertTrue(result.getData().isEmpty());
        assertTrue(result.hasError());
        assertTrue(result.getErrorMessage().contains("Connection refused"));
    }

    @Test
    void getSystemStatusList_whenExceptionOccurs_inProd_returnsGenericError() {
        String baseUrl = "http://localhost/api";
        environment.setProperty(StatusService.STATUS_API_URL_ENV, baseUrl);
        environment.setProperty(StatusService.APP_ENV_ENV, "production");
        statusService = new StatusService(environment, restTemplate);

        String expectedUrl = baseUrl + "/" + StatusService.STATUS_API_PATH;
        when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.GET), any(), eq(JsonNode.class)))
            .thenThrow(new RuntimeException("Connection refused"));

        ServiceResponse<SystemStatusItem> result = statusService.getSystemStatusList("test-token");

        assertTrue(result.getData().isEmpty());
        assertTrue(result.hasError());
        assertEquals("An error occurred while fetching system status. Please contact support.", result.getErrorMessage());
    }

    @Test
    void getSystemStatusList_whenUnauthorizedWithReason_returnsEmptyListAndSetsFriendlyError() {
        String baseUrl = "http://localhost/api";
        environment.setProperty(StatusService.STATUS_API_URL_ENV, baseUrl);
        statusService = new StatusService(environment, restTemplate);

        String expectedUrl = baseUrl + "/" + StatusService.STATUS_API_PATH;
        String responseBody = "{\"error\":\"Unauthorized\",\"reason\":\"JWT audience mismatch\"}";

        HttpClientErrorException.Unauthorized ex = (HttpClientErrorException.Unauthorized) HttpClientErrorException.create(
            HttpStatus.UNAUTHORIZED, "Unauthorized", HttpHeaders.EMPTY, responseBody.getBytes(), StandardCharsets.UTF_8);

        when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.GET), any(), eq(JsonNode.class)))
            .thenThrow(ex);

        ServiceResponse<SystemStatusItem> result = statusService.getSystemStatusList("test-token");

        assertTrue(result.getData().isEmpty());
        assertTrue(result.hasError());
        assertEquals("Access Denied: You do not have permission to view system status. Reason: JWT audience mismatch", result.getErrorMessage());
    }

    @Test
    void deleteSystemStatus_whenSuccessful_returnsSuccess() {
        String baseUrl = "http://localhost/api";
        environment.setProperty(StatusService.STATUS_API_URL_ENV, baseUrl);
        statusService = new StatusService(environment, restTemplate);

        String expectedUrl = baseUrl + "/" + StatusService.STATUS_API_PATH + "?system_id=sys1&test_case=tc1";

        ObjectMapper mapper = new ObjectMapper();
        JsonNode responseNode = mapper.createObjectNode().put("message", "System status record deleted successfully");

        when(restTemplate.exchange(eq(java.net.URI.create(expectedUrl)), eq(HttpMethod.DELETE), any(), eq(JsonNode.class)))
            .thenReturn(new ResponseEntity<>(responseNode, HttpStatus.OK));

        ServiceResponse<Void> result = statusService.deleteSystemStatus("sys1", "tc1", "token");

        assertFalse(result.hasError());
        assertEquals("System status record deleted successfully", result.getMessage());
    }

    @Test
    void deleteSystemStatus_withoutTestCase_returnsSuccess() {
        String baseUrl = "http://localhost/api";
        environment.setProperty(StatusService.STATUS_API_URL_ENV, baseUrl);
        statusService = new StatusService(environment, restTemplate);

        String expectedUrl = baseUrl + "/" + StatusService.STATUS_API_PATH + "?system_id=sys1";

        ObjectMapper mapper = new ObjectMapper();
        JsonNode responseNode = mapper.createObjectNode().put("message", "System status record deleted successfully");

        when(restTemplate.exchange(eq(java.net.URI.create(expectedUrl)), eq(HttpMethod.DELETE), any(), eq(JsonNode.class)))
            .thenReturn(new ResponseEntity<>(responseNode, HttpStatus.OK));

        ServiceResponse<Void> result = statusService.deleteSystemStatus("sys1", null, "token");

        assertFalse(result.hasError());
        assertEquals("System status record deleted successfully", result.getMessage());
    }
}
