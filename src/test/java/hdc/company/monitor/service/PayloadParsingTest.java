package hdc.company.monitor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hdc.company.monitor.model.SystemStatusItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PayloadParsingTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void reproducePayloadParsingIssue() throws Exception {
        String payload = "{\"user_id\":\"QC2pEukU782UF50rOcixKlS3KsG0Fix-x0y6_Z1I3EY\",\"preferred_username\":\"jules@hdc.company\",\"audience\":\"bf53ad5f-760d-40e8-a6db-467eacada791\",\"status_code\":200,\"response_body\":[{\"system_id\":\"jenkins\",\"test_case\":\"connectivity_test\",\"status\":\"pass\",\"timestamp\":\"2026-05-30 08:00:00\"},{\"system_id\":\"jenkins\",\"test_case\":\"health_check\",\"status\":\"pass\",\"timestamp\":\"2026-05-30 08:00:00\"},{\"system_id\":\"nexus\",\"test_case\":\"health_check\",\"status\":\"fail\",\"timestamp\":\"2026-05-30 08:00:00\"},{\"system_id\":\"nexus\",\"test_case\":\"storage_test\",\"status\":\"fail\",\"timestamp\":\"2026-05-30 08:00:00\"},{\"system_id\":\"teamcity\",\"test_case\":\"build_queue_test\",\"status\":\"pass\",\"timestamp\":\"2026-05-30 08:00:00\"},{\"system_id\":\"teamcity\",\"test_case\":\"health_check\",\"status\":\"pass\",\"timestamp\":\"2026-05-30 08:00:00\"}]}";

        // 1. Check if we can parse it directly as an array (should fail or return empty/null if it expects a list)
        // The current StatusService does: ResponseEntity<SystemStatusItem[]> response = restTemplate.exchange(statusApiUrl, HttpMethod.GET, entity, SystemStatusItem[].class);
        // This will likely fail with a Jackson error because the payload is an object, not an array.

        assertThrows(Exception.class, () -> {
            objectMapper.readValue(payload, SystemStatusItem[].class);
        }, "Should fail to parse wrapped object as array");

        // 2. Extract response_body and try to parse items
        JsonNode rootNode = objectMapper.readTree(payload);
        JsonNode responseBody = rootNode.get("response_body");
        assertNotNull(responseBody);
        assertTrue(responseBody.isArray());

        SystemStatusItem[] items = objectMapper.treeToValue(responseBody, SystemStatusItem[].class);
        assertNotNull(items);
        assertTrue(items.length > 0);

        SystemStatusItem firstItem = items[0];
        assertEquals("jenkins", firstItem.getSystemId());
        assertEquals("connectivity_test", firstItem.getTestCase());
        assertEquals("2026-05-30 08:00:00", firstItem.getUpdatedAt());
    }
}
