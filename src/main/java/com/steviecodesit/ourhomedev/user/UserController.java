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
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Log4j2
@RestController
@RequestMapping("/api/users")
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
    public ResponseEntity<String> registerUser(@RequestBody RegistrationRequest registrationRequest, HttpServletResponse response) {
        try {
            // Validate input data
            if (registrationRequest.getEmail() == null || registrationRequest.getEmail().isEmpty() ||
                    registrationRequest.getPassword() == null || registrationRequest.getPassword().isEmpty() ||
                    registrationRequest.getUsername() == null || registrationRequest.getUsername().isEmpty()) {
                return ResponseEntity.badRequest().body("Email, password, and userName are required.");
            }

            // Check if password is valid
            String password = registrationRequest.getPassword();
            if (password == null || !userService.isValidPassword(password)) {
                return ResponseEntity.badRequest().body("Password must meet the required criteria.");
            }

            // Check if the display name is unique
            if (!userService.isDisplayNameUnique(registrationRequest.getUsername())) {
                return ResponseEntity.badRequest().body("Display name is already taken.");
            }

            // Check if the email is unique
            if (!userService.isEmailUnique(registrationRequest.getEmail())) {
                return ResponseEntity.badRequest().body("A user has already registered with that email.");
            }

            UserRecord userRecord = firebaseAuthService.registerUser(registrationRequest.getEmail(), registrationRequest.getPassword(), registrationRequest.getUsername());

            // Save userRecord to Firestore
            userService.saveUser(userRecord);

            // Generate custom token
            String customToken = firebaseAuth.createCustomToken(userRecord.getUid());

            // Create a new cookie
            Cookie tokenCookie = new Cookie("customToken", customToken);
            tokenCookie.setHttpOnly(true);  // This makes the cookie HTTP-only
            tokenCookie.setSecure(true);  // This makes the cookie secure (works only over HTTPS)
            tokenCookie.setMaxAge(7 * 24 * 60 * 60); // Sets the cookie's age, e.g., 7 days
            tokenCookie.setPath("/"); // Sets the path for the cookie
            response.addCookie(tokenCookie); // Adds the cookie to the response

            return ResponseEntity.ok("User registered successfully!");
        } catch (FirebaseAuthException e) {
            log.error("Error during registration: ", e);
            return ResponseEntity.internalServerError().body("Registration failed.");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<String> loginUser(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        try {
            UserRecord userRecord = firebaseAuth.getUserByEmail(loginRequest.getEmail());

            // Check if the user is already logged in
            if (userService.isUserLoggedIn(userRecord.getUid())) {
                return ResponseEntity.badRequest().body("User is already logged in.");
            }

            // Update isLoggedIn to true for the logged-in user
             userService.updateIsLoggedInStatus(userRecord.getUid(), true);

            // Generate custom token
            String customToken = firebaseAuth.createCustomToken(userRecord.getUid());

            // Create a new cookie
            Cookie tokenCookie = new Cookie("customToken", customToken);
            tokenCookie.setHttpOnly(true);  // This makes the cookie HTTP-only
            tokenCookie.setSecure(true);  // This makes the cookie secure (works only over HTTPS)
            tokenCookie.setMaxAge(7 * 24 * 60 * 60); // Sets the cookie's age, e.g., 7 days
            tokenCookie.setPath("/"); // Sets the path for the cookie
            response.addCookie(tokenCookie); // Adds the cookie to the response

            return ResponseEntity.ok("User logged in successfully!");
        } catch (FirebaseAuthException e) {
            log.error("Error during login: ", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logoutUser(@RequestHeader("Authorization") String idToken, HttpServletResponse response) {
        try {
            // Verify and decode the Firebase ID token
            FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken);
            String userId = decodedToken.getUid();

            // Update isLoggedIn to false for the logged-out user
            userService.updateIsLoggedInStatus(userId, false);

            // Clear the cookie on successful logout
            Cookie clearTokenCookie = new Cookie("customToken", null);
            clearTokenCookie.setMaxAge(0);
            clearTokenCookie.setHttpOnly(true);
            clearTokenCookie.setSecure(true);
            clearTokenCookie.setPath("/");
            response.addCookie(clearTokenCookie);

            return ResponseEntity.ok("User logged out successfully!");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Logout failed");
        }
    }
}
