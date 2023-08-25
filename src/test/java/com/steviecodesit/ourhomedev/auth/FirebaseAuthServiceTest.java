package com.steviecodesit.ourhomedev.auth;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.steviecodesit.ourhomedev.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

public class FirebaseAuthServiceTest {

    private FirebaseAuthService firebaseAuthService;
    private FirebaseAuth firebaseAuth;
    private UserService userService;

    @BeforeEach
    public void setUp() {
        firebaseAuth = mock(FirebaseAuth.class);
        userService = mock(UserService.class);
        firebaseAuthService = new FirebaseAuthService(firebaseAuth, userService);
    }

    @Test
    void testRegisterUser() throws FirebaseAuthException {
        String email = "test@example.com";
        String password = "password";
        String displayName = "Test User";

        UserRecord userRecord = mock(UserRecord.class);

        // Use any() matcher to ignore the actual argument
        when(firebaseAuth.createUser(any(UserRecord.CreateRequest.class))).thenReturn(userRecord);
        // Call the method under test
        UserRecord returnedUserRecord = firebaseAuthService.registerUser(email, password, displayName);

        // Verify interactions with mock objects
        verify(firebaseAuth).createUser(any(UserRecord.CreateRequest.class));
        verify(userService).saveUser(userRecord);

        // Assert the result
        assertEquals(userRecord, returnedUserRecord);
    }
}
