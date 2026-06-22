package hdc.company.monitor.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hdc.company.monitor.util.CorrelationInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.web.client.RestTemplate;
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

    @Test
    public void restTemplate_hasCorrelationInterceptorWired() {
        RestTemplate restTemplate = config.restTemplate();

        boolean hasCorrelationInterceptor = restTemplate.getInterceptors().stream()
            .anyMatch(i -> i instanceof CorrelationInterceptor);

        assertTrue(hasCorrelationInterceptor, "RestTemplate should have a CorrelationInterceptor wired");
        assertEquals(1, restTemplate.getInterceptors().size());
    }

    @Test
    public void restTemplate_jacksonConverterSupportsTextHtml() {
        RestTemplate restTemplate = config.restTemplate();

        MappingJackson2HttpMessageConverter jackson = null;
        for (HttpMessageConverter<?> c : restTemplate.getMessageConverters()) {
            if (c instanceof MappingJackson2HttpMessageConverter mj) {
                jackson = mj;
                break;
            }
        }
        assertNotNull(jackson, "RestTemplate should have a Jackson converter");
        assertTrue(jackson.getSupportedMediaTypes().contains(MediaType.TEXT_HTML),
            "Jackson converter should support text/html to handle malformed server responses");
    }

    @Test
    public void passwordEncoder_isDelegatingPasswordEncoder() {
        PasswordEncoder encoder = config.passwordEncoder();

        assertNotNull(encoder);
        assertTrue(encoder instanceof DelegatingPasswordEncoder,
            "PasswordEncoder should be a DelegatingPasswordEncoder for multi-encoder support");
    }
}
