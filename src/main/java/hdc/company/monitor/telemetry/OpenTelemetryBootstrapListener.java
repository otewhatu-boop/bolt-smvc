package hdc.company.monitor.telemetry;

import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final String SUMO_TOKEN_HEADER = "X-Sumo-Token";
    private static final String[] DIAGNOSTIC_ENVIRONMENT_VARIABLES = {
            "OTEL_SERVICE_NAME",
            "OTEL_RESOURCE_ATTRIBUTES",
            "OTEL_EXPORTER_OTLP_ENDPOINT",
            "OTEL_EXPORTER_OTLP_PROTOCOL",
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
            logOtlpHeaderDiagnostics();

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
            System.setProperty(OTLP_HEADERS_PROPERTY, SUMO_TOKEN_HEADER + "=" + sumoToken.trim());
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
        if (trimmed.contains(SUMO_TOKEN_HEADER + "=")) {
            return SUMO_TOKEN_HEADER + "=" + sha256Mask(trimmed.substring(trimmed.indexOf('=') + 1));
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

