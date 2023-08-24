package com.steviecodesit.ourhomedev.user;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.*;
import com.google.firebase.auth.UserRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class UserServiceTest {

    private UserService userService;
    private Firestore firestore;
    private CollectionReference usersCollection;
    private DocumentReference userDocument;
    private Query query;

    @BeforeEach
    public void setUp() {
        firestore = mock(Firestore.class);
        usersCollection = mock(CollectionReference.class);
        userDocument = mock(DocumentReference.class);
        query = mock(Query.class);

        when(firestore.collection("users")).thenReturn(usersCollection);
        when(usersCollection.document(anyString())).thenReturn(userDocument);
        when(usersCollection.whereEqualTo(anyString(), anyString())).thenReturn(query);

        userService = new UserService(firestore);
    }

    @Test
    public void testSaveUser() {
        UserRecord userRecord = mock(UserRecord.class);
        String displayName = "Test User";

        when(userRecord.getEmail()).thenReturn("test@example.com");
        when(userRecord.getUid()).thenReturn("uid");

        userService.saveUser(userRecord, displayName);

        verify(usersCollection).document(userRecord.getUid());
        verify(userDocument).set(any(User.class));
    }

    @Test
    public void testUpdateIsLoggedInStatus() {
        String userId = "userId";

        userService.updateIsLoggedInStatus(userId, true);
        verify(usersCollection).document(userId);
        verify(userDocument).update("loggedIn", true);
    }

    @Test
    public void testIsUserLoggedIn_UserExistsAndIsLoggedIn() {
        String userId = "userId";
        User user = new User();
        user.setLoggedIn(true);

        DocumentSnapshot documentSnapshot = mock(DocumentSnapshot.class);
        ApiFuture<DocumentSnapshot> futureSnapshot = ApiFutures.immediateFuture(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(true);
        when(documentSnapshot.toObject(User.class)).thenReturn(user);
        when(userDocument.get()).thenReturn(futureSnapshot);

        boolean isLoggedIn = userService.isUserLoggedIn(userId);

        assertTrue(isLoggedIn);
        verify(firestore.collection("users")).document(userId);
    }

    @Test
    public void testIsUserLoggedIn_UserExistsAndIsNotLoggedIn() {
        String userId = "userId";
        User user = new User();
        user.setLoggedIn(false);

        DocumentSnapshot documentSnapshot = mock(DocumentSnapshot.class);
        ApiFuture<DocumentSnapshot> futureSnapshot = ApiFutures.immediateFuture(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(true);
        when(documentSnapshot.toObject(User.class)).thenReturn(user);
        when(userDocument.get()).thenReturn(futureSnapshot);

        boolean isLoggedIn = userService.isUserLoggedIn(userId);

        assertFalse(isLoggedIn);
        verify(firestore.collection("users")).document(userId);
    }

    @Test
    public void testIsUserLoggedIn_UserDoesNotExist() {
        String userId = "userId";

        DocumentSnapshot documentSnapshot = mock(DocumentSnapshot.class);
        ApiFuture<DocumentSnapshot> futureSnapshot = ApiFutures.immediateFuture(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(false);
        when(userDocument.get()).thenReturn(futureSnapshot);

        boolean isLoggedIn = userService.isUserLoggedIn(userId);

        assertFalse(isLoggedIn);
        verify(firestore.collection("users")).document(userId);
    }

    @Test
    public void testIsUserLoggedIn_InterruptedException() throws Exception {
        String userId = "userId";

        ApiFuture<DocumentSnapshot> futureSnapshot = mock(ApiFuture.class);
        when(futureSnapshot.get()).thenThrow(InterruptedException.class);
        when(userDocument.get()).thenReturn(futureSnapshot);

        boolean isLoggedIn = userService.isUserLoggedIn(userId);

        assertFalse(isLoggedIn);
        verify(firestore.collection("users")).document(userId);
    }

    @Test
    public void testIsUserLoggedIn_ExecutionException() throws Exception {
        String userId = "userId";

        ApiFuture<DocumentSnapshot> futureSnapshot = mock(ApiFuture.class);
        when(futureSnapshot.get()).thenThrow(ExecutionException.class);
        when(userDocument.get()).thenReturn(futureSnapshot);

        boolean isLoggedIn = userService.isUserLoggedIn(userId);

        assertFalse(isLoggedIn);
        verify(firestore.collection("users")).document(userId);
    }

    @Test
    public void testIsDisplayNameUnique_DisplayNameIsUnique() {
        String displayName = "UniqueName";

        // Mocking Firestore objects
        CollectionReference usersCollection = mock(CollectionReference.class);
        Query query = mock(Query.class);
        QuerySnapshot querySnapshot = mock(QuerySnapshot.class);
        ApiFuture<QuerySnapshot> futureSnapshot = ApiFutures.immediateFuture(querySnapshot);

        when(firestore.collection("users")).thenReturn(usersCollection);
        when(usersCollection.whereEqualTo("displayName", displayName)).thenReturn(query);
        when(query.limit(1)).thenReturn(query);
        when(query.get()).thenReturn(futureSnapshot);
        when(querySnapshot.isEmpty()).thenReturn(true);

        boolean isUnique = userService.isDisplayNameUnique(displayName);

        assertTrue(isUnique);
        verify(usersCollection).whereEqualTo("displayName", displayName);
    }


    @Test
    public void testIsDisplayNameUnique_DisplayNameIsNotUnique() {
        String displayName = "DuplicateName";

        CollectionReference usersCollection = mock(CollectionReference.class);
        Query query = mock(Query.class);
        QuerySnapshot querySnapshot = mock(QuerySnapshot.class);
        ApiFuture<QuerySnapshot> futureSnapshot = ApiFutures.immediateFuture(querySnapshot);

        when(firestore.collection("users")).thenReturn(usersCollection);
        when(usersCollection.whereEqualTo("displayName", displayName)).thenReturn(query);
        when(query.limit(1)).thenReturn(query);
        when(query.get()).thenReturn(futureSnapshot);
        when(querySnapshot.isEmpty()).thenReturn(false);

        boolean isUnique = userService.isDisplayNameUnique(displayName);

        assertFalse(isUnique);
        verify(firestore.collection("users")).whereEqualTo("displayName", displayName);
    }

    @Test
    public void testIsDisplayNameUnique_InterruptedException() throws Exception {
        String displayName = "UniqueName";

        Query query = mock(Query.class);
        CollectionReference usersCollection = mock(CollectionReference.class);
        ApiFuture<QuerySnapshot> futureSnapshot = mock(ApiFuture.class);
        when(firestore.collection("users")).thenReturn(usersCollection);
        when(usersCollection.whereEqualTo("displayName", displayName)).thenReturn(query);
        when(query.limit(1)).thenReturn(query);
        when(query.get()).thenReturn(futureSnapshot);
        when(futureSnapshot.get()).thenThrow(InterruptedException.class);

        boolean isUnique = userService.isDisplayNameUnique(displayName);

        assertFalse(isUnique);
        verify(firestore.collection("users")).whereEqualTo("displayName", displayName);
    }

    @Test
    public void testIsDisplayNameUnique_ExecutionException() throws Exception {
        String displayName = "UniqueName";

        Query query = mock(Query.class);
        CollectionReference usersCollection = mock(CollectionReference.class);
        ApiFuture<QuerySnapshot> futureSnapshot = mock(ApiFuture.class);
        when(firestore.collection("users")).thenReturn(usersCollection);
        when(usersCollection.whereEqualTo("displayName", displayName)).thenReturn(query);
        when(query.limit(1)).thenReturn(query);
        when(query.get()).thenReturn(futureSnapshot);
        when(futureSnapshot.get()).thenThrow(ExecutionException.class);

        boolean isUnique = userService.isDisplayNameUnique(displayName);

        assertFalse(isUnique);
        verify(firestore.collection("users")).whereEqualTo("displayName", displayName);
    }

    @Test
    public void testIsValidPassword_ValidPassword() {
        assertTrue(userService.isValidPassword("Password1!"));
        assertTrue(userService.isValidPassword("Aa1!Aa1!"));
        assertTrue(userService.isValidPassword("Xy$456Zz"));
    }

    @Test
    public void testIsValidPassword_InvalidPasswordNoDigit() {
        assertFalse(userService.isValidPassword("Password!"));
    }

    @Test
    public void testIsValidPassword_InvalidPasswordNoLowercase() {
        assertFalse(userService.isValidPassword("PASSWORD1!"));
    }

    @Test
    public void testIsValidPassword_InvalidPasswordNoUppdercase() {
        assertFalse(userService.isValidPassword("password1!"));
    }

    @Test
    public void testIsValidPassword_InvalidPasswordNoSpecialCharacter() {
        assertFalse(userService.isValidPassword("Password1"));
    }

    @Test
    public void testIsValidPassword_InvalidPasswordTooShort() {
        assertFalse(userService.isValidPassword("P1a!"));
    }
}
