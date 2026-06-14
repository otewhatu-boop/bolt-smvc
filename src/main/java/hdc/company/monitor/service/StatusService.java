package hdc.company.monitor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hdc.company.monitor.model.ProductItem;
import hdc.company.monitor.model.SystemStatusItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import java.util.ArrayList;
import java.util.List;

import java.util.Collections;

@Service
public class StatusService {

    private static final Logger logger = LoggerFactory.getLogger(StatusService.class);

    public static final String STATUS_API_URL_ENV = "STATUS_API_URL";
    public static final String STATUS_API_PATH = "status.php";
    public static final String PRODUCT_API_PATH = "product";

    private final RestTemplate restTemplate;
    private final String statusApiUrl;
    private final String productApiUrl;
    private final ObjectMapper objectMapper;
    private String lastErrorMessage = null;

    @Autowired
    public StatusService(Environment environment) {
        this(environment, new RestTemplate());
    }

    public StatusService(Environment environment, RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
        String envUrl = environment.getProperty(STATUS_API_URL_ENV);
        String rawUrl = envUrl != null && !envUrl.isBlank() ? envUrl : null;
        this.statusApiUrl = rawUrl != null ? normalizeApiUrl(rawUrl, STATUS_API_PATH) : null;
        this.productApiUrl = rawUrl != null ? normalizeApiUrl(rawUrl, PRODUCT_API_PATH) : null;

        // Ensure RestTemplate can read JSON even when server responds with text/html
        try {
            List<HttpMessageConverter<?>> converters = this.restTemplate.getMessageConverters();
            boolean jacksonFound = false;
            for (HttpMessageConverter<?> c : converters) {
                if (c instanceof MappingJackson2HttpMessageConverter mj) {
                    jacksonFound = true;
                    List<MediaType> types = new ArrayList<>(mj.getSupportedMediaTypes());
                    if (!types.contains(MediaType.TEXT_HTML)) {
                        types.add(MediaType.TEXT_HTML);
                        mj.setSupportedMediaTypes(types);
                    }
                    break;
                }
            }
            if (!jacksonFound) {
                MappingJackson2HttpMessageConverter mj = new MappingJackson2HttpMessageConverter();
                mj.setSupportedMediaTypes(List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_HTML));
                this.restTemplate.getMessageConverters().add(0, mj);
            }
        } catch (Exception ex) {
            logger.warn("Unable to configure RestTemplate message converters to accept text/html", ex);
        }

