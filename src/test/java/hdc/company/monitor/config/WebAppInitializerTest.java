package hdc.company.monitor.config;

import jakarta.servlet.Filter;
import org.junit.jupiter.api.Test;
import org.springframework.web.filter.DelegatingFilterProxy;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class WebAppInitializerTest {

    @Test
    void shouldConfigureDispatcherServletInitializer() {
        WebAppInitializer initializer = new WebAppInitializer();

        assertArrayEquals(new Class<?>[]{WebConfig.class}, initializer.getServletConfigClasses());
        assertArrayEquals(new String[]{"/"}, initializer.getServletMappings());

        Filter[] servletFilters = initializer.getServletFilters();
        assertNotNull(servletFilters);
        assertEquals(1, servletFilters.length);
        assertEquals(DelegatingFilterProxy.class, servletFilters[0].getClass());
    }
}
