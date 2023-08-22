package com.steviecodesit.ourhomedev.config.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class RateLimitingFilterTest {

    private RateLimitingFilter rateLimitingFilter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    public void setUp() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
        rateLimitingFilter = new RateLimitingFilter();
    }

    @Test
    public void testRequestWithinLimit() throws IOException, ServletException {
        rateLimitingFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    public void testRateLimitExceeded() throws IOException, ServletException {
        rateLimitingFilter.doFilter(request, response, filterChain);

        StringWriter responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        rateLimitingFilter.doFilter(request, response, filterChain);

        verify(response).setStatus(429);
        assertEquals("Too many requests", responseWriter.toString());
    }
}
