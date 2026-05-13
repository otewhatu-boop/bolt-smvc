package hdc.company.monitor.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.ui.ExtendedModelMap;

import java.security.Principal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ControllerUnitTest {

    private static final class TestOidcPrincipal extends DefaultOidcUser implements Principal {
        public TestOidcPrincipal(Collection<? extends GrantedAuthority> authorities, OidcIdToken idToken, String userNameAttributeName) {
            super(authorities, idToken, userNameAttributeName);
        }
    }

    @Test
    void shouldReturnDashboardAndPopulateOidcUserAttributes() {
        DashboardController controller = new DashboardController();
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(60), Map.of("preferred_username", "jules", "email", "jules@example.com", "name", "Jules"));
        TestOidcPrincipal oidcUser = new TestOidcPrincipal(List.of(new SimpleGrantedAuthority("ROLE_USER")), idToken, "email");

        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller.dashboard(oidcUser, model);

        assertEquals("dashboard", view);
        assertEquals("Jules", model.get("userName"));
        assertEquals("jules@example.com", model.get("userEmail"));
    }

    @Test
    void shouldReturnProfileAndPopulateClaimsForOidcUser() {
        ProfileController controller = new ProfileController();
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(60), Map.of("email", "jules@example.com", "preferred_username", "jules"));
        TestOidcPrincipal oidcUser = new TestOidcPrincipal(List.of(new SimpleGrantedAuthority("ROLE_USER")), idToken, "email");

        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller.profile(oidcUser, model);

        assertEquals("profile", view);
        assertEquals(oidcUser, model.get("user"));
        assertEquals(oidcUser.getClaims(), model.get("claims"));
    }

    @Test
    void shouldReturnProfileViewWhenPrincipalIsNotOidcUser() {
        ProfileController controller = new ProfileController();
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.profile(new Principal() {
            @Override
            public String getName() {
                return "anonymous";
            }
        }, model);

        assertEquals("profile", view);
        assertNull(model.get("user"));
        assertNull(model.get("claims"));
    }

    @Test
    void shouldReturnDashboardViewWhenPrincipalIsNotOidcUser() {
        DashboardController controller = new DashboardController();
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.dashboard(new Principal() {
            @Override
            public String getName() {
                return "anonymous";
            }
        }, model);

        assertEquals("dashboard", view);
        assertNull(model.get("userName"));
        assertNull(model.get("userEmail"));
    }
}