        if (statusApiUrl != null) {
            logger.info("STATUS_API_URL resolved to [{}]", statusApiUrl);
        } else {
            logger.warn("STATUS_API_URL is not configured. Backend status API calls will not be made.");
        }
    }

    private static String normalizeApiUrl(String rawUrl, String path) {
        String normalized = rawUrl.trim();
        if (normalized.endsWith(path)) {
            return normalized;
        }
        if (normalized.endsWith("/")) {
            return normalized + path;
        }
        return normalized + "/" + path;
    }

    public List<ProductItem> getProductList(String accessToken) {
        lastErrorMessage = null;

        if (productApiUrl == null) {
            logger.debug("Product backend is not configured; returning empty list.");
            return Collections.emptyList();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            if (accessToken != null && !accessToken.isBlank()) {
                headers.setBearerAuth(accessToken);
                logger.debug("Authorization header added to product API request");
            }
            HttpEntity<String> entity = new HttpEntity<>(headers);
            logger.info("Calling backend product API at {}", productApiUrl);
            ResponseEntity<JsonNode> response = restTemplate.exchange(productApiUrl, HttpMethod.GET, entity, JsonNode.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode rootNode = response.getBody();
                JsonNode itemsNode = rootNode;

                if (rootNode.isObject() && rootNode.has("response_body")) {
                    itemsNode = rootNode.get("response_body");
                }

                if (itemsNode.isArray()) {
                    ProductItem[] items = objectMapper.treeToValue(itemsNode, ProductItem[].class);
                    logger.info("Backend product API returned {} items", items.length);
                    return List.of(items);
                } else if (itemsNode.isObject()) {
                    ProductItem item = objectMapper.treeToValue(itemsNode, ProductItem.class);
                    logger.info("Backend product API returned 1 item");
                    return List.of(item);
                } else {
                    logger.warn("Backend product API returned success but body/response_body is not an array or object");
                    lastErrorMessage = "Unexpected API response format";
                    return Collections.emptyList();
                }
            }
            lastErrorMessage = "Backend returned status " + response.getStatusCode();
        } catch (HttpClientErrorException.Unauthorized ex) {
            lastErrorMessage = "Access Denied: You do not have permission to view products.";
            logger.warn("Unauthorized access to backend product API");
        } catch (Exception ex) {
            logger.warn("Failed to fetch backend products from {}: {}", productApiUrl, ex.getMessage());
            lastErrorMessage = "Error fetching products: " + ex.getMessage();
        }
        return Collections.emptyList();
    }

    public List<SystemStatusItem> getSystemStatusList(String accessToken) {
        lastErrorMessage = null;
        
        if (statusApiUrl == null) {
            logger.debug("System status backend is not configured; returning empty list.");
            return Collections.emptyList();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            if (accessToken != null && !accessToken.isBlank()) {
                headers.setBearerAuth(accessToken);
                logger.debug("Authorization header added to status API request");
            } else {
                logger.warn("No access token available for status API request; proceeding without authorization");
            }
            HttpEntity<String> entity = new HttpEntity<>(headers);
            logger.info("Calling backend system status API at {}", statusApiUrl);
            ResponseEntity<JsonNode> response = restTemplate.exchange(statusApiUrl, HttpMethod.GET, entity, JsonNode.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode rootNode = response.getBody();
                JsonNode itemsNode = rootNode;

                if (rootNode.isObject() && rootNode.has("response_body")) {
                    itemsNode = rootNode.get("response_body");
                    logger.debug("Detected wrapped response format with 'response_body'");
                }

                if (itemsNode.isArray()) {
                    SystemStatusItem[] items = objectMapper.treeToValue(itemsNode, SystemStatusItem[].class);
                    logger.info("Backend system status API returned {} items", items.length);
                    return List.of(items);
                } else {
                    logger.warn("Backend system status API returned success but body/response_body is not an array");
                    lastErrorMessage = "Unexpected API response format";
                    return Collections.emptyList();
                }
            }
            logger.warn("Backend system status API returned {} with no body", response.getStatusCode());
            lastErrorMessage = "Backend returned status " + response.getStatusCode();
        } catch (HttpClientErrorException.Unauthorized ex) {
            String body = ex.getResponseBodyAsString();
            String reason = null;
            try {
                JsonNode errorNode = objectMapper.readTree(body);
                if (errorNode.has("reason")) {
                    reason = errorNode.get("reason").asText();
                }
            } catch (Exception e) {
                logger.debug("Could not parse 401 error response body as JSON: {}", body);
            }

            if (reason != null) {
                lastErrorMessage = "Access Denied: You do not have permission to view system status. Reason: " + reason;
            } else {
                lastErrorMessage = "Access Denied: You do not have permission to view system status.";
            }
            logger.warn("Unauthorized access to backend system status API: {}", lastErrorMessage);
        } catch (Exception ex) {
            logger.warn("Failed to fetch backend system status from {}: {}", statusApiUrl, ex.getMessage(), ex);
            lastErrorMessage = "Error fetching status: " + ex.getMessage();
        }
        return Collections.emptyList();
    }

    public List<SystemStatusItem> getSystemStatusList() {
        return getSystemStatusList(null);
    }

    public boolean isConfigured() {
        return statusApiUrl != null;
    }

    public boolean hasError() {
        return lastErrorMessage != null;
    }

    public String getErrorMessage() {
        return lastErrorMessage;
    }

    public List<String> getMissingConfiguration() {
        if (isConfigured()) {
            return Collections.emptyList();
        }
        return List.of(STATUS_API_URL_ENV);
    }
}
