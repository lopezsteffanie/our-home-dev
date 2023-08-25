package com.steviecodesit.ourhomedev.household;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.steviecodesit.ourhomedev.user.User;
import com.steviecodesit.ourhomedev.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HouseholdService {

    private final FirebaseAuth firebaseAuth;
    private final Firestore firestore;
    private final UserService userService;

    @Autowired
    public HouseholdService(FirebaseAuth firebaseAuth, Firestore firestore, UserService userService) {
        this.firebaseAuth = firebaseAuth;
        this.firestore = firestore;
        this.userService = userService;
    }

    public Household createHousehold(Household household, String userIdToken) throws FirebaseAuthException {
        String uid = userService.verifyTokenAndGetUserId(userIdToken);

        // Save the Household to Firestore
        DocumentReference newHouseholdRef = firestore.collection("households").document();
        household.setId(newHouseholdRef.getId());
        newHouseholdRef.set(household);

        // Create and Save HouseholdMembership for the user
        HouseholdMembership membership = new HouseholdMembership();
        membership.setHouseholdId(household.getId());
        membership.setUserId(uid);
        membership.setHouseholdRole(HouseholdRole.OWNER);
        membership.setMemberStatus(HouseholdMembershipStatus.ACCEPTED);

        household.getMembers().add(membership);

        DocumentReference membershipRef = firestore.collection("householdMemberships").document();
        membershipRef.set(membership);

        return household;
    }

    public void sendInvite(HouseholdInvite invite) {
        CollectionReference invitesCollection = firestore.collection("householdInvites");
        invitesCollection.add(invite);
    }

    public boolean isOwnerOfHousehold(String userId, String householdId) {
        Household household;
        try {
            household = getHouseholdById(householdId);
        } catch (Exception e) {
            // TODO: handle exception
            throw new RuntimeException(e);
        }

        return household.getMembers().stream()
                .anyMatch(member -> member.getUserId().equals(userId) && member.getHouseholdRole() == HouseholdRole.OWNER);
    }

    public boolean isAlreadyAMember(String userId, String householdId) {
        Household household;
        try {
            household = getHouseholdById(householdId);
        } catch (Exception e) {
            // TODO: handle exception
            throw new RuntimeException(e);
        }

        return household.getMembers().stream()
                .anyMatch(member -> member.getUserId().equals(userId));
    }

    public void addMemberWithPendingStatus(String userId, String householdId) {
        Household household;
        try {
            household = getHouseholdById(householdId);
        } catch (Exception e) {
            // TODO: handle exception
            throw new RuntimeException(e);
        }

        HouseholdMembership newMember = new HouseholdMembership();
        newMember.setUserId(userId);
        newMember.setHouseholdId(householdId);
        newMember.setHouseholdRole(HouseholdRole.MEMBER);
        newMember.setMemberStatus(HouseholdMembershipStatus.PENDING);

        household.getMembers().add(newMember);

        try {
            updateHouseholdMembers(householdId, household);
        } catch (Exception e) {
            // TODO: handle exceptions
            throw new RuntimeException(e);
        }
    }

    public void acceptMembership(String householdId, String userId) throws Exception {
        DocumentReference householdRef = firestore.collection("households").document(householdId);
        Household household = householdRef.get().get().toObject(Household.class);

        for (HouseholdMembership membership : household.getMembers()) {
            if (membership.getUserId().equals(userId)) {
                membership.setMemberStatus(HouseholdMembershipStatus.ACCEPTED);
            }
        }
        householdRef.set(household);
    }

    public void declineMembership(String householdId, String userId) throws Exception {
        DocumentReference householdRef = firestore.collection("houseohlds").document(householdId);
        Household household = householdRef.get().get().toObject(Household.class);

        for (HouseholdMembership membership : household.getMembers()) {
            if (membership.getUserId().equals(userId)) {
                membership.setMemberStatus(HouseholdMembershipStatus.DECLINED);
            }
        }
        householdRef.set(household);
    }

    public void cancelInvitation(String householdId, String userId) throws Exception {
        DocumentReference householdRef = firestore.collection("households").document(householdId);
        Household household = householdRef.get().get().toObject(Household.class);

        // Check if the membership exists and update its status
        for (HouseholdMembership membership : household.getMembers()) {
            if (membership.getUserId().equals(userId)) {
                membership.setMemberStatus(HouseholdMembershipStatus.DECLINED);
                break;
            }
        }

        householdRef.set(household);

        userService.removeMembershipFromUser(userId, householdId);
    }

    public void requestJoinHousehold(String requesterUserId, String targetUserId) throws Exception {
        DocumentReference targetUserRef = firestore.collection("users").document(targetUserId);
        User targetUser = targetUserRef.get().get().toObject(User.class);

        if (targetUser.getHouseholdMembership() != null) {
            HouseholdMembership newMembership = new HouseholdMembership();
            newMembership.setUserId(requesterUserId);
            newMembership.setHouseholdId(targetUser.getHouseholdMembership().getHouseholdId());
            newMembership.setMemberStatus(HouseholdMembershipStatus.PENDING);
            newMembership.setHouseholdRole(HouseholdRole.MEMBER);

            // Update the household document to include this new member request
            DocumentReference householdRef = firestore.collection("household").document(targetUser.getHouseholdMembership().getHouseholdId());
            Household household = householdRef.get().get().toObject(Household.class);

            household.getMembers().add(newMembership);
            householdRef.set(household);

            // Update requesters user document to include the PENDING householdMembership
            DocumentReference requesterUserRef = firestore.collection("users").document(requesterUserId);
            User requesterUser = requesterUserRef.get().get().toObject(User.class);
            requesterUser.setHouseholdMembership(newMembership);
            requesterUserRef.set(requesterUser);
        } else {
            throw new Exception("Target user is not part of any household.");
        }
    }

    public void acceptUserToHousehold(String userIdToAccept, String householdId, String ownerId) throws Exception {
        // Fetch the household
        DocumentReference householdRef = firestore.collection("households").document(householdId);
        Household household = householdRef.get().get().toObject(Household.class);

        // Ensure the owner is making this request
        HouseholdMembership ownerMembership = household.getMembers().stream()
                .filter(m -> m.getUserId().equals(ownerId) && m.getHouseholdRole() == HouseholdRole.OWNER)
                .findFirst()
                .orElseThrow(() -> new Exception("Only the owner can accept new members."));

        // Find the pending membership of the user to accept
        HouseholdMembership membershipToAccept = household.getMembers().stream()
                .filter(m -> m.getUserId().equals(userIdToAccept) && m.getMemberStatus() == HouseholdMembershipStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> new Exception("No pending request from the specified user."));

        // Update the membership status to ACCEPTED
        membershipToAccept.setMemberStatus(HouseholdMembershipStatus.ACCEPTED);
        householdRef.set(household);

        // Update the user's document to reflect the ACCEPTED status
        DocumentReference userRef = firestore.collection("users").document(userIdToAccept);
        User user = userRef.get().get().toObject(User.class);
        user.setHouseholdMembership(membershipToAccept);
        userRef.set(user);
    }

    public void denyUserFromHousehold(String userIdToDeny, String householdId, String ownerId) throws  Exception {
        // Fetch the household
        DocumentReference householdRef = firestore.collection("households").document(householdId);
        Household household = householdRef.get().get().toObject(Household.class);

        // Ensure the owner is making this request
        HouseholdMembership ownerMembership = household.getMembers().stream()
                .filter(m -> m.getUserId().equals(ownerId) && m.getHouseholdRole() == HouseholdRole.OWNER)
                .findFirst()
                .orElseThrow(() -> new Exception("Only the owner can deny members."));

        // Remove the PENDING membership from the household document
        boolean removed = household.getMembers().removeIf(m -> m.getUserId().equals(userIdToDeny) && m.getMemberStatus() == HouseholdMembershipStatus.PENDING);
        if (!removed) {
            throw new Exception("No pending request from the specified user to deny.");
        }

        householdRef.set(household);

        // Update the user's document to remove the denied membership
        DocumentReference userRef = firestore.collection("users").document(userIdToDeny);
        User user = userRef.get().get().toObject(User.class);
        user.setHouseholdMembership(null);
        userRef.set(user);
    }

    public void leaveHousehold(String userId, String householdId) throws Exception {
        // Fetch the household
        DocumentReference householdRef = firestore.collection("households").document(householdId);
        Household household = householdRef.get().get().toObject(Household.class);

        // Try to find the member in the household
        HouseholdMembership memberToRemove = household.getMembers().stream()
                .filter(m -> m.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new Exception("User is not a member of the household."));

        // Check if the member is the owner
        if (memberToRemove.getHouseholdRole() == HouseholdRole.OWNER) {
            // Remove the owner from the household members
            household.getMembers().remove(memberToRemove);

            // Choose a new owner, in this case, the longest-standing member
            if (!household.getMembers().isEmpty()) {
                HouseholdMembership newOwner = household.getMembers().get(0);
                newOwner.setHouseholdRole(HouseholdRole.OWNER);
            } else {
                // TODO: If there are no more members, consider deleting the household or marking it as inactive
            }
        } else {
            // If just a member, simply remove the member from the household
            household.getMembers().remove(memberToRemove);
        }

        householdRef.set(household);

        // Update the old owner's document to remove their membership
        DocumentReference ownerRef = firestore.collection("users").document(userId);
        User owner = ownerRef.get().get().toObject(User.class);
        owner.setHouseholdMembership(null);
        ownerRef.set(owner);
    }

    private Household getHouseholdById(String householdId) throws Exception {
        DocumentReference docRef = firestore.collection("households").document(householdId);
        DocumentSnapshot documentSnapshot = docRef.get().get();

        if (documentSnapshot.exists()) {
            return documentSnapshot.toObject(Household.class);
        } else {
            throw new Exception("Household not found");
        }
    }

    private void updateHouseholdMembers(String householdId, Household updatedHousehold) throws  Exception {
        DocumentReference docRef = firestore.collection("households").document(householdId);

        docRef.update("members", updatedHousehold.getMembers()).get();
    }
}
