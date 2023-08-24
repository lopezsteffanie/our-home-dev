package com.steviecodesit.ourhomedev.user;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.steviecodesit.ourhomedev.auth.FirebaseAuthService;
import com.steviecodesit.ourhomedev.auth.LoginRequest;
import com.steviecodesit.ourhomedev.auth.RegistrationRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class UserController {

    private final FirebaseAuthService firebaseAuthService;
    private final UserService userService;
    private final FirebaseAuth firebaseAuth;

    public UserController(FirebaseAuthService firebaseAuthService, UserService userService, FirebaseAuth firebaseAuth) {
        this.firebaseAuthService = firebaseAuthService;
        this.userService = userService;
        this.firebaseAuth = firebaseAuth;
    }


    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody RegistrationRequest registrationRequest) {
        try {
            // Validate input data
            if (registrationRequest.getEmail() == null || registrationRequest.getEmail().isEmpty() ||
                    registrationRequest.getPassword() == null || registrationRequest.getPassword().isEmpty() ||
                    registrationRequest.getUsername() == null || registrationRequest.getUsername().isEmpty()) {
                return ResponseEntity.badRequest().body("Email, password, and userName are required.");
            }

            // Check if the display name is unique
            if (!userService.isDisplayNameUnique(registrationRequest.getUsername())) {
                return ResponseEntity.badRequest().body("Display name is already taken.");
            }

            UserRecord userRecord = firebaseAuthService.registerUser(registrationRequest.getEmail(), registrationRequest.getPassword(), registrationRequest.getUsername());

            // Save userRecord to Firestore
            userService.saveUser(userRecord, registrationRequest.getUsername());

            // Generate custom token
            String customToken = firebaseAuth.createCustomToken(userRecord.getUid());

            return ResponseEntity.ok("User registered successfully! " + customToken);
        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Registration failed.");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<String> loginUser(@RequestBody LoginRequest loginRequest) {
        try {
            UserRecord userRecord = firebaseAuth.getUserByEmail(loginRequest.getEmail());

            // Check if the user is already logged in
            if (userService.isUserLoggedIn(userRecord.getUid())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User is already logged in.");
            }

            // Update isLoggedIn to true for the logged-in user
             userService.updateIsLoggedInStatus(userRecord.getUid(), true);

            // Generate custom token
            String customToken = firebaseAuth.createCustomToken(userRecord.getUid());

            return ResponseEntity.ok("User logged in successfully! " + customToken);
        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logoutUser(@RequestHeader("Authorization") String idToken) {
        try {
            // Verify and decode the Firebase ID token
            FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken);
            String userId = decodedToken.getUid();

            // Update isLoggedIn to false for the logged-out user
            userService.updateIsLoggedInStatus(userId, false);

            return ResponseEntity.ok("User logged out successfully!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Logout failed");
        }
    }
}
