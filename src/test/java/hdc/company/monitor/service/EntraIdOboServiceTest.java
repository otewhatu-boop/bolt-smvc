package hdc.company.monitor.service;

import hdc.company.monitor.config.EntraIdProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntraIdOboServiceTest {

    @Mock
    private EntraIdProperties properties;

    @Mock
    private RestTemplate restTemplate;

    private EntraIdOboService oboService;

    @BeforeEach
    void setUp() {
        oboService = new EntraIdOboService(properties, restTemplate);
    }

    @Test
    void getOboToken_returnsNull_whenPropertiesNotConfigured() {
        when(properties.isConfigured()).thenReturn(false);

        String result = oboService.getOboToken("user-token");

        assertNull(result);
        verifyNoInteractions(restTemplate);
    }

    @Test
    void getOboToken_returnsNull_whenUserAccessTokenIsNull() {
        when(properties.isConfigured()).thenReturn(true);

        String result = oboService.getOboToken(null);

        assertNull(result);
        verifyNoInteractions(restTemplate);
    }

    @Test
    void getOboToken_returnsNull_whenUserAccessTokenIsBlank() {
        when(properties.isConfigured()).thenReturn(true);

        String result = oboService.getOboToken("   ");

        assertNull(result);
        verifyNoInteractions(restTemplate);
    }

    @Test
    void getOboToken_returnsToken_whenExchangeSucceeds() {
        configureValidProperties();
        when(properties.isConfigured()).thenReturn(true);

        Map<String, Object> body = new HashMap<>();
        body.put("access_token", "obo-token-value");
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
            .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        String result = oboService.getOboToken("user-token");

        assertEquals("obo-token-value", result);
    }

    @Test
    void getOboToken_returnsNull_whenResponseSuccessfulButAccessTokenMissing() {
        configureValidProperties();
        when(properties.isConfigured()).thenReturn(true);

        Map<String, Object> body = new HashMap<>();
        body.put("token_type", "Bearer");
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
            .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        String result = oboService.getOboToken("user-token");

        assertNull(result);
    }

    @Test
    void getOboToken_returnsNull_whenResponseIs4xx() {
        configureValidProperties();
        when(properties.isConfigured()).thenReturn(true);

        Map<String, Object> body = new HashMap<>();
        body.put("error", "invalid_grant");
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
            .thenReturn(new ResponseEntity<>(body, HttpStatus.BAD_REQUEST));

        String result = oboService.getOboToken("user-token");

        assertNull(result);
    }

    @Test
    void getOboToken_returnsNull_whenResponseBodyIsNull() {
        configureValidProperties();
        when(properties.isConfigured()).thenReturn(true);

        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
            .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        String result = oboService.getOboToken("user-token");

        assertNull(result);
    }

    @Test
    void getOboToken_returnsNull_whenRestTemplateThrowsException() {
        configureValidProperties();
        when(properties.isConfigured()).thenReturn(true);

        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
            .thenThrow(new RestClientException("connection refused"));

        String result = oboService.getOboToken("user-token");

        assertNull(result);
    }

    @Test
    void getOboToken_buildsCorrectFormUrlencodedBodyAndUrl() {
        configureValidProperties();
        when(properties.isConfigured()).thenReturn(true);

        Map<String, Object> body = new HashMap<>();
        body.put("access_token", "obo-token-value");
        ArgumentCaptor<HttpEntity<?>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        when(restTemplate.postForEntity(urlCaptor.capture(), entityCaptor.capture(), eq(Map.class)))
            .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        oboService.getOboToken("user-token");

        assertEquals("https://login.microsoftonline.com/test-tenant/oauth2/v2.0/token", urlCaptor.getValue());

        HttpEntity<?> entity = entityCaptor.getValue();
        assertEquals(MediaType.APPLICATION_FORM_URLENCODED, entity.getHeaders().getContentType());
        assertEquals(MediaType.APPLICATION_JSON, entity.getHeaders().getAccept().get(0));

        @SuppressWarnings("unchecked")
        MultiValueMap<String, String> formBody = (MultiValueMap<String, String>) entity.getBody();
        assertNotNull(formBody);
        assertEquals("urn:ietf:params:oauth:grant-type:jwt-bearer", formBody.getFirst("grant_type"));
        assertEquals("test-client-id", formBody.getFirst("client_id"));
        assertEquals("test-secret", formBody.getFirst("client_secret"));
        assertEquals("user-token", formBody.getFirst("assertion"));
        assertEquals("api://php-scope", formBody.getFirst("scope"));
        assertEquals("on_behalf_of", formBody.getFirst("requested_token_use"));
    }

    @Test
    void getPhpApiScope_delegatesToProperties() {
        when(properties.getPhpApiScope()).thenReturn("api://custom-scope");

        assertEquals("api://custom-scope", oboService.getPhpApiScope());
    }

    private void configureValidProperties() {
        when(properties.getTenantId()).thenReturn("test-tenant");
        when(properties.getClientId()).thenReturn("test-client-id");
        when(properties.getClientSecret()).thenReturn("test-secret");
        when(properties.getPhpApiScope()).thenReturn("api://php-scope");
    }
}
