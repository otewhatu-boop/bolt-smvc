package hdc.company.monitor.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CorrelationFilterTest {

    private final CorrelationFilter filter = new CorrelationFilter();

    @Test
    void shouldUseIncomingCorrelationIdHeader() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationConstants.HEADER_NAME, "incoming-id");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals("incoming-id", response.getHeader(CorrelationConstants.HEADER_NAME));
    }

    @Test
    void shouldGenerateCorrelationIdWhenHeaderAbsent() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        String responseId = response.getHeader(CorrelationConstants.HEADER_NAME);
        assertNotNull(responseId);
        assertFalse(responseId.isBlank());
    }

    @Test
    void shouldGenerateCorrelationIdWhenHeaderBlank() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationConstants.HEADER_NAME, "   ");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        String responseId = response.getHeader(CorrelationConstants.HEADER_NAME);
        assertNotNull(responseId);
        assertFalse(responseId.isBlank());
        assertNotEquals("   ", responseId);
    }

    @Test
    void shouldClearMdcAfterRequest() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationConstants.HEADER_NAME, "clean-id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertNull(MDC.get(CorrelationConstants.MDC_KEY));
    }

    @Test
    void shouldClearMdcEvenWhenChainThrows() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain throwingChain = (req, res) -> {
            throw new ServletException("chain error");
        };

        assertThrows(ServletException.class, () -> filter.doFilter(request, response, throwingChain));
        assertNull(MDC.get(CorrelationConstants.MDC_KEY));
    }

    @Test
    void shouldPassThroughForNonHttpRequests() throws IOException, ServletException {
        FilterChain chain = mock(FilterChain.class);
        jakarta.servlet.ServletRequest nonHttpRequest = mock(jakarta.servlet.ServletRequest.class);
        jakarta.servlet.ServletResponse nonHttpResponse = mock(jakarta.servlet.ServletResponse.class);

        filter.doFilter(nonHttpRequest, nonHttpResponse, chain);

        verify(chain).doFilter(nonHttpRequest, nonHttpResponse);
    }

    @Test
    void initAndDestroyShouldNotThrow() throws ServletException {
        assertDoesNotThrow(() -> filter.init(null));
        assertDoesNotThrow(() -> filter.destroy());
    }
}
