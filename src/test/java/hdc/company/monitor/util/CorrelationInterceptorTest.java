package hdc.company.monitor.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CorrelationInterceptorTest {

    private CorrelationInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new CorrelationInterceptor();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void intercept_addsCorrelationIdHeader_whenMdcHasValue() throws IOException {
        MDC.put(CorrelationConstants.MDC_KEY, "trace-id-1");
        HttpRequest request = mock(HttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
        byte[] body = new byte[0];
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse expectedResponse = mock(ClientHttpResponse.class);
        when(execution.execute(request, body)).thenReturn(expectedResponse);

        ClientHttpResponse result = interceptor.intercept(request, body, execution);

        assertSame(expectedResponse, result);
        assertEquals("trace-id-1", headers.getFirst(CorrelationConstants.HEADER_NAME));
    }

    @Test
    void intercept_omitsHeader_whenMdcValueMissing() throws IOException {
        HttpRequest request = mock(HttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
        byte[] body = new byte[0];
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse expectedResponse = mock(ClientHttpResponse.class);
        when(execution.execute(request, body)).thenReturn(expectedResponse);

        ClientHttpResponse result = interceptor.intercept(request, body, execution);

        assertSame(expectedResponse, result);
        assertNull(headers.getFirst(CorrelationConstants.HEADER_NAME));
    }

    @Test
    void intercept_omitsHeader_whenMdcValueBlank() throws IOException {
        MDC.put(CorrelationConstants.MDC_KEY, "   ");
        HttpRequest request = mock(HttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
        byte[] body = new byte[0];
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse expectedResponse = mock(ClientHttpResponse.class);
        when(execution.execute(request, body)).thenReturn(expectedResponse);

        ClientHttpResponse result = interceptor.intercept(request, body, execution);

        assertSame(expectedResponse, result);
        assertNull(headers.getFirst(CorrelationConstants.HEADER_NAME));
    }
}
