package hdc.company.monitor.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;

@Configuration
@EnableWebSecurity
@Profile("!test") // Use EntraID for production and dev
@PropertySource("classpath:entra-id.properties")
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private final EntraIdProperties entraIdProperties;
    private final Environment environment;
    private final ObjectProvider<ActiveDirectoryLdapAuthenticationProvider> activeDirectoryLdapAuthenticationProvider;
    private final ObjectProvider<UserDetailsService> optionalUserDetailsService;

    public SecurityConfig(EntraIdProperties entraIdProperties,
                          Environment environment,
                          ObjectProvider<ActiveDirectoryLdapAuthenticationProvider> activeDirectoryLdapAuthenticationProvider,
                          ObjectProvider<UserDetailsService> optionalUserDetailsService) {
        this.entraIdProperties = entraIdProperties;
        this.environment = environment;
        this.activeDirectoryLdapAuthenticationProvider = activeDirectoryLdapAuthenticationProvider;
        this.optionalUserDetailsService = optionalUserDetailsService;
    }

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        if (!entraIdProperties.isConfigured()) {
            logger.warn("Azure EntraID is not configured. EntraID login will be disabled.");
            return registrationId -> null;
        }

        ClientRegistration entraRegistration = ClientRegistration.withRegistrationId("entra")
                .clientId(entraIdProperties.getClientId())
                .clientSecret(entraIdProperties.getClientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(entraIdProperties.getRedirectUri())
                .scope("openid", "profile", "email")
                .authorizationUri("https://login.microsoftonline.com/" + entraIdProperties.getTenantId() + "/oauth2/v2.0/authorize")
                .tokenUri("https://login.microsoftonline.com/" + entraIdProperties.getTenantId() + "/oauth2/v2.0/token")
                .jwkSetUri("https://login.microsoftonline.com/" + entraIdProperties.getTenantId() + "/discovery/v2.0/keys")
                .userInfoUri("https://graph.microsoft.com/oidc/userinfo")
                .userNameAttributeName("preferred_username")
                .build();

        return new InMemoryClientRegistrationRepository(entraRegistration);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        String profiles = String.join(",", environment.getActiveProfiles());
        String activeProfilesMessage = "Active Spring profiles: " + profiles;
        System.out.println(activeProfilesMessage);
        logger.info(activeProfilesMessage);
        if (!entraIdProperties.isConfigured()) {
            String warning = "Azure EntraID is not configured. Falling back to local authentication or permissive mode.";
            System.out.println(warning);
            logger.warn(warning);

            boolean isDevProfile = hasActiveProfile("dev");
            ActiveDirectoryLdapAuthenticationProvider ldapProvider = activeDirectoryLdapAuthenticationProvider.getIfAvailable();
            UserDetailsService devUserDetailsService = optionalUserDetailsService.getIfAvailable();

            if (isDevProfile && devUserDetailsService != null) {
                http
                    .userDetailsService(devUserDetailsService)
                    .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(new AntPathRequestMatcher("/login")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/login/oauth2/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/index.html")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/css/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/js/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/images/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/favicon.svg")).permitAll()
                        .anyRequest().authenticated()
                    )
                    .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                    )
                    .logout(logout -> logout
                        .logoutUrl("/logout")
                        .permitAll()
                    );

                return http.build();
            }

            if (ldapProvider != null) {
                http
                    .authenticationProvider(ldapProvider)
                    .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(new AntPathRequestMatcher("/login")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/login/oauth2/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/index.html")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/css/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/js/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/images/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/favicon.svg")).permitAll()
                        .anyRequest().authenticated()
                    )
                    .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                    )
                    .logout(logout -> logout
                        .logoutUrl("/logout")
                        .permitAll()
                    );

                return http.build();
            }

            http
                .authorizeHttpRequests(authorize -> authorize
                    .requestMatchers(new AntPathRequestMatcher("/login")).permitAll()
                    .requestMatchers(new AntPathRequestMatcher("/login/oauth2/**")).permitAll()
                    .requestMatchers(new AntPathRequestMatcher("/index.html")).permitAll()
                    .requestMatchers(new AntPathRequestMatcher("/css/**")).permitAll()
                    .requestMatchers(new AntPathRequestMatcher("/js/**")).permitAll()
                    .requestMatchers(new AntPathRequestMatcher("/images/**")).permitAll()
                    .requestMatchers(new AntPathRequestMatcher("/favicon.svg")).permitAll()
                    .anyRequest().permitAll()
                );
            return http.build();
        }

        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(new AntPathRequestMatcher("/login")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/login/oauth2/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/oauth2/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/index.html")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/css/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/js/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/images/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/favicon.svg")).permitAll()
                .anyRequest().authenticated()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .permitAll()
            );

        return http.build();
    }

    private boolean hasActiveProfile(String profile) {
        for (String activeProfile : environment.getActiveProfiles()) {
            if (profile.equalsIgnoreCase(activeProfile)) {
                return true;
            }
        }
        return false;
    }
}
