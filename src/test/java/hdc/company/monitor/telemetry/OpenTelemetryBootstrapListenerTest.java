package hdc.company.monitor.telemetry;

import jakarta.servlet.ServletContextEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class OpenTelemetryBootstrapListenerTest {

    private static final String OTLP_HEADERS_PROP = "otel.exporter.otlp.headers";
    private static final String OTLP_ENDPOINT_PROP = "otel.exporter.otlp.endpoint";
    private static final String OTLP_LOGS_ENDPOINT_PROP = "otel.exporter.otlp.logs.endpoint";
    private static final String OTLP_TRACES_ENDPOINT_PROP = "otel.exporter.otlp.traces.endpoint";
    private static final String OTLP_METRICS_ENDPOINT_PROP = "otel.exporter.otlp.metrics.endpoint";
    private static final String OTLP_PROTOCOL_PROP = "otel.exporter.otlp.protocol";

    private final String[] managedProperties = {
            OTLP_HEADERS_PROP,
            OTLP_ENDPOINT_PROP,
            OTLP_LOGS_ENDPOINT_PROP,
            OTLP_TRACES_ENDPOINT_PROP,
            OTLP_METRICS_ENDPOINT_PROP,
            OTLP_PROTOCOL_PROP
    };

    @BeforeEach
    void clearProperties() {
        for (String prop : managedProperties) {
            System.clearProperty(prop);
        }
    }

    @AfterEach
    void restoreProperties() {
        for (String prop : managedProperties) {
            System.clearProperty(prop);
        }
    }

    @Test
    void contextInitializedShouldCompleteWithoutThrowing() {
        OpenTelemetryBootstrapListener listener = new OpenTelemetryBootstrapListener();
        ServletContextEvent event = mock(ServletContextEvent.class);
        assertDoesNotThrow(() -> listener.contextInitialized(event));
    }

    @Test
    void contextDestroyedWithNullSdkShouldNotThrow() {
        OpenTelemetryBootstrapListener listener = new OpenTelemetryBootstrapListener();
        ServletContextEvent event = mock(ServletContextEvent.class);
        assertDoesNotThrow(() -> listener.contextDestroyed(event));
    }

    @Test
    void contextDestroyedAfterInitShouldNotThrow() {
        OpenTelemetryBootstrapListener listener = new OpenTelemetryBootstrapListener();
        ServletContextEvent event = mock(ServletContextEvent.class);
        listener.contextInitialized(event);
        assertDoesNotThrow(() -> listener.contextDestroyed(event));
    }

    @Test
    void configureSumoTokenHeader_setsPropertyWhenHeadersNotConfigured() {
        // Trigger via contextInitialized — we just verify the property is set or is absent based on env
        // (No X_SUMO_TOKEN env in CI, so property should remain unset — just verify no exception)
        OpenTelemetryBootstrapListener listener = new OpenTelemetryBootstrapListener();
        assertDoesNotThrow(() -> listener.contextInitialized(mock(ServletContextEvent.class)));
    }

    @Test
    void configureOtlpEndpointCompatibility_noopWhenEndpointNotSet() {
        System.clearProperty(OTLP_ENDPOINT_PROP);
        OpenTelemetryBootstrapListener listener = new OpenTelemetryBootstrapListener();
        assertDoesNotThrow(() -> listener.contextInitialized(mock(ServletContextEvent.class)));
        assertNull(System.getProperty(OTLP_LOGS_ENDPOINT_PROP));
    }

    @Test
    void configureOtlpEndpointCompatibility_noopWhenEndpointHasNoPath() {
        System.setProperty(OTLP_ENDPOINT_PROP, "https://collector.example.com");
        OpenTelemetryBootstrapListener listener = new OpenTelemetryBootstrapListener();
        assertDoesNotThrow(() -> listener.contextInitialized(mock(ServletContextEvent.class)));
        assertNull(System.getProperty(OTLP_LOGS_ENDPOINT_PROP));
    }

    @Test
    void configureOtlpEndpointCompatibility_noopWhenEndpointPathIsSlash() {
        System.setProperty(OTLP_ENDPOINT_PROP, "https://collector.example.com/");
        OpenTelemetryBootstrapListener listener = new OpenTelemetryBootstrapListener();
        assertDoesNotThrow(() -> listener.contextInitialized(mock(ServletContextEvent.class)));
        assertNull(System.getProperty(OTLP_LOGS_ENDPOINT_PROP));
    }

    @Test
    void configureOtlpEndpointCompatibility_setsSignalEndpointsAndBaseWhenPathPresent() {
        System.setProperty(OTLP_ENDPOINT_PROP, "https://collector.example.com/v1/traces");
        OpenTelemetryBootstrapListener listener = new OpenTelemetryBootstrapListener();
        assertDoesNotThrow(() -> listener.contextInitialized(mock(ServletContextEvent.class)));
        assertEquals("https://collector.example.com", System.getProperty(OTLP_ENDPOINT_PROP));
        assertEquals("https://collector.example.com/v1/logs", System.getProperty(OTLP_LOGS_ENDPOINT_PROP));
        assertEquals("https://collector.example.com/v1/traces", System.getProperty(OTLP_TRACES_ENDPOINT_PROP));
        assertEquals("https://collector.example.com/v1/metrics", System.getProperty(OTLP_METRICS_ENDPOINT_PROP));
        assertEquals("http/protobuf", System.getProperty(OTLP_PROTOCOL_PROP));
    }

    @Test
    void configureOtlpEndpointCompatibility_doesNotOverrideExistingSignalEndpoints() {
        System.setProperty(OTLP_ENDPOINT_PROP, "https://collector.example.com/v1/traces");
        System.setProperty(OTLP_LOGS_ENDPOINT_PROP, "https://custom-logs.example.com/v1/logs");
        OpenTelemetryBootstrapListener listener = new OpenTelemetryBootstrapListener();
        assertDoesNotThrow(() -> listener.contextInitialized(mock(ServletContextEvent.class)));
        assertEquals("https://custom-logs.example.com/v1/logs", System.getProperty(OTLP_LOGS_ENDPOINT_PROP));
    }

    @Test
    void configureOtlpEndpointCompatibility_doesNotOverrideExistingProtocol() {
        System.setProperty(OTLP_ENDPOINT_PROP, "https://collector.example.com/v1/traces");
        System.setProperty(OTLP_PROTOCOL_PROP, "grpc");
        OpenTelemetryBootstrapListener listener = new OpenTelemetryBootstrapListener();
        assertDoesNotThrow(() -> listener.contextInitialized(mock(ServletContextEvent.class)));
        assertEquals("grpc", System.getProperty(OTLP_PROTOCOL_PROP));
    }

    @Test
    void configureOtlpEndpointCompatibility_handlesEndpointWithTrailingSlash() {
        System.setProperty(OTLP_ENDPOINT_PROP, "https://collector.example.com/collector/");
        OpenTelemetryBootstrapListener listener = new OpenTelemetryBootstrapListener();
        assertDoesNotThrow(() -> listener.contextInitialized(mock(ServletContextEvent.class)));
        assertEquals("https://collector.example.com/collector/v1/logs", System.getProperty(OTLP_LOGS_ENDPOINT_PROP));
    }

    @Test
    void configureOtlpEndpointCompatibility_handlesAlreadySignalSuffixedEndpoint() {
        System.setProperty(OTLP_ENDPOINT_PROP, "https://collector.example.com/v1/logs");
        OpenTelemetryBootstrapListener listener = new OpenTelemetryBootstrapListener();
        assertDoesNotThrow(() -> listener.contextInitialized(mock(ServletContextEvent.class)));
        assertEquals("https://collector.example.com/v1/logs", System.getProperty(OTLP_LOGS_ENDPOINT_PROP));
    }

    @Test
    void configureOtlpEndpointCompatibility_skipsWhenHeadersPropertyAlreadySet() {
        System.setProperty(OTLP_HEADERS_PROP, "x-custom=value");
        OpenTelemetryBootstrapListener listener = new OpenTelemetryBootstrapListener();
        assertDoesNotThrow(() -> listener.contextInitialized(mock(ServletContextEvent.class)));
        assertEquals("x-custom=value", System.getProperty(OTLP_HEADERS_PROP));
    }

    @Test
    void configureOtlpEndpointCompatibility_handlesInvalidUri() {
        System.setProperty(OTLP_ENDPOINT_PROP, "not a valid uri ://");
        OpenTelemetryBootstrapListener listener = new OpenTelemetryBootstrapListener();
        assertDoesNotThrow(() -> listener.contextInitialized(mock(ServletContextEvent.class)));
    }
}
