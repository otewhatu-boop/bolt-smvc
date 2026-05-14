package hdc.company.monitor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;

@Configuration
@EnableWebMvc
@ComponentScan("hdc.company.monitor")
public class WebConfig implements WebMvcConfigurer {

    @Value("${ad.domain:hdc.webhop.net}")
    private String adDomain;

    @Value("${ad.url:ldap://hdc.webhop.net/}")
    private String adUrl;

    @Value("${app.profile:prod}")
    private String appProfile;
    
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

    // === PRODUCTION SECURITY CONFIG (Active when dev/test profile is NOT active) ===
    @Bean
    @Profile("!dev & !test")
    public ActiveDirectoryLdapAuthenticationProvider activeDirectoryLdapAuthenticationProvider() {
        ActiveDirectoryLdapAuthenticationProvider provider =
                new ActiveDirectoryLdapAuthenticationProvider(adDomain, adUrl);
        provider.setConvertSubErrorCodesToExceptions(true);
        provider.setUseAuthenticationRequestCredentials(true);
        return provider;
    }

    // === DEVELOPMENT SECURITY CONFIG (Active when dev profile IS active) ===
    @Bean
    @Profile("dev")
    public UserDetailsService devUsers() {
        UserDetails user = User.withDefaultPasswordEncoder()
                .username("devuser")
                .password("password")
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(user);
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
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
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