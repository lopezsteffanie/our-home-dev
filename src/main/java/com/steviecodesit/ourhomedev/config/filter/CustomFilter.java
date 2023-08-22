package com.steviecodesit.ourhomedev.config.filter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.steviecodesit.ourhomedev.user.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

public class CustomFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(CustomFilter.class);
    private final FirebaseAuth firebaseAuth;

    public CustomFilter(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // Log authentication attempt
        logger.debug("Authentication attempt from IP: {}", request.getRemoteAddr());

        String path = request.getRequestURI();

        // Public endpoints are accessible without authentication
        if (path.startsWith("/public-endpoints/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Your logic to authenticate with Firebase (replace with your implementation)
        Authentication authentication = authenticateWithFirebase(request);

        if (authentication != null) {
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);

            // Log successful authentication
            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                User user = (User) principal;
                logger.info("Successful authentication for user: {}", user.getUsername()); // Or any other user details you wish to log
            }
        } else {
            // Log failed authentication
            logger.warn("Failed authentication attempt");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            // Optionally, you can include a message to inform the client
            response.getWriter().write("Authentication failed");
        }
    }

    private Authentication authenticateWithFirebase(HttpServletRequest request) {
        // Extract the token from the request header
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7); // Remove "Bearer " prefix
        }

        if (token == null) {
            return null; // No token provided
        }

        try {
            // Verify the token with Firebase
            FirebaseToken decodedToken = firebaseAuth.verifyIdToken(token);

            // Extract the user's UID and other information from the decoded token
            String uid = decodedToken.getUid();
            String email = decodedToken.getEmail();
            String displayName = decodedToken.getName();

            // You can create a custom User object that holds information specific to your application
            User user = new User(uid, email, displayName);

            // Create a Spring Security Authentication object that represents the authenticated user
            Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());

            return authentication;
        } catch (FirebaseAuthException e) {
            // Token was invalid or other Firebase authentication error occurred
            logger.warn("Failed authentication attempt using token: {}", token);
            return null;
        }
    }
}
