package com.steviecodesit.ourhomedev.user;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.UserRecord;
import org.springframework.stereotype.Service;

@Service
public class UserServicce {

    private final CollectionReference usersCollection;

    public UserServicce(Firestore firestore) {
        this.usersCollection = firestore.collection("users");
    }

    public void saveUser(UserRecord userRecord) {
        // Map userRecord data to User model (create User class)
        // Save the User model to Firestore
    }
}
