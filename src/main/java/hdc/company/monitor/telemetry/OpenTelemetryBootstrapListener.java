package hdc.company.monitor.telemetry;

import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Initializes OpenTelemetry for the Logback appender so application logs are
 * duplicated to the OTLP endpoint configured by environment variables.
 */
@WebListener
public class OpenTelemetryBootstrapListener implements ServletContextListener {

    private static final Logger logger = LoggerFactory.getLogger(OpenTelemetryBootstrapListener.class);

    private static final String SUMO_TOKEN_ENV = "X_SUMO_TOKEN";
    private static final String OTLP_HEADERS_PROPERTY = "otel.exporter.otlp.headers";
    private static final String OTLP_HEADERS_ENV = "OTEL_EXPORTER_OTLP_HEADERS";
    private static final String OTLP_ENDPOINT_PROPERTY = "otel.exporter.otlp.endpoint";
    private static final String OTLP_ENDPOINT_ENV = "OTEL_EXPORTER_OTLP_ENDPOINT";
    private static final String OTLP_LOGS_ENDPOINT_PROPERTY = "otel.exporter.otlp.logs.endpoint";
    private static final String OTLP_LOGS_ENDPOINT_ENV = "OTEL_EXPORTER_OTLP_LOGS_ENDPOINT";
    private static final String OTLP_TRACES_ENDPOINT_PROPERTY = "otel.exporter.otlp.traces.endpoint";
    private static final String OTLP_TRACES_ENDPOINT_ENV = "OTEL_EXPORTER_OTLP_TRACES_ENDPOINT";
    private static final String OTLP_METRICS_ENDPOINT_PROPERTY = "otel.exporter.otlp.metrics.endpoint";
    private static final String OTLP_METRICS_ENDPOINT_ENV = "OTEL_EXPORTER_OTLP_METRICS_ENDPOINT";
    private static final String OTLP_PROTOCOL_PROPERTY = "otel.exporter.otlp.protocol";
    private static final String OTLP_PROTOCOL_ENV = "OTEL_EXPORTER_OTLP_PROTOCOL";
    private static final String SUMO_AUTHORIZATION_HEADER = "Authorization";
    private static final String SUMO_AUTHORIZATION_VALUE_PREFIX = "x-sumo-token:";
    private static final String[] DIAGNOSTIC_ENVIRONMENT_VARIABLES = {
            "OTEL_SERVICE_NAME",
            "OTEL_RESOURCE_ATTRIBUTES",
            OTLP_ENDPOINT_ENV,
            OTLP_LOGS_ENDPOINT_ENV,
            OTLP_TRACES_ENDPOINT_ENV,
            OTLP_METRICS_ENDPOINT_ENV,
            OTLP_PROTOCOL_ENV,
            "OTEL_EXPORTER_OTLP_HEADERS",
            SUMO_TOKEN_ENV,
            "OTEL_TRACES_EXPORTER",
            "OTEL_LOGS_EXPORTER",
            "OTEL_LOG_LEVEL",
            "OTEL_METRICS_EXPORTER"
    };

    private OpenTelemetrySdk openTelemetrySdk;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            logStartupDiagnostics("before OpenTelemetry autoconfiguration");
            configureSumoTokenHeader();
            configureOtlpEndpointCompatibility();
            logOtlpHeaderDiagnostics();
            logOtlpEndpointDiagnostics();

