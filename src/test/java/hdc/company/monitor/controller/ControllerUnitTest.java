package hdc.company.monitor.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;

import java.security.Principal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import hdc.company.monitor.service.EntraIdOboService;
import hdc.company.monitor.service.StatusService;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hdc.company.monitor.model.ServiceResponse;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

public class ControllerUnitTest {

    private static final class TestOidcPrincipal extends DefaultOidcUser implements Principal {
        public TestOidcPrincipal(Collection<? extends GrantedAuthority> authorities, OidcIdToken idToken, String userNameAttributeName) {
            super(authorities, idToken, userNameAttributeName);
        }
    }

    @Test
    void shouldReturnDashboardAndPopulateOidcUserAttributes() {
        OAuth2AuthorizedClientRepository authorizedClientRepository = mock(OAuth2AuthorizedClientRepository.class);
        EntraIdOboService oboService = mock(EntraIdOboService.class);
        DashboardController controller = new DashboardController(new StatusService(new MockEnvironment(), new RestTemplate()), oboService, authorizedClientRepository);
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(60), Map.of("preferred_username", "jules", "email", "jules@example.com", "name", "Jules"));
        TestOidcPrincipal oidcUser = new TestOidcPrincipal(List.of(new SimpleGrantedAuthority("ROLE_USER")), idToken, "email");

        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller.dashboard(oidcUser, new MockHttpServletRequest(), model);

