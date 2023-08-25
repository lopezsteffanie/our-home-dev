package com.steviecodesit.ourhomedev.household;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.FirebaseAuthException;
import com.steviecodesit.ourhomedev.user.User;
import com.steviecodesit.ourhomedev.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HouseholdService {

    private final Firestore firestore;
    private final UserService userService;

    @Autowired
    public HouseholdService(Firestore firestore, UserService userService) {
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
        HouseholdMembership membership = HouseholdMembership.builder()
                .householdId(household.getId())
                .userId(uid)
                .householdRole(HouseholdRole.OWNER)
                .memberStatus(HouseholdMembershipStatus.ACCEPTED)
                .build();

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

        HouseholdMembership newMember = HouseholdMembership.builder()
                .userId(userId)
                .householdId(householdId)
                .householdRole(HouseholdRole.MEMBER)
                .memberStatus(HouseholdMembershipStatus.PENDING)
                .build();

        household.getMembers().add(newMember);

        try {
            updateHouseholdMembers(householdId, household);
        } catch (Exception e) {
            // TODO: handle exceptions
            throw new RuntimeException(e);
        }
    }

    public void acceptOrDeclineMembership(String householdId, String userId, HouseholdMembershipStatus status) {
        Household household;
        try {
            household = getHouseholdById(householdId);
        } catch (Exception e) {
            // TODO: handle exception
            throw new RuntimeException(e);
        }

        for (HouseholdMembership membership : household.getMembers()) {
            if (membership.getUserId().equals(userId)) {
                membership.setMemberStatus(status);
            }
        }
        firestore.collection("households").document(householdId).set(household);
    }

    public void cancelInvitation(String householdId, String userId) throws Exception {
        Household household;
        try {
            household = getHouseholdById(householdId);
        } catch (Exception e) {
            // TODO: handle exception
            throw new RuntimeException(e);
        }

        // Check if the membership exists and update its status
        for (HouseholdMembership membership : household.getMembers()) {
            if (membership.getUserId().equals(userId)) {
                membership.setMemberStatus(HouseholdMembershipStatus.DECLINED);
                break;
            }
        }

        firestore.collection("households").document(householdId).set(household);

        userService.removeMembershipFromUser(userId, householdId);
    }

    public void requestJoinHousehold(String requesterUserId, String targetUserId) throws Exception {
        User targetUser = userService.getUserById(targetUserId);

        if (targetUser.getHouseholdMembership() != null) {
            HouseholdMembership newMembership = HouseholdMembership.builder()
                    .userId(requesterUserId)
                    .householdId(targetUser.getHouseholdMembership().getHouseholdId())
                    .memberStatus(HouseholdMembershipStatus.PENDING)
                    .householdRole(HouseholdRole.MEMBER)
                    .build();

            // Update the household document to include this new member request
            String householdId = targetUser.getHouseholdMembership().getHouseholdId();
            Household household;
            try {
                household = getHouseholdById(householdId);
            } catch (Exception e) {
                // TODO: handle exception
                throw new RuntimeException(e);
            }

            household.getMembers().add(newMembership);
            firestore.collection("households").document(householdId).set(household);

            // Update requesters user document to include the PENDING householdMembership
            User requesterUser = userService.getUserById(requesterUserId);
            requesterUser.setHouseholdMembership(newMembership);
            firestore.collection("users").document(requesterUserId).set(requesterUser);
        } else {
            throw new Exception("Target user is not part of any household.");
        }
    }

    public void acceptOrRejectUserToHousehold(String userId, String householdId, String ownerId, HouseholdMembershipStatus status) throws Exception {
        // Fetch the household
        Household household;
        try {
            household = getHouseholdById(householdId);
        } catch (Exception e) {
            // TODO: handle exception
            throw new RuntimeException(e);
        }

        // Ensure the owner is making this request
        household.getMembers().stream()
                .filter(m -> m.getUserId().equals(ownerId) && m.getHouseholdRole() == HouseholdRole.OWNER)
                .findFirst()
                .orElseThrow(() -> new Exception("Only the owner can accept new members."));

        // Find the pending membership of the user to accept
        HouseholdMembership membership = household.getMembers().stream()
                .filter(m -> m.getUserId().equals(userId) && m.getMemberStatus() == HouseholdMembershipStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> new Exception("No pending request from the specified user."));

        // Update the membership status
        membership.setMemberStatus(status);
        firestore.collection("households").document(householdId).set(household);

        User user = userService.getUserById(userId);
        if (status == HouseholdMembershipStatus.ACCEPTED) {
            // Update the user's document to reflect the new status
            user.setHouseholdMembership(membership);
        }
        if (status == HouseholdMembershipStatus.DECLINED) {
            // Update the user's document to remove the denied membership
            user.setHouseholdMembership(null);
        }
        firestore.collection("users").document(userId).set(user);
    }

    public void leaveHousehold(String userId, String householdId) throws Exception {
        // Fetch the household
        Household household;
        try {
            household = getHouseholdById(householdId);
        } catch (Exception e) {
            // TODO: handle exception
            throw new RuntimeException(e);
        }

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

        firestore.collection("households").document(householdId).set(household);

        // Update the old user's document to remove their membership
        User user = userService.getUserById(userId);
        user.setHouseholdMembership(null);
        firestore.collection("users").document(userId).set(user);
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