            openTelemetrySdk = AutoConfiguredOpenTelemetrySdk.initialize().getOpenTelemetrySdk();
            OpenTelemetryAppender.install(openTelemetrySdk);
            logger.info("OpenTelemetry logging initialized. Logs should now be exported when OTEL_LOGS_EXPORTER is configured.");
            logStartupDiagnostics("after OpenTelemetry appender installation");
        } catch (RuntimeException | LinkageError ex) {
            openTelemetrySdk = null;
            logger.warn("OpenTelemetry logging could not be initialized. Continuing with console logging only.", ex);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (openTelemetrySdk != null) {
            openTelemetrySdk.close();
        }
    }

    private static void configureSumoTokenHeader() {
        if (isConfigured(System.getProperty(OTLP_HEADERS_PROPERTY)) || isConfigured(System.getenv(OTLP_HEADERS_ENV))) {
            return;
        }

        String sumoToken = System.getenv(SUMO_TOKEN_ENV);
        if (isConfigured(sumoToken)) {
            System.setProperty(
                    OTLP_HEADERS_PROPERTY,
                    SUMO_AUTHORIZATION_HEADER + "=" + SUMO_AUTHORIZATION_VALUE_PREFIX + " " + sumoToken.trim()
            );
        }
    }

    private static void configureOtlpEndpointCompatibility() {
        String configuredEndpoint = firstConfigured(System.getProperty(OTLP_ENDPOINT_PROPERTY), System.getenv(OTLP_ENDPOINT_ENV));
        if (!isConfigured(configuredEndpoint)) {
            return;
        }

        URI endpointUri;
        try {
            endpointUri = new URI(configuredEndpoint.trim());
        } catch (URISyntaxException ex) {
            logger.warn("OTLP endpoint is not a valid URI: {}", configuredEndpoint, ex);
            return;
        }

        String path = endpointUri.getPath();
        if (!isConfigured(path) || "/".equals(path)) {
            return;
        }

        setIfMissing(OTLP_LOGS_ENDPOINT_PROPERTY, OTLP_LOGS_ENDPOINT_ENV, signalEndpoint(configuredEndpoint, "logs"));
        setIfMissing(OTLP_TRACES_ENDPOINT_PROPERTY, OTLP_TRACES_ENDPOINT_ENV, signalEndpoint(configuredEndpoint, "traces"));
        setIfMissing(OTLP_METRICS_ENDPOINT_PROPERTY, OTLP_METRICS_ENDPOINT_ENV, signalEndpoint(configuredEndpoint, "metrics"));

        String baseEndpoint = baseEndpoint(endpointUri);
        System.setProperty(OTLP_ENDPOINT_PROPERTY, baseEndpoint);
        logger.info(
                "OTLP endpoint '{}' includes path '{}'. Using '{}' as the generic OTLP endpoint and signal-specific /v1/* endpoints where not already configured.",
                configuredEndpoint,
                path,
                baseEndpoint
        );

        if (!isConfigured(System.getProperty(OTLP_PROTOCOL_PROPERTY)) && !isConfigured(System.getenv(OTLP_PROTOCOL_ENV))) {
            System.setProperty(OTLP_PROTOCOL_PROPERTY, "http/protobuf");
            logger.info("OTLP endpoint has a collector path; defaulting {} to http/protobuf.", OTLP_PROTOCOL_PROPERTY);
        }
    }

    private static String signalEndpoint(String endpoint, String signal) {
        String trimmedEndpoint = endpoint.trim();
        String suffix = "/v1/" + signal;
        if (trimmedEndpoint.endsWith(suffix)) {
            return trimmedEndpoint;
        }
        if (trimmedEndpoint.endsWith("/")) {
            return trimmedEndpoint.substring(0, trimmedEndpoint.length() - 1) + suffix;
        }
        return trimmedEndpoint + suffix;
    }

    private static String baseEndpoint(URI endpointUri) {
        try {
            return new URI(
                    endpointUri.getScheme(),
                    endpointUri.getUserInfo(),
                    endpointUri.getHost(),
                    endpointUri.getPort(),
                    null,
                    null,
                    null
            ).toString();
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Could not derive base OTLP endpoint from " + endpointUri, ex);
        }
    }

    private static void logStartupDiagnostics(String phase) {
        logger.info("OpenTelemetry startup diagnostics ({}):", phase);
        for (String name : DIAGNOSTIC_ENVIRONMENT_VARIABLES) {
            logger.info("{}={}", name, diagnosticValue(name, System.getenv(name)));
        }
    }

    private static void logOtlpHeaderDiagnostics() {
        logger.info(
                "OpenTelemetry OTLP header diagnostics: system property {} is {}, environment variable {} is {}.",
                OTLP_HEADERS_PROPERTY,
                diagnosticValue(OTLP_HEADERS_PROPERTY, System.getProperty(OTLP_HEADERS_PROPERTY)),
                OTLP_HEADERS_ENV,
                diagnosticValue(OTLP_HEADERS_ENV, System.getenv(OTLP_HEADERS_ENV))
        );
    }

    private static void logOtlpEndpointDiagnostics() {
        logger.info(
                "OpenTelemetry OTLP endpoint diagnostics: {}={}, {}={}, {}={}, {}={}, {}={}",
                OTLP_ENDPOINT_PROPERTY,
                diagnosticValue(OTLP_ENDPOINT_PROPERTY, System.getProperty(OTLP_ENDPOINT_PROPERTY)),
                OTLP_LOGS_ENDPOINT_PROPERTY,
                diagnosticValue(OTLP_LOGS_ENDPOINT_PROPERTY, System.getProperty(OTLP_LOGS_ENDPOINT_PROPERTY)),
                OTLP_TRACES_ENDPOINT_PROPERTY,
                diagnosticValue(OTLP_TRACES_ENDPOINT_PROPERTY, System.getProperty(OTLP_TRACES_ENDPOINT_PROPERTY)),
                OTLP_METRICS_ENDPOINT_PROPERTY,
                diagnosticValue(OTLP_METRICS_ENDPOINT_PROPERTY, System.getProperty(OTLP_METRICS_ENDPOINT_PROPERTY)),
                OTLP_PROTOCOL_PROPERTY,
                diagnosticValue(OTLP_PROTOCOL_PROPERTY, System.getProperty(OTLP_PROTOCOL_PROPERTY))
        );
    }

    private static void setIfMissing(String propertyName, String environmentVariableName, String value) {
        if (!isConfigured(System.getProperty(propertyName)) && !isConfigured(System.getenv(environmentVariableName))) {
            System.setProperty(propertyName, value);
        }
    }

    private static String firstConfigured(String first, String second) {
        if (isConfigured(first)) {
            return first;
        }
        return second;
    }

    private static String diagnosticValue(String name, String value) {
        if (!isConfigured(value)) {
            return "<unset>";
        }

        if (SUMO_TOKEN_ENV.equals(name)) {
            return sha256Mask(value);
        }

        if (OTLP_HEADERS_ENV.equals(name) || OTLP_HEADERS_PROPERTY.equals(name)) {
            return maskHeaderValue(value);
        }

        return value.trim();
    }

    private static String maskHeaderValue(String value) {
        String trimmed = value.trim();
        if (trimmed.startsWith(SUMO_AUTHORIZATION_HEADER + "=") && trimmed.contains(SUMO_AUTHORIZATION_VALUE_PREFIX)) {
            String token = trimmed.substring(trimmed.indexOf(SUMO_AUTHORIZATION_VALUE_PREFIX) + SUMO_AUTHORIZATION_VALUE_PREFIX.length());
            return SUMO_AUTHORIZATION_HEADER + "=" + SUMO_AUTHORIZATION_VALUE_PREFIX + " " + sha256Mask(token);
        }
        return "<configured>";
    }

    private static String sha256Mask(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.trim().getBytes(StandardCharsets.UTF_8));
            return "sha256:" + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            return "<sha256-unavailable>";
        }
    }

    private static boolean isConfigured(String value) {
        return value != null && !value.isBlank();
    }
}

