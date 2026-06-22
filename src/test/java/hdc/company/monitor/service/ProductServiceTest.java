package hdc.company.monitor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hdc.company.monitor.model.ProductItem;
import hdc.company.monitor.model.ServiceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    void getProductList_whenObjectMapSuccessful_returnsList() {
        String baseUrl = "http://localhost/api";
        environment.setProperty(StatusService.STATUS_API_URL_ENV, baseUrl);
        statusService = new StatusService(environment, restTemplate);

        String expectedUrl = baseUrl + "/" + StatusService.PRODUCT_API_PATH;
        ObjectMapper mapper = new ObjectMapper();
        com.fasterxml.jackson.databind.node.ObjectNode mapNode = mapper.createObjectNode();
        mapNode.set("key1", mapper.createObjectNode().put("product_name", "prod1").put("product_description", "desc1"));
        mapNode.set("key2", mapper.createObjectNode().put("product_name", "prod2").put("product_description", "desc2"));

        when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.GET), any(), eq(JsonNode.class)))
            .thenReturn(new ResponseEntity<>(mapNode, HttpStatus.OK));

        ServiceResponse<ProductItem> result = statusService.getProductList("test-token");

        assertEquals(2, result.getData().size());
        // Since it's a map, order might not be guaranteed, but we check presence
        assertTrue(result.getData().stream().anyMatch(p -> "prod1".equals(p.getProductName())));
        assertTrue(result.getData().stream().anyMatch(p -> "prod2".equals(p.getProductName())));
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

        ServiceResponse<ProductItem> result = statusService.getProductList("test-token");

        assertEquals(2, result.getData().size());
        assertEquals("prod1", result.getData().get(0).getProductName());
        assertEquals("prod2", result.getData().get(1).getProductName());
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

        ServiceResponse<ProductItem> result = statusService.getProductList("test-token");

        assertTrue(result.getData().isEmpty());
        assertTrue(result.hasError());
        assertEquals("An error occurred while fetching products. Please contact support.", result.getErrorMessage());
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

        ServiceResponse<ProductItem> result = statusService.getProductList("test-token");

        assertEquals(1, result.getData().size());
        assertEquals("prod1", result.getData().get(0).getProductName());
    }

    @Test
    void createProduct_whenSuccessful_returnsSuccess() {
        String baseUrl = "http://localhost/api";
        environment.setProperty(StatusService.STATUS_API_URL_ENV, baseUrl);
        statusService = new StatusService(environment, restTemplate);

        ProductItem item = new ProductItem("newProd", "newDesc", "newTC");
        String expectedUrl = baseUrl + "/" + StatusService.PRODUCT_API_PATH;

        ObjectMapper mapper = new ObjectMapper();
        JsonNode responseNode = mapper.createObjectNode().put("message", "Product created successfully");

        when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.POST), any(), eq(JsonNode.class)))
            .thenReturn(new ResponseEntity<>(responseNode, HttpStatus.CREATED));

        ServiceResponse<Void> result = statusService.createProduct(item, "token");

        assertFalse(result.hasError());
        assertEquals("Product created successfully", result.getMessage());
    }

    @Test
    void updateProduct_whenSuccessful_returnsSuccess() {
        String baseUrl = "http://localhost/api";
        environment.setProperty(StatusService.STATUS_API_URL_ENV, baseUrl);
        statusService = new StatusService(environment, restTemplate);

        String expectedUrl = baseUrl + "/" + StatusService.PRODUCT_API_PATH + "?product_name={productName}";

        ObjectMapper mapper = new ObjectMapper();
        JsonNode responseNode = mapper.createObjectNode().put("message", "Product updated successfully");

        when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.PUT), any(), eq(JsonNode.class), eq("prod1")))
            .thenReturn(new ResponseEntity<>(responseNode, HttpStatus.OK));

        ServiceResponse<Void> result = statusService.updateProduct("prod1", "newDesc", "newTC", "token");

        assertFalse(result.hasError());
        assertEquals("Product updated successfully", result.getMessage());
    }

    @Test
    void deleteProduct_whenSuccessful_returnsSuccess() {
        String baseUrl = "http://localhost/api";
        environment.setProperty(StatusService.STATUS_API_URL_ENV, baseUrl);
        statusService = new StatusService(environment, restTemplate);

        String expectedUrl = baseUrl + "/" + StatusService.PRODUCT_API_PATH + "?product_name={productName}";

        ObjectMapper mapper = new ObjectMapper();
        JsonNode responseNode = mapper.createObjectNode().put("message", "Product deleted successfully");

        when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.DELETE), any(), eq(JsonNode.class), eq("prod1")))
            .thenReturn(new ResponseEntity<>(responseNode, HttpStatus.OK));

        ServiceResponse<Void> result = statusService.deleteProduct("prod1", "token");

        assertFalse(result.hasError());
        assertEquals("Product deleted successfully", result.getMessage());
    }

    @Test
    void getProductList_whenNotConfigured_returnsEmptySuccess() {
        statusService = new StatusService(environment, restTemplate);

        ServiceResponse<ProductItem> result = statusService.getProductList("token");

        assertFalse(result.hasError());
        assertTrue(result.getData().isEmpty());
    }

    @Test
    void getProductList_whenUnauthorized_returnsAccessDeniedError() {
        String baseUrl = "http://localhost/api";
        environment.setProperty(StatusService.STATUS_API_URL_ENV, baseUrl);
        statusService = new StatusService(environment, restTemplate);

        String expectedUrl = baseUrl + "/" + StatusService.PRODUCT_API_PATH;
        HttpClientErrorException.Unauthorized ex = (HttpClientErrorException.Unauthorized) HttpClientErrorException.create(
            HttpStatus.UNAUTHORIZED, "Unauthorized", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8);

        when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.GET), any(), eq(JsonNode.class)))
            .thenThrow(ex);

        ServiceResponse<ProductItem> result = statusService.getProductList("token");

        assertTrue(result.hasError());
        assertTrue(result.getErrorMessage().contains("Access Denied"));
    }

    @Test
    void getProductList_when2xxButBodyIsPrimitive_returnsUnexpectedFormatError() {
        String baseUrl = "http://localhost/api";
        environment.setProperty(StatusService.STATUS_API_URL_ENV, baseUrl);
        statusService = new StatusService(environment, restTemplate);

        String expectedUrl = baseUrl + "/" + StatusService.PRODUCT_API_PATH;
        ObjectMapper mapper = new ObjectMapper();
        JsonNode primitive = mapper.getNodeFactory().numberNode(42);

        when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.GET), any(), eq(JsonNode.class)))
            .thenReturn(new ResponseEntity<>(primitive, HttpStatus.OK));

        ServiceResponse<ProductItem> result = statusService.getProductList("token");

        assertTrue(result.hasError());
        assertEquals("Unexpected API response format", result.getErrorMessage());
    }

    @Test
    void getProductList_when2xxButBodyIsNull_returnsBackendStatusError() {
        String baseUrl = "http://localhost/api";
        environment.setProperty(StatusService.STATUS_API_URL_ENV, baseUrl);
        statusService = new StatusService(environment, restTemplate);

        String expectedUrl = baseUrl + "/" + StatusService.PRODUCT_API_PATH;
        when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.GET), any(), eq(JsonNode.class)))
            .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        ServiceResponse<ProductItem> result = statusService.getProductList("token");

        assertTrue(result.hasError());
        assertTrue(result.getErrorMessage().contains("200"));
    }

    @Test
    void getProductList_whenResponseIsSingleObjectWithProductName_returnsSingleItemList() {
        String baseUrl = "http://localhost/api";
        environment.setProperty(StatusService.STATUS_API_URL_ENV, baseUrl);
        statusService = new StatusService(environment, restTemplate);

        String expectedUrl = baseUrl + "/" + StatusService.PRODUCT_API_PATH;
        ObjectMapper mapper = new ObjectMapper();
        JsonNode singleObject = mapper.createObjectNode()
            .put("product_name", "solo")
            .put("product_description", "one")
            .put("test_case", "tc");

        when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.GET), any(), eq(JsonNode.class)))
            .thenReturn(new ResponseEntity<>(singleObject, HttpStatus.OK));

        ServiceResponse<ProductItem> result = statusService.getProductList("token");

        assertFalse(result.hasError());
        assertEquals(1, result.getData().size());
        assertEquals("solo", result.getData().get(0).getProductName());
    }

    @Test
    void getProductList_whenObjectMapContainsUnparseableField_skipsAndContinues() {
        String baseUrl = "http://localhost/api";
        environment.setProperty(StatusService.STATUS_API_URL_ENV, baseUrl);
        statusService = new StatusService(environment, restTemplate);

        String expectedUrl = baseUrl + "/" + StatusService.PRODUCT_API_PATH;
        ObjectMapper mapper = new ObjectMapper();
        com.fasterxml.jackson.databind.node.ObjectNode mapNode = mapper.createObjectNode();
        mapNode.set("valid", mapper.createObjectNode().put("product_name", "prodA").put("product_description", "dA"));
        mapNode.set("invalid", mapper.getNodeFactory().textNode("not-an-object"));

        when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.GET), any(), eq(JsonNode.class)))
            .thenReturn(new ResponseEntity<>(mapNode, HttpStatus.OK));

        ServiceResponse<ProductItem> result = statusService.getProductList("token");

        assertEquals(1, result.getData().size());
        assertEquals("prodA", result.getData().get(0).getProductName());
    }

    @Test
    void getProductList_whenExceptionOccurs_inDev_returnsDetailedError() {
        String baseUrl = "http://localhost/api";
        environment.setProperty(StatusService.STATUS_API_URL_ENV, baseUrl);
        environment.setProperty(StatusService.APP_ENV_ENV, "development");
        statusService = new StatusService(environment, restTemplate);

        String expectedUrl = baseUrl + "/" + StatusService.PRODUCT_API_PATH;
        when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.GET), any(), eq(JsonNode.class)))
            .thenThrow(new RuntimeException("connection refused"));

        ServiceResponse<ProductItem> result = statusService.getProductList("token");

        assertTrue(result.hasError());
        assertTrue(result.getErrorMessage().contains("connection refused"));
    }

    @Test
    void createProduct_whenNotConfigured_returnsError() {
        statusService = new StatusService(environment, restTemplate);

        ProductItem item = new ProductItem("p", "d", "t");
        ServiceResponse<Void> result = statusService.createProduct(item, "token");

        assertTrue(result.hasError());
        assertEquals("Product API not configured", result.getErrorMessage());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void createProduct_whenBackendReturnsNon2xx_returnsError() {
        String baseUrl = "http://localhost/api";
        environment.setProperty(StatusService.STATUS_API_URL_ENV, baseUrl);
        statusService = new StatusService(environment, restTemplate);

        String expectedUrl = baseUrl + "/" + StatusService.PRODUCT_API_PATH;
        when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.POST), any(), eq(JsonNode.class)))
            .thenReturn(new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR));

        ServiceResponse<Void> result = statusService.createProduct(new ProductItem("p", "d", "t"), "token");

        assertTrue(result.hasError());
        assertTrue(result.getErrorMessage().contains("500"));
    }

    @Test
    void createProduct_whenBackendReturns2xxWithNullBody_usesDefaultMessage() {
        String baseUrl = "http://localhost/api";
        environment.setProperty(StatusService.STATUS_API_URL_ENV, baseUrl);
        statusService = new StatusService(environment, restTemplate);

        String expectedUrl = baseUrl + "/" + StatusService.PRODUCT_API_PATH;
        when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.POST), any(), eq(JsonNode.class)))
            .thenReturn(new ResponseEntity<>(null, HttpStatus.CREATED));

        ServiceResponse<Void> result = statusService.createProduct(new ProductItem("p", "d", "t"), "token");

        assertFalse(result.hasError());
        assertEquals("Product created successfully", result.getMessage());
    }

    @Test
    void createProduct_whenRestTemplateThrows_returnsErrorWithExceptionMessage() {
        String baseUrl = "http://localhost/api";
        environment.setProperty(StatusService.STATUS_API_URL_ENV, baseUrl);
        statusService = new StatusService(environment, restTemplate);

        String expectedUrl = baseUrl + "/" + StatusService.PRODUCT_API_PATH;
        when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.POST), any(), eq(JsonNode.class)))
            .thenThrow(new RuntimeException("boom"));

        ServiceResponse<Void> result = statusService.createProduct(new ProductItem("p", "d", "t"), "token");

        assertTrue(result.hasError());
        assertTrue(result.getErrorMessage().contains("boom"));
    }

    @Test
    void updateProduct_whenNotConfigured_returnsError() {
        statusService = new StatusService(environment, restTemplate);

        ServiceResponse<Void> result = statusService.updateProduct("p", "d", "t", "token");

        assertTrue(result.hasError());
        assertEquals("Product API not configured", result.getErrorMessage());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void updateProduct_whenBackendReturnsNon2xx_returnsError() {
        String baseUrl = "http://localhost/api";
        environment.setProperty(StatusService.STATUS_API_URL_ENV, baseUrl);
        statusService = new StatusService(environment, restTemplate);

        String expectedUrl = baseUrl + "/" + StatusService.PRODUCT_API_PATH + "?product_name={productName}";
        when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.PUT), any(), eq(JsonNode.class), eq("p1")))
            .thenReturn(new ResponseEntity<>(null, HttpStatus.BAD_REQUEST));

        ServiceResponse<Void> result = statusService.updateProduct("p1", "d", "t", "token");

        assertTrue(result.hasError());
        assertTrue(result.getErrorMessage().contains("400"));
    }

    @Test
    void updateProduct_whenBackendReturns2xxWithNullBody_usesDefaultMessage() {
        String baseUrl = "http://localhost/api";
        environment.setProperty(StatusService.STATUS_API_URL_ENV, baseUrl);
        statusService = new StatusService(environment, restTemplate);

        String expectedUrl = baseUrl + "/" + StatusService.PRODUCT_API_PATH + "?product_name={productName}";
        when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.PUT), any(), eq(JsonNode.class), eq("p1")))
            .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        ServiceResponse<Void> result = statusService.updateProduct("p1", "newDesc", "tc", "token");

        assertFalse(result.hasError());
        assertEquals("Product updated successfully", result.getMessage());
    }

    @Test
    void updateProduct_whenRestTemplateThrows_returnsErrorWithExceptionMessage() {
        String baseUrl = "http://localhost/api";
        environment.setProperty(StatusService.STATUS_API_URL_ENV, baseUrl);
        statusService = new StatusService(environment, restTemplate);

        String expectedUrl = baseUrl + "/" + StatusService.PRODUCT_API_PATH + "?product_name={productName}";
        when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.PUT), any(), eq(JsonNode.class), eq("p1")))
            .thenThrow(new RuntimeException("broken"));

        ServiceResponse<Void> result = statusService.updateProduct("p1", "d", "t", "token");

        assertTrue(result.hasError());
        assertTrue(result.getErrorMessage().contains("broken"));
    }

    @Test
    void deleteProduct_whenNotConfigured_returnsError() {
        statusService = new StatusService(environment, restTemplate);

        ServiceResponse<Void> result = statusService.deleteProduct("p", "token");

        assertTrue(result.hasError());
        assertEquals("Product API not configured", result.getErrorMessage());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void deleteProduct_whenBackendReturnsNon2xx_returnsError() {
        String baseUrl = "http://localhost/api";
        environment.setProperty(StatusService.STATUS_API_URL_ENV, baseUrl);
        statusService = new StatusService(environment, restTemplate);

        String expectedUrl = baseUrl + "/" + StatusService.PRODUCT_API_PATH + "?product_name={productName}";
        when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.DELETE), any(), eq(JsonNode.class), eq("p1")))
            .thenReturn(new ResponseEntity<>(null, HttpStatus.NOT_FOUND));

        ServiceResponse<Void> result = statusService.deleteProduct("p1", "token");

        assertTrue(result.hasError());
        assertTrue(result.getErrorMessage().contains("404"));
    }

    @Test
    void deleteProduct_whenBackendReturns2xxWithNullBody_usesDefaultMessage() {
        String baseUrl = "http://localhost/api";
        environment.setProperty(StatusService.STATUS_API_URL_ENV, baseUrl);
        statusService = new StatusService(environment, restTemplate);

        String expectedUrl = baseUrl + "/" + StatusService.PRODUCT_API_PATH + "?product_name={productName}";
        when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.DELETE), any(), eq(JsonNode.class), eq("p1")))
            .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        ServiceResponse<Void> result = statusService.deleteProduct("p1", "token");

        assertFalse(result.hasError());
        assertEquals("Product deleted successfully", result.getMessage());
    }

    @Test
    void deleteProduct_whenRestTemplateThrows_returnsErrorWithExceptionMessage() {
        String baseUrl = "http://localhost/api";
        environment.setProperty(StatusService.STATUS_API_URL_ENV, baseUrl);
        statusService = new StatusService(environment, restTemplate);

        String expectedUrl = baseUrl + "/" + StatusService.PRODUCT_API_PATH + "?product_name={productName}";
        when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.DELETE), any(), eq(JsonNode.class), eq("p1")))
            .thenThrow(new RuntimeException("nope"));

        ServiceResponse<Void> result = statusService.deleteProduct("p1", "token");

        assertTrue(result.hasError());
        assertTrue(result.getErrorMessage().contains("nope"));
    }

    @Test
    void createProduct_whenAccessTokenNull_doesNotSetBearerAuth() {
        String baseUrl = "http://localhost/api";
        environment.setProperty(StatusService.STATUS_API_URL_ENV, baseUrl);
        statusService = new StatusService(environment, restTemplate);

        String expectedUrl = baseUrl + "/" + StatusService.PRODUCT_API_PATH;
        when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.POST), any(), eq(JsonNode.class)))
            .thenReturn(new ResponseEntity<>(null, HttpStatus.CREATED));

        ServiceResponse<Void> result = statusService.createProduct(new ProductItem("p", "d", "t"), null);

        assertFalse(result.hasError());
        verify(restTemplate).exchange(eq(expectedUrl), eq(HttpMethod.POST), any(), eq(JsonNode.class));
    }
}
