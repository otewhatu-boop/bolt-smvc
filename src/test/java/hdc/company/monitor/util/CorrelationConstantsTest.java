package hdc.company.monitor.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.*;

public class CorrelationConstantsTest {

    @Test
    void headerNameConstantShouldBeXCorrelationId() {
        assertEquals("X-Correlation-ID", CorrelationConstants.HEADER_NAME);
    }

    @Test
    void mdcKeyConstantShouldBeCorrelationId() {
        assertEquals("correlationId", CorrelationConstants.MDC_KEY);
    }

    @Test
    void privateConstructorShouldBeAccessible() throws Exception {
        Constructor<CorrelationConstants> constructor = CorrelationConstants.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertNotNull(constructor.newInstance());
    }
}
