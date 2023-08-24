package com.steviecodesit.ourhomedev.user;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.steviecodesit.ourhomedev.auth.FirebaseAuthService;
import com.steviecodesit.ourhomedev.auth.LoginRequest;
import com.steviecodesit.ourhomedev.auth.RegistrationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        request.setPassword("password123");
        request.setUsername("username");

        UserRecord userRecord = mock(UserRecord.class);
        when(userRecord.getUid()).thenReturn("uid123");
        when(userService.isDisplayNameUnique(anyString())).thenReturn(true);
        when(firebaseAuthService.registerUser(anyString(), anyString(), anyString())).thenReturn(userRecord);
        when(firebaseAuth.createCustomToken("uid123")).thenReturn("customToken");

        ResponseEntity<String> response = userController.registerUser(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User registered successfully! customToken", response.getBody());
    }

    @Test
    public void registerUser_EmptyRegistration() throws FirebaseAuthException {
        RegistrationRequest request = new RegistrationRequest();

        ResponseEntity<String> response = userController.registerUser(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Email, password, and userName are required.", response.getBody());
    }

    @Test
    public void registerUser_DisplayNameNotUnique() throws FirebaseAuthException {
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail("email@example.com");
        request.setPassword("password123");
        request.setUsername("username");

        when(userService.isDisplayNameUnique(anyString())).thenReturn(false);

        ResponseEntity<String> response = userController.registerUser(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Display name is already taken.", response.getBody());
    }

    @Test
    public void registerUser_InternalServiceError() throws FirebaseAuthException {
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail("email@example.com");
        request.setPassword("password123");
        request.setUsername("username");

        when(userService.isDisplayNameUnique(anyString())).thenReturn(true);
        when(firebaseAuthService.registerUser(anyString(), anyString(), anyString())).thenThrow(FirebaseAuthException.class);

        ResponseEntity<String> response = userController.registerUser(request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Registration failed.", response.getBody());
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

        ResponseEntity<String> response = userController.loginUser(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User logged in successfully! customToken", response.getBody());
    }

    @Test
    public void loginUser_UserAlreadyLoggedIn() throws FirebaseAuthException {
        LoginRequest request = new LoginRequest();
        request.setEmail("email@example.com");

        UserRecord userRecord = mock(UserRecord.class);
        when(userRecord.getUid()).thenReturn("uid123");
        when(userService.isUserLoggedIn(anyString())).thenReturn(true);

        when(firebaseAuth.getUserByEmail(request.getEmail())).thenReturn(userRecord);

        ResponseEntity<String> response = userController.loginUser(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("User is already logged in.", response.getBody());
    }

    @Test
    public void loginUser_UnauthorizedRequest() throws FirebaseAuthException {
        LoginRequest request = new LoginRequest();
        request.setEmail("email@example.com");

        UserRecord userRecord = mock(UserRecord.class);
        when(userRecord.getUid()).thenReturn("uid123");
        when(userService.isUserLoggedIn(anyString())).thenReturn(false);
        when(firebaseAuth.getUserByEmail(anyString())).thenThrow(FirebaseAuthException.class);

        ResponseEntity<String> response = userController.loginUser(request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    public void logoutUser_Success() throws Exception {
        String uid = "uid123";
        String token = "someToken";

        FirebaseToken firebaseToken = mock(FirebaseToken.class);
        when(firebaseToken.getUid()).thenReturn(uid);
        when(firebaseAuth.verifyIdToken(token)).thenReturn(firebaseToken);

        ResponseEntity<String> response = userController.logoutUser(token);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User logged out successfully!", response.getBody());

        verify(userService).updateIsLoggedInStatus(uid, false);
    }

    @Test
    public void logoutUser_InternalServiceError() throws Exception {
        String token = "someToken";

        when(firebaseAuth.verifyIdToken(token)).thenThrow(RuntimeException.class);

        ResponseEntity<String> response = userController.logoutUser(token);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Logout failed", response.getBody());
    }
}
