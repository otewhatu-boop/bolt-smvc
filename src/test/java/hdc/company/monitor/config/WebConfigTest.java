package hdc.company.monitor.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;

public class WebConfigTest {

    private WebConfig config;

    @BeforeEach
    public void setUp() {
        config = new WebConfig();
    }

    @Test
    public void shouldCreateTemplateResolver() {
        SpringResourceTemplateResolver resolver = config.templateResolver();

        assertNotNull(resolver, "Template resolver should be created");
        assertEquals("/WEB-INF/templates/", resolver.getPrefix());
        assertEquals(".html", resolver.getSuffix());
        assertEquals("UTF-8", resolver.getCharacterEncoding());
    }

    @Test
    public void shouldCreateTemplateEngine() {
        SpringTemplateEngine engine = config.templateEngine();

        assertNotNull(engine, "Template engine should be created");
    }

    @Test
    public void shouldCreateViewResolver() {
        ThymeleafViewResolver resolver = config.viewResolver();

        assertNotNull(resolver, "View resolver should be created");
        assertNotNull(resolver.getTemplateEngine(), "View resolver should have a template engine");
        assertEquals("UTF-8", resolver.getCharacterEncoding());
    }

    @Test
    public void shouldCreateSecurityCustomizer() {
        assertNotNull(config.webSecurityCustomizer(), "Web security customizer should be available");
    }

    @Test
    public void shouldRegisterResourceHandlers() {
        ResourceHandlerRegistry registry = new ResourceHandlerRegistry(new GenericApplicationContext(), new MockServletContext());

        config.addResourceHandlers(registry);

        assertEquals(true, registry.hasMappingForPattern("/favicon.svg"));
        assertEquals(true, registry.hasMappingForPattern("/static/**"));
    }
}
