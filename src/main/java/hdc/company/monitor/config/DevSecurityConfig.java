package hdc.company.monitor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Development-only security configuration.
 * Active when the 'dev' profile is set. Provides a simple in-memory user for local development.
 */
@Configuration
@Profile("dev")
@org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
public class DevSecurityConfig {

    @Bean
    public UserDetailsService users() {
        // Default dev user: username=devuser, password=password (ok for dev only)
        UserDetails user = User.withDefaultPasswordEncoder()
                .username("devuser")
                .password("password")
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**", "/images/**").permitAll()
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
}
