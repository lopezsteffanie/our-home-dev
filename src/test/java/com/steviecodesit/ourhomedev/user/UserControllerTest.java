package com.steviecodesit.ourhomedev.user;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.steviecodesit.ourhomedev.auth.FirebaseAuthService;
import com.steviecodesit.ourhomedev.request.LoginRequest;
import com.steviecodesit.ourhomedev.request.RegistrationRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class UserControllerTest {

    private UserController userController;
    private FirebaseAuthService firebaseAuthService;
    private FirebaseAuth firebaseAuth;
    private UserService userService;

    @BeforeEach
    public void setUp() {
        firebaseAuthService = mock(FirebaseAuthService.class);
        userService = mock(UserService.class);
        firebaseAuth = mock(FirebaseAuth.class);
        userController = new UserController(firebaseAuthService, userService, firebaseAuth);
    }

    @Test
    public void registerUser_Success() throws FirebaseAuthException {
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail("email@example.com");
        request.setPassword("Password1!");
        request.setUsername("username");

        UserRecord userRecord = mock(UserRecord.class);
        when(userRecord.getUid()).thenReturn("uid123");
        when(userService.isValidPassword(anyString())).thenReturn(true);
        when(userService.isDisplayNameUnique(anyString())).thenReturn(true);
        when(userService.isEmailUnique(anyString())).thenReturn(true);
        when(firebaseAuthService.registerUser(anyString(), anyString(), anyString())).thenReturn(userRecord);
        when(firebaseAuth.createCustomToken("uid123")).thenReturn("customToken");

        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        ResponseEntity<String> response = userController.registerUser(request, mockResponse);

        // Verify the response status and body
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User registered successfully!", response.getBody());

        // Verify that the cookie was added to the response
        verify(mockResponse).addCookie(argThat(cookie ->
                "customToken".equals(cookie.getName()) &&
                        "customToken".equals(cookie.getValue()) &&
                        cookie.isHttpOnly() &&
                        cookie.getSecure() &&
                        cookie.getMaxAge() == 7 * 24 * 60 * 60
        ));
    }

    @Test
    public void registerUser_EmptyRegistration() {
        RegistrationRequest request = new RegistrationRequest();

        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        ResponseEntity<String> response = userController.registerUser(request, mockResponse);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Email, password, and userName are required.", response.getBody());

        verifyNoInteractions(mockResponse);
    }

    @Test
    public void registerUser_InvalidPassword() {
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail("email@example.com");
        request.setPassword("password123");
        request.setUsername("username");

        when(userService.isValidPassword(anyString())).thenReturn(false);

        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        ResponseEntity<String> response = userController.registerUser(request, mockResponse);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Password must meet the required criteria.", response.getBody());

        verifyNoInteractions(mockResponse);
    }

    @Test
    public void registerUser_DisplayNameNotUnique() {
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail("email@example.com");
        request.setPassword("Password1!");
        request.setUsername("username");

        when(userService.isValidPassword(anyString())).thenReturn(true);
        when(userService.isDisplayNameUnique(anyString())).thenReturn(false);

        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        ResponseEntity<String> response = userController.registerUser(request, mockResponse);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Display name is already taken.", response.getBody());

        verifyNoInteractions(mockResponse);
    }

    @Test
    public void registerUser_EmailNotUnique() {
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail("email@example.com");
        request.setPassword("Password1!");
        request.setUsername("username");

        when(userService.isValidPassword(anyString())).thenReturn(true);
        when(userService.isDisplayNameUnique(anyString())).thenReturn(true);
        when(userService.isEmailUnique(anyString())).thenReturn(false);

        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        ResponseEntity<String> response = userController.registerUser(request, mockResponse);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("A user has already registered with that email.", response.getBody());

        verifyNoInteractions(mockResponse);
    }

    @Test
    public void registerUser_InternalServiceError() throws FirebaseAuthException {
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail("email@example.com");
        request.setPassword("Password1!");
        request.setUsername("username");

        when(userService.isValidPassword(anyString())).thenReturn(true);
        when(userService.isDisplayNameUnique(anyString())).thenReturn(true);
        when(userService.isEmailUnique(anyString())).thenReturn(true);
        when(firebaseAuthService.registerUser(anyString(), anyString(), anyString())).thenThrow(FirebaseAuthException.class);

        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        ResponseEntity<String> response = userController.registerUser(request, mockResponse);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Registration failed.", response.getBody());

        verifyNoInteractions(mockResponse);
    }

    @Test
    public void loginUser_Success() throws FirebaseAuthException {
        LoginRequest request = new LoginRequest();
        request.setEmail("email@example.com");

        UserRecord userRecord = mock(UserRecord.class);
        when(userRecord.getUid()).thenReturn("uid123");
        when(userService.isUserLoggedIn(anyString())).thenReturn(false);
        when(firebaseAuth.getUserByEmail(anyString())).thenReturn(userRecord);
        when(firebaseAuth.createCustomToken("uid123")).thenReturn("customToken");

        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        ResponseEntity<String> response = userController.loginUser(request, mockResponse);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User logged in successfully!", response.getBody());

        verify(mockResponse).addCookie(argThat(cookie ->
                "customToken".equals(cookie.getName()) &&
                        "customToken".equals(cookie.getValue()) &&
                        cookie.isHttpOnly() &&
                        cookie.getSecure() &&
                        cookie.getMaxAge() == 7 * 24 * 60 * 60
        ));
    }

    @Test
    public void loginUser_UserAlreadyLoggedIn() throws FirebaseAuthException {
        LoginRequest request = new LoginRequest();
        request.setEmail("email@example.com");

        UserRecord userRecord = mock(UserRecord.class);
        when(userRecord.getUid()).thenReturn("uid123");
        when(userService.isUserLoggedIn(anyString())).thenReturn(true);

        when(firebaseAuth.getUserByEmail(request.getEmail())).thenReturn(userRecord);

        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        ResponseEntity<String> response = userController.loginUser(request, mockResponse);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("User is already logged in.", response.getBody());

        verifyNoInteractions(mockResponse);
    }

    @Test
    public void loginUser_UnauthorizedRequest() throws FirebaseAuthException {
        LoginRequest request = new LoginRequest();
        request.setEmail("email@example.com");

        UserRecord userRecord = mock(UserRecord.class);
        when(userRecord.getUid()).thenReturn("uid123");
        when(userService.isUserLoggedIn(anyString())).thenReturn(false);
        when(firebaseAuth.getUserByEmail(anyString())).thenThrow(FirebaseAuthException.class);

        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        ResponseEntity<String> response = userController.loginUser(request, mockResponse);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        verifyNoInteractions(mockResponse);
    }

    @Test
    public void logoutUser_Success() throws Exception {
        String uid = "uid123";
        String token = "someToken";

        FirebaseToken firebaseToken = mock(FirebaseToken.class);
        when(firebaseToken.getUid()).thenReturn(uid);
        when(firebaseAuth.verifyIdToken(token)).thenReturn(firebaseToken);

        MockHttpServletResponse mockResponse = new MockHttpServletResponse();
        ResponseEntity<String> response = userController.logoutUser(token, mockResponse);

        Cookie[] cookies = mockResponse.getCookies();
        Cookie customTokenCookie = Arrays.stream(cookies)
                .filter(cookie -> "customToken".equals(cookie.getName()))
                .findFirst()
                .orElse(null);

        assertNotNull(customTokenCookie);
        assertEquals(0, customTokenCookie.getMaxAge());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User logged out successfully!", response.getBody());

        verify(userService).updateIsLoggedInStatus(uid, false);
    }

    @Test
    public void logoutUser_InternalServiceError() throws Exception {
        String token = "someToken";

        when(firebaseAuth.verifyIdToken(token)).thenThrow(RuntimeException.class);

        MockHttpServletResponse mockResponse = new MockHttpServletResponse();
        ResponseEntity<String> response = userController.logoutUser(token, mockResponse);

        Cookie[] cookies = mockResponse.getCookies();
        Cookie customTokenCookie = Arrays.stream(cookies)
                .filter(cookie -> "customToken".equals(cookie.getName()))
                .findFirst()
                .orElse(null);


        assertNull(customTokenCookie);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Logout failed", response.getBody());
    }
}
