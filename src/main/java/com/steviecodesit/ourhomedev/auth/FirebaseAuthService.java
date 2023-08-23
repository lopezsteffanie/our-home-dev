package com.steviecodesit.ourhomedev.auth;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.steviecodesit.ourhomedev.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Email;
import javax.validation.constraints.Size;

@Service
@Validated
public class FirebaseAuthService {

    private final UserService userService;
    private final FirebaseAuth firebaseAuth;

    @Autowired
    public FirebaseAuthService(FirebaseAuth firebaseAuth, UserService userService) {
        this.firebaseAuth = firebaseAuth;
        this.userService = userService;
    }

    public UserRecord registerUser(@Email String email, @Size(min = 6) String password, String displayName) throws FirebaseAuthException {
        UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                .setEmail(email)
                .setPassword(password)
                .setDisplayName(displayName);

        UserRecord userRecord = firebaseAuth.createUser(request);
        userService.saveUser(userRecord, displayName);

        return userRecord;
    }
}
