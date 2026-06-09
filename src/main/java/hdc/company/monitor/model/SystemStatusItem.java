package hdc.company.monitor.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SystemStatusItem {

    @JsonProperty("system_id")
    private String systemId;

    @JsonProperty("test_case")
    private String testCase;

    @JsonProperty("status")
    private String status;

    @JsonProperty("updated_at")
    @JsonAlias("timestamp")
    private String updatedAt;

    public SystemStatusItem() {
    }

    public SystemStatusItem(String systemId, String status, String updatedAt) {
        this(systemId, null, status, updatedAt);
    }

    public SystemStatusItem(String systemId, String testCase, String status, String updatedAt) {
        this.systemId = systemId;
        this.testCase = testCase;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public String getTestCase() {
        return testCase;
    }

    public void setTestCase(String testCase) {
        this.testCase = testCase;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
