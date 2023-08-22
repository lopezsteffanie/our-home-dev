package com.steviecodesit.ourhomedev.config.filter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class CustomFilterTest {

    private CustomFilter customFilter;
    private FirebaseAuth firebaseAuth;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    public void setUp() {
        firebaseAuth = mock(FirebaseAuth.class);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
        customFilter = new CustomFilter(firebaseAuth);
    }

    @Test
    void testPublicEndpoint() throws Exception {
        when(request.getRequestURI()).thenReturn("/public-endpoints/test");

        customFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void testAuthenticationFailure() throws Exception {
        when(request.getRequestURI()).thenReturn("/private-endpoints/test");
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid_token");
        when(firebaseAuth.verifyIdToken(anyString())).thenThrow(FirebaseAuthException.class);

        StringWriter responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        customFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertEquals("Authentication failed", responseWriter.toString());
    }

    @Test
    void testAuthenticationSuccess() throws Exception {
        String token = "valid_token";
        FirebaseToken decodedToken = mock(FirebaseToken.class);
        when(decodedToken.getUid()).thenReturn("uid");
        when(decodedToken.getEmail()).thenReturn("email");
        when(decodedToken.getName()).thenReturn("displayName");

        when(request.getRequestURI()).thenReturn("/private-endpoints/test");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(firebaseAuth.verifyIdToken(token)).thenReturn(decodedToken);

        customFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
