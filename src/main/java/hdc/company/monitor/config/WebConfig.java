package hdc.company.monitor.config;

import hdc.company.monitor.util.CorrelationInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Configuration
@EnableWebMvc
@ComponentScan("hdc.company.monitor")
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // Add Correlation Interceptor
        restTemplate.setInterceptors(Collections.singletonList(new CorrelationInterceptor()));

        // Ensure RestTemplate can read JSON even when server responds with text/html
        List<HttpMessageConverter<?>> converters = restTemplate.getMessageConverters();
        boolean jacksonFound = false;
        for (HttpMessageConverter<?> c : converters) {
            if (c instanceof MappingJackson2HttpMessageConverter mj) {
                jacksonFound = true;
                List<MediaType> types = new ArrayList<>(mj.getSupportedMediaTypes());
                if (!types.contains(MediaType.TEXT_HTML)) {
                    types.add(MediaType.TEXT_HTML);
                    mj.setSupportedMediaTypes(types);
                }
                break;
            }
        }
        if (!jacksonFound) {
            MappingJackson2HttpMessageConverter mj = new MappingJackson2HttpMessageConverter();
            mj.setSupportedMediaTypes(List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_HTML));
            restTemplate.getMessageConverters().add(0, mj);
        }

        return restTemplate;
    }

    @Bean
    public SpringResourceTemplateResolver templateResolver() {
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setPrefix("/WEB-INF/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        return resolver;
    }
    
    @Bean
    public SpringTemplateEngine templateEngine() {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(templateResolver());
        return engine;
    }
    
    @Bean
    public ThymeleafViewResolver viewResolver() {
        ThymeleafViewResolver resolver = new ThymeleafViewResolver();
        resolver.setTemplateEngine(templateEngine());
        resolver.setCharacterEncoding("UTF-8");
        return resolver;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * Ensure favicon and its common variants are ignored by Spring Security completely
     * so that unauthenticated requests (e.g., browser automatic GETs) are not redirected
     * to the login page.
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers(
                "/favicon.ico",
                "/favicon-16x16.png",
                "/favicon-32x32.png",
                "/favicon.svg"
        );
    }
    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // Serve favicon.svg from the classpath:/static/ directory
        registry.addResourceHandler("/favicon.svg")
            .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);
        // Also allow general static files under /static/**
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);
    }

}