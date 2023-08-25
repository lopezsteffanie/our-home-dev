package com.steviecodesit.ourhomedev.user;

import com.google.cloud.firestore.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.steviecodesit.ourhomedev.household.HouseholdMembership;
import com.steviecodesit.ourhomedev.household.HouseholdMembershipStatus;
import com.steviecodesit.ourhomedev.household.HouseholdRole;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class UserService {

    private final Firestore firestore;
    private final FirebaseAuth firebaseAuth;

    public UserService(Firestore firestore, FirebaseAuth firebaseAuth) {
        this.firestore = firestore;
        this.firebaseAuth = firebaseAuth;
    }

    public void saveUser(UserRecord userRecord) {
        // Map UserRecord data to your User model
        User user = new User();
        user.setEmail(userRecord.getEmail());
        user.setDisplayName(user.getDisplayName());
        user.setLoggedIn(true);
        user.setHouseholdMembership(null);

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

    public boolean isEmailUnique(String email) {
        CollectionReference usersCollection = firestore.collection("users");

        try {
            // Query Firestore to check if any document has the same email
            Query query = usersCollection.whereEqualTo("email", email).limit(1);
            QuerySnapshot querySnapshot = query.get().get();

            return querySnapshot.isEmpty(); // Return true if no documents match
        } catch (InterruptedException | ExecutionException e) {
            // Log the exception or take appropriate action
            System.err.println("Exception occurred: " + e.getMessage());
            return false;
        }
    }

    public boolean isValidPassword(String password) {
        // password must contain:
        // at least 8 characters
        // at least one number
        // at least one lowercase letter
        // at least one uppercase letter, at least one special character
        return (password.length() >= 8) && (password.matches(".*\\d.*")) && (password.matches(".*[a-z].*"))
                && (password.matches(".*[A-Z].*")) && (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\",.<>/?].*"));
    }

    public List<User> findUserByEmailOrUsername(String query) {
        CollectionReference usersCollection = firestore.collection("users");
        List<User> matchingUsers = new ArrayList<>();

        // Find users by email
        Query emailQuery = usersCollection.whereEqualTo("email", query);
        try {
            QuerySnapshot emailSnapshot = emailQuery.get().get();
            for (DocumentSnapshot document : emailSnapshot.getDocuments()) {
                matchingUsers.add(document.toObject(User.class));
            }
        } catch (InterruptedException | ExecutionException e) {
            // Handle exceptions
        }

        // Find users by username (avoid duplicates if any user matches both email and username)
        Query usernameQuery = usersCollection.whereEqualTo("displayName", query);
        try {
            QuerySnapshot usernameSnapshot = usernameQuery.get().get();
            for (DocumentSnapshot document : usernameSnapshot.getDocuments()) {
                User user = document.toObject(User.class);
                if (!matchingUsers.contains(user)) {
                    matchingUsers.add(user);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            // Handle exceptions
        }

        return matchingUsers;
    }

    public void addMembershipToUser(String userId, String householdId, HouseholdRole role, HouseholdMembershipStatus status) throws Exception {
        DocumentReference userRef = firestore.collection("users").document(userId);
        User user = userRef.get().get().toObject(User.class);

        if (user != null) {
            HouseholdMembership membership = user.getHouseholdMembership();
            membership.setHouseholdId(householdId);
            membership.setUserId(userId);
            membership.setHouseholdRole(role);
            membership.setMemberStatus(status);

            userRef.set(user);
        }
    }

    public void removeMembershipFromUser(String userId, String householdId) throws Exception {
        DocumentReference userRef = firestore.collection("users").document(userId);
        User user = userRef.get().get().toObject(User.class);

        if (user.getHouseholdMembership() != null && user.getHouseholdMembership().getHouseholdId().equals(householdId)) {
            user.setHouseholdMembership(null);
        }

        userRef.set(user);
    }

    public String verifyTokenAndGetUserId(String userIdToken) throws FirebaseAuthException {
        FirebaseToken decodedToken = firebaseAuth.verifyIdToken(userIdToken);
        return decodedToken.getUid();
    }
}
