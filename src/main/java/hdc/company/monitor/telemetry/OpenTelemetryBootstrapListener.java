package hdc.company.monitor.telemetry;

import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * Initializes OpenTelemetry for the Logback appender so application logs are
 * duplicated to the OTLP endpoint configured by environment variables.
 */
@WebListener
public class OpenTelemetryBootstrapListener implements ServletContextListener {

    private static final String SUMO_TOKEN_ENV = "X_SUMO_TOKEN";
    private static final String OTLP_HEADERS_PROPERTY = "otel.exporter.otlp.headers";
    private static final String OTLP_HEADERS_ENV = "OTEL_EXPORTER_OTLP_HEADERS";
    private static final String SUMO_TOKEN_HEADER = "X-Sumo-Token";

    private OpenTelemetrySdk openTelemetrySdk;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        configureSumoTokenHeader();

        openTelemetrySdk = AutoConfiguredOpenTelemetrySdk.initialize().getOpenTelemetrySdk();
        OpenTelemetryAppender.install(openTelemetrySdk);
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

    private static boolean isConfigured(String value) {
        return value != null && !value.isBlank();
    }
}
