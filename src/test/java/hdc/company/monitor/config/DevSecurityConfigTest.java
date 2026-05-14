package hdc.company.monitor.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { hdc.company.monitor.config.DevSecurityConfig.class })
@ActiveProfiles("dev")
public class DevSecurityConfigTest {

    @Autowired
    private UserDetailsService userDetailsService;

    @Test
    void devUserIsPresent() {
        assertNotNull(userDetailsService);
        assertNotNull(userDetailsService.loadUserByUsername("devuser"));
    }
}