        assertEquals("dashboard", view);
        assertEquals("Jules", model.get("userName"));
        assertEquals("jules@example.com", model.get("userEmail"));
    }

    @Test
    void shouldReturnProfileAndPopulateClaimsForOidcUser() {
        EntraIdOboService oboService = mock(EntraIdOboService.class);
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
        }, oboService, new MockEnvironment());
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
    void shouldPopulateAccessTokenInfoWhenAccessTokenIsNotJwt() {
        EntraIdOboService oboService = mock(EntraIdOboService.class);
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "not-a-jwt",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Set.of("openid", "profile")
        );

        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("entra")
                .clientId("client-id")
                .clientSecret("client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://auth.example.com/oauth2/authorize")
                .tokenUri("https://auth.example.com/oauth2/token")
                .scope("openid", "profile")
                .build();

        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                new TestOidcPrincipal(List.of(new SimpleGrantedAuthority("ROLE_USER")),
                        new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(60), Map.of("email", "jules@example.com")),
                        "email"),
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                "entra"
        );

        OAuth2AuthorizedClientRepository repository = new OAuth2AuthorizedClientRepository() {
            @SuppressWarnings("unchecked")
            @Override
            public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String clientRegistrationId, Authentication principal, HttpServletRequest request) {
                return (T) new OAuth2AuthorizedClient(clientRegistration, authentication.getName(), accessToken);
            }

            @Override
            public void saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient, Authentication principal, HttpServletRequest request, HttpServletResponse response) {
            }

            @Override
            public void removeAuthorizedClient(String clientRegistrationId, Authentication principal, HttpServletRequest request, HttpServletResponse response) {
            }
        };

        ProfileController controller = new ProfileController(repository, oboService, new MockEnvironment());
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.profile(authentication, null, model);

        assertEquals("profile", view);
        assertNotNull(model.get("accessTokenJson"));
        assertEquals("not-a-jwt", model.get("accessTokenValue"));
        assertNotNull(model.get("accessTokenScopes"));
    }

    @Test
    void shouldReturnProfileViewWhenPrincipalIsNotOidcUser() {
        EntraIdOboService oboService = mock(EntraIdOboService.class);
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
        }, oboService, new MockEnvironment());
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
        OAuth2AuthorizedClientRepository authorizedClientRepository = mock(OAuth2AuthorizedClientRepository.class);
        EntraIdOboService oboService = mock(EntraIdOboService.class);
        DashboardController controller = new DashboardController(new StatusService(new MockEnvironment(), new RestTemplate()), oboService, authorizedClientRepository);
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.dashboard(new Principal() {
            @Override
            public String getName() {
                return "anonymous";
            }
        }, new MockHttpServletRequest(), model);

        assertEquals("dashboard", view);
        assertNull(model.get("userName"));
        assertNull(model.get("userEmail"));
    }

    @Test
    void shouldHandleMissingBackendConfiguration() {
        OAuth2AuthorizedClientRepository authorizedClientRepository = mock(OAuth2AuthorizedClientRepository.class);
        EntraIdOboService oboService = mock(EntraIdOboService.class);
        DashboardController controller = new DashboardController(new StatusService(new MockEnvironment(), new RestTemplate()), oboService, authorizedClientRepository);
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.dashboard(new Principal() {
            @Override
            public String getName() {
                return "anonymous";
            }
        }, new MockHttpServletRequest(), model);

        assertEquals("dashboard", view);
        assertNotNull(model.get("systemStatusList"));
        assertTrue(((List<?>) model.get("systemStatusList")).isEmpty());
        assertNotNull(model.get("statusConfigMissing"));
        assertEquals(List.of(StatusService.STATUS_API_URL_ENV), model.get("statusConfigMissing"));
        assertNull(model.get("statusFetchError"));
    }

    @Test
    void dashboard_setsOboTokenFailureMessage_whenPrincipalIsAuthenticationButTokenMissing() {
        OAuth2AuthorizedClientRepository authorizedClientRepository = mock(OAuth2AuthorizedClientRepository.class);
        EntraIdOboService oboService = mock(EntraIdOboService.class);
        when(oboService.getPhpApiScope()).thenReturn("api://expected-scope");
        DashboardController controller = new DashboardController(
            new StatusService(new MockEnvironment(), new RestTemplate()), oboService, authorizedClientRepository);

        Authentication authentication = new Authentication() {
            @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(); }
            @Override public Object getCredentials() { return null; }
            @Override public Object getDetails() { return null; }
            @Override public Object getPrincipal() { return new Object(); }
            @Override public boolean isAuthenticated() { return true; }
            @Override public void setAuthenticated(boolean isAuthenticated) {}
            @Override public String getName() { return "tester"; }
        };

        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller.dashboard(authentication, new MockHttpServletRequest(), model);

        assertEquals("dashboard", view);
        assertNotNull(model.get("statusFetchError"));
        assertTrue(((String) model.get("statusFetchError")).contains("Failed to obtain OBO token"));
        assertTrue(((String) model.get("statusFetchError")).contains("api://expected-scope"));
    }

    @Test
    void dashboard_setsStatusFetchErrorFromResponse_whenTokenPresentButStatusCallFails() {
        OAuth2AuthorizedClientRepository authorizedClientRepository = mock(OAuth2AuthorizedClientRepository.class);
        EntraIdOboService oboService = mock(EntraIdOboService.class);
        when(oboService.getOboToken(anyString())).thenReturn("obo-token");
        StatusService statusService = mock(StatusService.class);
        when(statusService.getSystemStatusList("obo-token"))
            .thenReturn(ServiceResponse.error("upstream is down"));

        DashboardController controller = new DashboardController(statusService, oboService, authorizedClientRepository);

        Authentication authentication = new Authentication() {
            @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(); }
            @Override public Object getCredentials() { return null; }
            @Override public Object getDetails() { return null; }
            @Override public Object getPrincipal() { return new Object(); }
            @Override public boolean isAuthenticated() { return true; }
            @Override public void setAuthenticated(boolean isAuthenticated) {}
            @Override public String getName() { return "tester"; }
        };

        ExtendedModelMap model = new ExtendedModelMap();
        controller.dashboard(authentication, new MockHttpServletRequest(), model);

        assertEquals("upstream is down", model.get("statusFetchError"));
    }

    @Test
    void deleteStatus_setsSuccessMessage_whenServiceSucceeds() {
        OAuth2AuthorizedClientRepository authorizedClientRepository = mock(OAuth2AuthorizedClientRepository.class);
        EntraIdOboService oboService = mock(EntraIdOboService.class);
        when(oboService.getOboToken(anyString())).thenReturn("obo-token");
        StatusService statusService = mock(StatusService.class);
        when(statusService.deleteSystemStatus("sys1", "tc1", "obo-token"))
            .thenReturn(ServiceResponse.successMessage("deleted ok"));

        DashboardController controller = new DashboardController(statusService, oboService, authorizedClientRepository);
        RedirectAttributes redirectAttributes = new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        Authentication authentication = new Authentication() {
            @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(); }
            @Override public Object getCredentials() { return null; }
            @Override public Object getDetails() { return null; }
            @Override public Object getPrincipal() { return new Object(); }
            @Override public boolean isAuthenticated() { return true; }
            @Override public void setAuthenticated(boolean isAuthenticated) {}
            @Override public String getName() { return "tester"; }
        };

        String view = controller.deleteStatus("sys1", "tc1", authentication, new MockHttpServletRequest(), redirectAttributes);

        assertEquals("redirect:/dashboard", view);
        assertEquals("deleted ok", redirectAttributes.getFlashAttributes().get("successMessage"));
        assertNull(redirectAttributes.getFlashAttributes().get("errorMessage"));
    }

    @Test
    void deleteStatus_setsErrorMessage_whenServiceReturnsError() {
        OAuth2AuthorizedClientRepository authorizedClientRepository = mock(OAuth2AuthorizedClientRepository.class);
        EntraIdOboService oboService = mock(EntraIdOboService.class);
        when(oboService.getOboToken(anyString())).thenReturn("obo-token");
        StatusService statusService = mock(StatusService.class);
        when(statusService.deleteSystemStatus("sys1", null, "obo-token"))
            .thenReturn(ServiceResponse.error("forbidden"));

        DashboardController controller = new DashboardController(statusService, oboService, authorizedClientRepository);
        RedirectAttributes redirectAttributes = new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        Authentication authentication = new Authentication() {
            @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(); }
            @Override public Object getCredentials() { return null; }
            @Override public Object getDetails() { return null; }
            @Override public Object getPrincipal() { return new Object(); }
            @Override public boolean isAuthenticated() { return true; }
            @Override public void setAuthenticated(boolean isAuthenticated) {}
            @Override public String getName() { return "tester"; }
        };

        String view = controller.deleteStatus("sys1", null, authentication, new MockHttpServletRequest(), redirectAttributes);

        assertEquals("redirect:/dashboard", view);
        assertEquals("forbidden", redirectAttributes.getFlashAttributes().get("errorMessage"));
        assertNull(redirectAttributes.getFlashAttributes().get("successMessage"));
    }

    @Test
    void manage_setsProductFetchErrorWithOboMessage_whenInitialTokenPresentButOboFails() {
        OAuth2AuthorizedClientRepository authorizedClientRepository = mock(OAuth2AuthorizedClientRepository.class);
        EntraIdOboService oboService = mock(EntraIdOboService.class);
        when(oboService.getPhpApiScope()).thenReturn("api://scope-x");

        OAuth2AuthorizedClient authorizedClient = mock(OAuth2AuthorizedClient.class);
        OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "initial-token", Instant.now(), Instant.now().plusSeconds(60));
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(authorizedClientRepository.loadAuthorizedClient(eq("entra"), any(Authentication.class), any()))
            .thenReturn(authorizedClient);
        when(oboService.getOboToken("initial-token")).thenReturn(null);

        StatusService statusService = new StatusService(new MockEnvironment(), new RestTemplate());
        ManageController controller = new ManageController(statusService, oboService, authorizedClientRepository);

        Authentication authentication = new Authentication() {
            @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(); }
            @Override public Object getCredentials() { return null; }
            @Override public Object getDetails() { return null; }
            @Override public Object getPrincipal() { return new Object(); }
            @Override public boolean isAuthenticated() { return true; }
            @Override public void setAuthenticated(boolean isAuthenticated) {}
            @Override public String getName() { return "manager"; }
        };

        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller.manage(authentication, new MockHttpServletRequest(), model);

        assertEquals("manage", view);
        assertNotNull(model.get("productFetchError"));
        assertTrue(((String) model.get("productFetchError")).contains("Failed to obtain OBO token"));
        assertTrue(((String) model.get("productFetchError")).contains("api://scope-x"));
    }

    @Test
    void manage_createSetsErrorMessage_whenServiceReturnsError() {
        OAuth2AuthorizedClientRepository authorizedClientRepository = mock(OAuth2AuthorizedClientRepository.class);
        EntraIdOboService oboService = mock(EntraIdOboService.class);
        when(oboService.getOboToken(anyString())).thenReturn("obo-token");
        StatusService statusService = mock(StatusService.class);
        when(statusService.createProduct(any(), eq("obo-token")))
            .thenReturn(ServiceResponse.error("cannot create"));

        ManageController controller = new ManageController(statusService, oboService, authorizedClientRepository);
        RedirectAttributes redirectAttributes = new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        Authentication authentication = mockAuth();
        String view = controller.createProduct("p", "d", "t", authentication, new MockHttpServletRequest(), redirectAttributes);

        assertEquals("redirect:/manage", view);
        assertEquals("cannot create", redirectAttributes.getFlashAttributes().get("errorMessage"));
    }

    @Test
    void manage_updateSetsSuccessMessage_whenServiceSucceeds() {
        OAuth2AuthorizedClientRepository authorizedClientRepository = mock(OAuth2AuthorizedClientRepository.class);
        EntraIdOboService oboService = mock(EntraIdOboService.class);
        when(oboService.getOboToken(anyString())).thenReturn("obo-token");
        StatusService statusService = mock(StatusService.class);
        when(statusService.updateProduct("p1", "newDesc", "tc", "obo-token"))
            .thenReturn(ServiceResponse.successMessage("updated"));

        ManageController controller = new ManageController(statusService, oboService, authorizedClientRepository);
        RedirectAttributes redirectAttributes = new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        String view = controller.updateProduct("p1", "newDesc", "tc", mockAuth(), new MockHttpServletRequest(), redirectAttributes);

        assertEquals("redirect:/manage", view);
        assertEquals("updated", redirectAttributes.getFlashAttributes().get("successMessage"));
    }

    @Test
    void manage_deleteSetsErrorMessage_whenServiceReturnsError() {
        OAuth2AuthorizedClientRepository authorizedClientRepository = mock(OAuth2AuthorizedClientRepository.class);
        EntraIdOboService oboService = mock(EntraIdOboService.class);
        when(oboService.getOboToken(anyString())).thenReturn("obo-token");
        StatusService statusService = mock(StatusService.class);
        when(statusService.deleteProduct("p1", "obo-token"))
            .thenReturn(ServiceResponse.error("denied"));

        ManageController controller = new ManageController(statusService, oboService, authorizedClientRepository);
        RedirectAttributes redirectAttributes = new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        String view = controller.deleteProduct("p1", mockAuth(), new MockHttpServletRequest(), redirectAttributes);

        assertEquals("redirect:/manage", view);
        assertEquals("denied", redirectAttributes.getFlashAttributes().get("errorMessage"));
    }

    private static Authentication mockAuth() {
        return new Authentication() {
            @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(); }
            @Override public Object getCredentials() { return null; }
            @Override public Object getDetails() { return null; }
            @Override public Object getPrincipal() { return new Object(); }
            @Override public boolean isAuthenticated() { return true; }
            @Override public void setAuthenticated(boolean isAuthenticated) {}
            @Override public String getName() { return "tester"; }
        };
    }
}
