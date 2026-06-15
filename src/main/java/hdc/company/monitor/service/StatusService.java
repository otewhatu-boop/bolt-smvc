package hdc.company.monitor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hdc.company.monitor.model.ProductItem;
import hdc.company.monitor.model.ServiceResponse;
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
    public static final String APP_ENV_ENV = "APP_ENV";

    private final Environment environment;
    private final RestTemplate restTemplate;
    private final String apiBaseUrl;
    private final String statusApiUrl;
    private final String productApiUrl;
    private final ObjectMapper objectMapper;

    @Autowired
    public StatusService(Environment environment) {
        this(environment, new RestTemplate());
    }

    public StatusService(Environment environment, RestTemplate restTemplate) {
        this.environment = environment;
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
        String envUrl = this.environment.getProperty(STATUS_API_URL_ENV);
        String rawUrl = envUrl != null && !envUrl.isBlank() ? envUrl.trim() : null;
        this.apiBaseUrl = rawUrl != null ? normalizeBaseUrl(rawUrl) : null;
        this.statusApiUrl = apiBaseUrl != null ? buildUrl(STATUS_API_PATH) : null;
        this.productApiUrl = apiBaseUrl != null ? buildUrl(PRODUCT_API_PATH) : null;

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

        if (apiBaseUrl != null) {
            logger.info("API base URL resolved to [{}]", apiBaseUrl);
            logger.info("Status endpoint resolved to [{}]", statusApiUrl);
            logger.info("Product endpoint resolved to [{}]", productApiUrl);
        } else {
            logger.warn("STATUS_API_URL is not configured. Backend API calls will not be made.");
        }
    }

    private String buildUrl(String path) {
        if (apiBaseUrl == null) return null;
        return apiBaseUrl + (apiBaseUrl.endsWith("/") ? "" : "/") + path;
    }

    private static String normalizeBaseUrl(String rawUrl) {
        String normalized = rawUrl;
        if (normalized.endsWith(STATUS_API_PATH)) {
            normalized = normalized.substring(0, normalized.length() - STATUS_API_PATH.length());
        }
        if (normalized.isEmpty()) return "/";
        return normalized;
    }

    public ServiceResponse<ProductItem> getProductList(String accessToken) {
        if (productApiUrl == null) {
            logger.debug("Product backend is not configured; returning empty list.");
            return ServiceResponse.success(Collections.emptyList());
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
                    return ServiceResponse.success(List.of(items));
                } else if (itemsNode.isObject()) {
                    ProductItem item = objectMapper.treeToValue(itemsNode, ProductItem.class);
                    logger.info("Backend product API returned 1 item");
                    return ServiceResponse.success(List.of(item));
                } else {
                    logger.warn("Backend product API returned success but body/response_body is not an array or object");
                    return ServiceResponse.error("Unexpected API response format");
                }
            }
            return ServiceResponse.error("Backend returned status " + response.getStatusCode());
        } catch (HttpClientErrorException.Unauthorized ex) {
            logger.warn("Unauthorized access to backend product API");
            return ServiceResponse.error("Access Denied: You do not have permission to view products.");
        } catch (Exception ex) {
            logger.warn("Failed to fetch backend products from {}: {}", productApiUrl, ex.getMessage());
            String errorMessage;
            if ("development".equalsIgnoreCase(environment.getProperty(APP_ENV_ENV))) {
                errorMessage = "Error fetching products: " + ex.getMessage();
            } else {
                errorMessage = "An error occurred while fetching products. Please contact support.";
            }
            return ServiceResponse.error(errorMessage);
        }
    }

    public ServiceResponse<SystemStatusItem> getSystemStatusList(String accessToken) {
        if (statusApiUrl == null) {
            logger.debug("System status backend is not configured; returning empty list.");
            return ServiceResponse.success(Collections.emptyList());
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
                    return ServiceResponse.success(List.of(items));
                } else {
                    logger.warn("Backend system status API returned success but body/response_body is not an array");
                    return ServiceResponse.error("Unexpected API response format");
                }
            }
            logger.warn("Backend system status API returned {} with no body", response.getStatusCode());
            return ServiceResponse.error("Backend returned status " + response.getStatusCode());
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

            String errorMessage;
            if (reason != null) {
                errorMessage = "Access Denied: You do not have permission to view system status. Reason: " + reason;
            } else {
                errorMessage = "Access Denied: You do not have permission to view system status.";
            }
            logger.warn("Unauthorized access to backend system status API: {}", errorMessage);
            return ServiceResponse.error(errorMessage);
        } catch (Exception ex) {
            logger.warn("Failed to fetch backend system status from {}: {}", statusApiUrl, ex.getMessage(), ex);
            String errorMessage;
            if ("development".equalsIgnoreCase(environment.getProperty(APP_ENV_ENV))) {
                errorMessage = "Error fetching status: " + ex.getMessage();
            } else {
                errorMessage = "An error occurred while fetching system status. Please contact support.";
            }
            return ServiceResponse.error(errorMessage);
        }
    }

    public ServiceResponse<SystemStatusItem> getSystemStatusList() {
        return getSystemStatusList(null);
    }

    public boolean isConfigured() {
        return statusApiUrl != null;
    }

    public List<String> getMissingConfiguration() {
        if (isConfigured()) {
            return Collections.emptyList();
        }
        return List.of(STATUS_API_URL_ENV);
    }
}
