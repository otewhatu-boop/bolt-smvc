package hdc.company.monitor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hdc.company.monitor.model.ProductItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class ProductServiceTest {

    private MockEnvironment environment;
    private StatusService statusService;

    @Mock
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        environment = new MockEnvironment();
    }

    @Test
    void getProductList_whenSuccessful_returnsList() {
        String baseUrl = "http://localhost/api";
        environment.setProperty(StatusService.STATUS_API_URL_ENV, baseUrl);
        statusService = new StatusService(environment, restTemplate);

        String expectedUrl = baseUrl + "/" + StatusService.PRODUCT_API_PATH;
        ObjectMapper mapper = new ObjectMapper();
        JsonNode itemsNode = mapper.createArrayNode()
            .add(mapper.createObjectNode().put("product_name", "prod1").put("product_description", "desc1").put("test_case", "tc1"))
            .add(mapper.createObjectNode().put("product_name", "prod2").put("product_description", "desc2").put("test_case", "tc2"));

        when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.GET), any(), eq(JsonNode.class)))
            .thenReturn(new ResponseEntity<>(itemsNode, HttpStatus.OK));

        List<ProductItem> result = statusService.getProductList("test-token");

        assertEquals(2, result.size());
        assertEquals("prod1", result.get(0).getProductName());
        assertEquals("prod2", result.get(1).getProductName());
    }

    @Test
    void getProductList_whenExceptionOccurs_inProd_returnsGenericError() {
        String baseUrl = "http://localhost/api";
        environment.setProperty(StatusService.STATUS_API_URL_ENV, baseUrl);
        environment.setProperty(StatusService.APP_ENV_ENV, "production");
        statusService = new StatusService(environment, restTemplate);

        String expectedUrl = baseUrl + "/" + StatusService.PRODUCT_API_PATH;
        when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.GET), any(), eq(JsonNode.class)))
            .thenThrow(new RuntimeException("Connection refused"));

        List<ProductItem> result = statusService.getProductList("test-token");

        assertTrue(result.isEmpty());
        assertTrue(statusService.hasError());
        assertEquals("An error occurred while fetching products. Please contact support.", statusService.getErrorMessage());
    }

    @Test
    void getProductList_whenWrappedSuccessful_returnsList() {
        String baseUrl = "http://localhost/api";
        environment.setProperty(StatusService.STATUS_API_URL_ENV, baseUrl);
        statusService = new StatusService(environment, restTemplate);

        String expectedUrl = baseUrl + "/" + StatusService.PRODUCT_API_PATH;
        ObjectMapper mapper = new ObjectMapper();
        JsonNode wrappedNode = mapper.createObjectNode()
            .set("response_body", mapper.createArrayNode()
                .add(mapper.createObjectNode().put("product_name", "prod1").put("product_description", "desc1").put("test_case", "tc1")));

        when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.GET), any(), eq(JsonNode.class)))
            .thenReturn(new ResponseEntity<>(wrappedNode, HttpStatus.OK));

        List<ProductItem> result = statusService.getProductList("test-token");

        assertEquals(1, result.size());
        assertEquals("prod1", result.get(0).getProductName());
    }
}
