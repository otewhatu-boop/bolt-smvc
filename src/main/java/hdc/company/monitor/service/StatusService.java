package hdc.company.monitor.service;

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
import org.springframework.web.client.RestTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import java.util.ArrayList;
import java.util.List;

import java.util.Collections;
import java.util.List;

@Service
public class StatusService {

    private static final Logger logger = LoggerFactory.getLogger(StatusService.class);

    public static final String STATUS_API_URL_ENV = "STATUS_API_URL";
    public static final String STATUS_API_URL_PROPERTY = "status.api.url";
    public static final String STATUS_API_PATH = "status.php";

    private final RestTemplate restTemplate;
    private final String statusApiUrl;
    private String lastErrorMessage = null;

    @Autowired
    public StatusService(Environment environment) {
        this(environment, new RestTemplate());
    }

    public StatusService(Environment environment, RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        String envUrl = environment.getProperty(STATUS_API_URL_ENV);
        String propertyUrl = environment.getProperty(STATUS_API_URL_PROPERTY);
        String rawUrl = envUrl != null && !envUrl.isBlank() ? envUrl : (propertyUrl != null && !propertyUrl.isBlank() ? propertyUrl : null);
        this.statusApiUrl = rawUrl != null ? normalizeStatusApiUrl(rawUrl) : null;

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

    private static String normalizeStatusApiUrl(String rawUrl) {
        String normalized = rawUrl.trim();
        if (normalized.endsWith(STATUS_API_PATH)) {
            return normalized;
        }
        if (normalized.endsWith("/")) {
            return normalized + STATUS_API_PATH;
        }
        return normalized + "/" + STATUS_API_PATH;
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
            ResponseEntity<SystemStatusItem[]> response = restTemplate.exchange(statusApiUrl, HttpMethod.GET, entity, SystemStatusItem[].class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                logger.info("Backend system status API returned {} items", response.getBody().length);
                return List.of(response.getBody());
            }
            logger.warn("Backend system status API returned {} with no body", response.getStatusCode());
            lastErrorMessage = "Backend returned status " + response.getStatusCode();
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
