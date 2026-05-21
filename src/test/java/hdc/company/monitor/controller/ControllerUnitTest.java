package hdc.company.monitor.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
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
        ProfileController controller = new ProfileController(new OAuth2AuthorizedClientRepository() {
            @Override
            public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String clientRegistrationId, Authentication principal, HttpServletRequest request) {
                return null;
            }

            @Override
            public void saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient, Authentication principal, HttpServletRequest request, HttpServletResponse response) {
            }

            @Override
            public void removeAuthorizedClient(String clientRegistrationId, Authentication principal, HttpServletRequest request, HttpServletResponse response) {
            }
        });
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(60), Map.of("email", "jules@example.com", "preferred_username", "jules"));
        TestOidcPrincipal oidcUser = new TestOidcPrincipal(List.of(new SimpleGrantedAuthority("ROLE_USER")), idToken, "email");
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "entra");

        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller.profile(authentication, null, model);

        assertEquals("profile", view);
        assertEquals(oidcUser, model.get("user"));
        assertEquals(oidcUser.getClaims(), model.get("claims"));
    }

    @Test
    void shouldReturnProfileViewWhenPrincipalIsNotOidcUser() {
        ProfileController controller = new ProfileController(new OAuth2AuthorizedClientRepository() {
            @Override
            public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String clientRegistrationId, Authentication principal, HttpServletRequest request) {
                return null;
            }

            @Override
            public void saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient, Authentication principal, HttpServletRequest request, HttpServletResponse response) {
            }

            @Override
            public void removeAuthorizedClient(String clientRegistrationId, Authentication principal, HttpServletRequest request, HttpServletResponse response) {
            }
        });
        ExtendedModelMap model = new ExtendedModelMap();

        Authentication authentication = new Authentication() {
            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return List.of();
            }

            @Override
            public Object getCredentials() {
                return null;
            }

            @Override
            public Object getDetails() {
                return null;
            }

            @Override
            public Object getPrincipal() {
                return new Object();
            }

            @Override
            public boolean isAuthenticated() {
                return false;
            }

            @Override
            public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
            }

            @Override
            public String getName() {
                return "anonymous";
            }
        };

        String view = controller.profile(authentication, null, model);

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
