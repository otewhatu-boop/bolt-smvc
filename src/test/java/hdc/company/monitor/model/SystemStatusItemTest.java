package hdc.company.monitor.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SystemStatusItemTest {

    @Test
    void testGettersAndSetters() {
        SystemStatusItem item = new SystemStatusItem();
        item.setSystemId("sys1");
        item.setStatus("OK");
        item.setUpdatedAt("2021-01-01");

        assertEquals("sys1", item.getSystemId());
        assertEquals("OK", item.getStatus());
        assertEquals("2021-01-01", item.getUpdatedAt());
    }

    @Test
    void testConstructor() {
        SystemStatusItem item = new SystemStatusItem("sys2", "WARN", "2021-01-02");
        assertEquals("sys2", item.getSystemId());
        assertEquals("WARN", item.getStatus());
        assertEquals("2021-01-02", item.getUpdatedAt());
    }
}
