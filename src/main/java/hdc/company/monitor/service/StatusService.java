package hdc.company.monitor.service;

import hdc.company.monitor.model.SystemStatusItem;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class StatusService {

    private static final String DEFAULT_STATUS_API_URL = "http://localhost/status.php";

    private final RestTemplate restTemplate = new RestTemplate();
    private final String statusApiUrl;

    public StatusService(Environment environment) {
        this.statusApiUrl = environment.getProperty("status.api.url", DEFAULT_STATUS_API_URL);
    }

    public List<SystemStatusItem> getSystemStatusList() {
        try {
            ResponseEntity<SystemStatusItem[]> response = restTemplate.getForEntity(statusApiUrl, SystemStatusItem[].class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Arrays.asList(response.getBody());
            }
        } catch (Exception ignored) {
            // Fall back to a default system status list when the external endpoint is unavailable.
        }
        return defaultSystemStatusList();
    }

    private List<SystemStatusItem> defaultSystemStatusList() {
        return Arrays.asList(
                new SystemStatusItem("system-001", "online", "2025-12-14 10:30:45"),
                new SystemStatusItem("system-002", "offline", "2025-12-14 10:25:30")
        );
    }
}
