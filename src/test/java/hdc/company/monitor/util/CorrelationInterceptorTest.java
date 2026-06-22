package hdc.company.monitor.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;

import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CorrelationInterceptorTest {

    private final CorrelationInterceptor interceptor = new CorrelationInterceptor();

    @AfterEach
    void clearMdc() {
        MDC.remove(CorrelationConstants.MDC_KEY);
    }

    @Test
    void shouldAddCorrelationIdHeaderWhenMdcValuePresent() throws IOException {
        MDC.put(CorrelationConstants.MDC_KEY, "test-correlation-id");

        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://example.com"));
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse mockResponse = mock(ClientHttpResponse.class);
        when(execution.execute(any(), any())).thenReturn(mockResponse);

        interceptor.intercept(request, new byte[0], execution);

        assertEquals("test-correlation-id", request.getHeaders().getFirst(CorrelationConstants.HEADER_NAME));
        verify(execution).execute(request, new byte[0]);
    }

    @Test
    void shouldNotAddHeaderWhenMdcValueAbsent() throws IOException {
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://example.com"));
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse mockResponse = mock(ClientHttpResponse.class);
        when(execution.execute(any(), any())).thenReturn(mockResponse);

        interceptor.intercept(request, new byte[0], execution);

        assertFalse(request.getHeaders().containsKey(CorrelationConstants.HEADER_NAME));
        verify(execution).execute(request, new byte[0]);
    }

    @Test
    void shouldNotAddHeaderWhenMdcValueBlank() throws IOException {
        MDC.put(CorrelationConstants.MDC_KEY, "   ");

        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://example.com"));
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse mockResponse = mock(ClientHttpResponse.class);
        when(execution.execute(any(), any())).thenReturn(mockResponse);

        interceptor.intercept(request, new byte[0], execution);

        assertFalse(request.getHeaders().containsKey(CorrelationConstants.HEADER_NAME));
    }
}
