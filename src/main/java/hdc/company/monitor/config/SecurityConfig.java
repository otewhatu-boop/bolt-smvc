package hdc.company.monitor.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.context.annotation.PropertySource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.web.client.RestClient;
import hdc.company.monitor.util.CorrelationInterceptor;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@PropertySource("classpath:entra-id.properties")
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private final EntraIdProperties entraIdProperties;
    private final Environment environment;

    public SecurityConfig(EntraIdProperties entraIdProperties,
                          Environment environment) {
        this.entraIdProperties = entraIdProperties;
        this.environment = environment;
    }


    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        if (!entraIdProperties.isConfigured()) {
            logger.warn("Azure EntraID is not configured. EntraID login will be disabled.");
            ClientRegistration dummyRegistration = ClientRegistration.withRegistrationId("entra")
                    .clientId("dummy-client-id")
                    .clientSecret("dummy-client-secret")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("http://localhost:8080/smvc/login/oauth2/code/entra")
                    .scope("openid", "profile", "email", entraIdProperties.getApiScope())
                    .authorizationUri("https://login.microsoftonline.com/dummy/oauth2/v2.0/authorize")
                    .tokenUri("https://login.microsoftonline.com/dummy/oauth2/v2.0/token")
                    .jwkSetUri("https://login.microsoftonline.com/dummy/discovery/v2.0/keys")
                    .userInfoUri("https://graph.microsoft.com/oidc/userinfo")
                    .userNameAttributeName("sub")
                    .build();
            return new InMemoryClientRegistrationRepository(dummyRegistration);
        }

        ClientRegistration entraRegistration = ClientRegistration.withRegistrationId("entra")
                .clientId(entraIdProperties.getClientId())
                .clientSecret(entraIdProperties.getClientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(entraIdProperties.getRedirectUri())
                .scope("openid", "profile", "email", entraIdProperties.getApiScope())
                .authorizationUri("https://login.microsoftonline.com/" + entraIdProperties.getTenantId() + "/oauth2/v2.0/authorize")
                .tokenUri("https://login.microsoftonline.com/" + entraIdProperties.getTenantId() + "/oauth2/v2.0/token")
                .jwkSetUri("https://login.microsoftonline.com/" + entraIdProperties.getTenantId() + "/discovery/v2.0/keys")
                .userInfoUri("https://graph.microsoft.com/oidc/userinfo")
                .userNameAttributeName("sub")
                .build();

        return new InMemoryClientRegistrationRepository(entraRegistration);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, OAuth2AuthorizedClientService authorizedClientService) throws Exception {
        String profiles = String.join(",", environment.getActiveProfiles());
        String activeProfilesMessage = "Active Spring profiles: " + profiles;
        System.out.println(activeProfilesMessage);
        logger.info(activeProfilesMessage);
        if (!entraIdProperties.isConfigured()) {
            String warning = "Azure EntraID is not configured. Falling back to permissive mode.";
            System.out.println(warning);
            logger.warn(warning);

            http
                .authorizeHttpRequests(authorize -> authorize
                    .requestMatchers("/login").permitAll()
                    .requestMatchers("/login/oauth2/**").permitAll()
                    .requestMatchers("/index.html").permitAll()
                    .requestMatchers("/css/**").permitAll()
                    .requestMatchers("/js/**").permitAll()
                    .requestMatchers("/images/**").permitAll()
                    .requestMatchers("/favicon.svg").permitAll()
                    .anyRequest().permitAll()
                );
            return http.build();
        }

        http
            .authorizeHttpRequests(authorize -> {
                authorize
                    .requestMatchers("/login").permitAll()
                    .requestMatchers("/login/oauth2/**").permitAll()
                    .requestMatchers("/oauth2/**").permitAll()
                    .requestMatchers("/index.html").permitAll()
                    .requestMatchers("/css/**").permitAll()
                    .requestMatchers("/js/**").permitAll()
                    .requestMatchers("/images/**").permitAll()
                    .requestMatchers("/favicon.svg").permitAll()
                    .anyRequest().authenticated();
            })
            .oauth2Login(oauth2 -> oauth2
                .tokenEndpoint(tokenEndpoint -> tokenEndpoint.accessTokenResponseClient(authorizationCodeTokenResponseClient()))
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureHandler((request, response, exception) -> {
                    String errorCode = "invalid_token_response";
                    String errorDescription = exception.getMessage();
                    if (exception instanceof OAuth2AuthenticationException authEx) {
                        errorCode = authEx.getError().getErrorCode();
                        errorDescription = authEx.getError().getDescription();
                        if (errorDescription == null || errorDescription.isBlank()) {
                            errorDescription = authEx.getError().toString();
                        }
                    }
                    String encoded = URLEncoder.encode(errorCode + ": " + (errorDescription == null ? "unknown error" : errorDescription), StandardCharsets.UTF_8);
                    logger.error("OAuth2 login failed: {} - {}", errorCode, errorDescription, exception);
                    response.sendRedirect(request.getContextPath() + "/login?error=" + encoded);
                })
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutRequestMatcher(request -> "GET".equalsIgnoreCase(request.getMethod()) && "/logout".equals(request.getServletPath()))
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .addLogoutHandler((request, response, authentication) -> {
                    if (authentication != null && authorizedClientService != null) {
                        try {
                            authorizedClientService.removeAuthorizedClient("entra", authentication.getName());
                        } catch (Exception e) {
                            logger.warn("Failed to remove authorized client on logout", e);
                        }
                    }
                })
                .logoutSuccessUrl("/")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public OAuth2AuthorizedClientRepository authorizedClientRepository() {
        return new HttpSessionOAuth2AuthorizedClientRepository();
    }

    @Bean
    public OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository clientRegistrationRepository) {
        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
    }

    private OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> authorizationCodeTokenResponseClient() {
        RestClientAuthorizationCodeTokenResponseClient accessTokenResponseClient = new RestClientAuthorizationCodeTokenResponseClient();

        RestClient restClient = RestClient.builder()
                .messageConverters(messageConverters -> {
                    messageConverters.clear();
                    messageConverters.add(new FormHttpMessageConverter());
                    messageConverters.add(new OAuth2AccessTokenResponseHttpMessageConverter());
                })
                .requestInterceptor(new CorrelationInterceptor())
                .defaultStatusHandler(new OAuth2ErrorResponseErrorHandler())
                .build();

        accessTokenResponseClient.setRestClient(restClient);
        return accessTokenResponseClient;
    }
}
