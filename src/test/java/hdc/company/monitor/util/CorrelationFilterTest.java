package hdc.company.monitor.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CorrelationFilterTest {

    private CorrelationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelationFilter();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void doFilter_propagatesCorrelationIdFromHeader_whenPresent() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationConstants.HEADER_NAME, "abc-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals("abc-123", response.getHeader(CorrelationConstants.HEADER_NAME));
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_populatesMdcDuringChain_whenHeaderPresent() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationConstants.HEADER_NAME, "mdc-id");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        doAnswer(invocation -> {
            assertEquals("mdc-id", MDC.get(CorrelationConstants.MDC_KEY));
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilter(request, response, chain);
    }

    @Test
    void doFilter_generatesCorrelationId_whenHeaderMissing() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        String generated = response.getHeader(CorrelationConstants.HEADER_NAME);
        assertNotNull(generated);
        assertFalse(generated.isBlank());
    }

    @Test
    void doFilter_generatesCorrelationId_whenHeaderBlank() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationConstants.HEADER_NAME, "   ");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        String generated = response.getHeader(CorrelationConstants.HEADER_NAME);
        assertNotNull(generated);
        assertFalse(generated.isBlank());
        assertNotEquals("   ", generated);
    }

    @Test
    void doFilter_clearsMdcAfterChain_whenFilterCompletes() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationConstants.HEADER_NAME, "to-clear");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertNull(MDC.get(CorrelationConstants.MDC_KEY));
    }

    @Test
    void doFilter_stillClearsMdc_whenChainThrowsException() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationConstants.HEADER_NAME, "will-throw");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        doThrow(new RuntimeException("chain failed")).when(chain).doFilter(any(), any());

        assertThrows(RuntimeException.class, () -> filter.doFilter(request, response, chain));
        assertNull(MDC.get(CorrelationConstants.MDC_KEY));
    }

    @Test
    void doFilter_propagatesRequest_whenNotHttpServletRequest() throws IOException, ServletException {
        ServletRequest request = mock(ServletRequest.class);
        ServletResponse response = mock(ServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertNull(MDC.get(CorrelationConstants.MDC_KEY));
    }

    @Test
    void init_andDestroy_doNotThrow() {
        assertDoesNotThrow(() -> filter.init(new MockFilterConfig()));
        assertDoesNotThrow(() -> filter.destroy());
    }
}
