package com.steviecodesit.ourhomedev.user;

import com.google.cloud.firestore.*;
import com.google.firebase.auth.UserRecord;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
public class UserService {

    private final Firestore firestore;

    public UserService(Firestore firestore) {
        this.firestore = firestore;
    }

    public void saveUser(UserRecord userRecord, String displayName) {
        // Map UserRecord data to your User model
        User user = new User();
        user.setEmail(userRecord.getEmail());
        user.setDisplayName(displayName);
        user.setLoggedIn(true);

        // Save the user to Firestore
        CollectionReference usersCollection = firestore.collection("users");
        DocumentReference userDocument = usersCollection.document(userRecord.getUid());

        // Use the Firestore API to set the user data in the document
        userDocument.set(user);
    }

    public void updateIsLoggedInStatus(String userId, boolean isLoggedIn) {
        DocumentReference userDocument = firestore.collection("users").document(userId);
        userDocument.update("loggedIn", isLoggedIn);
    }

    public boolean isUserLoggedIn(String userId) {
        DocumentReference userDocument = firestore.collection("users").document(userId);
        try {
            DocumentSnapshot documentSnapshot = userDocument.get().get();
            if (documentSnapshot.exists()) {
                User user = documentSnapshot.toObject(User.class);
                return user != null && user.isLoggedIn();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Preserve the interruption status
            // Log the exception or take appropriate action
            System.err.println("InterruptedException occurred: " + e.getMessage());
        } catch (ExecutionException e) {
            // Log the exception or take appropriate action
            System.err.println("ExecutionException occurred: " + e.getMessage());
        }
        return false;
    }

    public boolean isDisplayNameUnique(String displayName) {
        CollectionReference usersCollection = firestore.collection("users");

        try {
            // Query Firestore to check if any document has the same display name
            Query query = usersCollection.whereEqualTo("displayName", displayName).limit(1);
            QuerySnapshot querySnapshot = query.get().get();

            return querySnapshot.isEmpty(); // Return true if no documents match
        } catch (InterruptedException | ExecutionException e) {
            // Log the exception or take appropriate action
            System.err.println("Exception occurred: " + e.getMessage());
            return false;
        }
    }
}
