package com.steviecodesit.ourhomedev.household;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.steviecodesit.ourhomedev.user.User;
import com.steviecodesit.ourhomedev.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/households")
public class HouseholdController {

    private HouseholdService householdService;
    private FirebaseAuth firebaseAuth;
    private UserService userService;

    @Autowired
    public HouseholdController(HouseholdService householdService, FirebaseAuth firebaseAuth, UserService userService) {
        this.householdService = householdService;
        this.firebaseAuth = firebaseAuth;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<Household> createHousehold(@RequestBody Household household, @RequestHeader("Authorization") String userIdToken) {
        try {
            Household createdHousehold = householdService.createHousehold(household, userIdToken);
            return ResponseEntity.ok(createdHousehold);
        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @GetMapping("/search-users")
    public ResponseEntity<List<User>> searchUsers(@RequestParam String query) {
        List<User> matchingUsers = userService.findUserByEmailOrUsername(query);
        return ResponseEntity.ok(matchingUsers);
    }

    @PostMapping("/send-invite")
    public ResponseEntity<String> sendInvite(@RequestBody HouseholdInvite invite) {
        try {
            // Validate if the inviter is the owner
            if (!householdService.isOwnerOfHousehold(invite.getInviterUserId(), invite.getHouseholdId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You're not authorized to send invites for this household.");
            }

            // Check if the user is already a member
            if (householdService.isAlreadyAMember(invite.getInviteeUserId(), invite.getHouseholdId())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User is already a member of this household.");
            }

            // Add the invitee to the household with status PENDING
            householdService.addMemberWithPendingStatus(invite.getInviteeUserId(), invite.getHouseholdId());

            // Store the invite
            householdService.sendInvite(invite);

            return ResponseEntity.ok("Invite sent successfully!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send invite.");
        }
    }

    @PostMapping("/accept-invite/{householdId}")
    public ResponseEntity<String> acceptInvite(@PathVariable String householdId, @RequestHeader("Authorization") String userIdToken) {
        try {
            // Verify the user token and get the user's ID
            String userId = userService.verifyTokenAndGetUserId(userIdToken);

            // Update the household's member list
            householdService.acceptMembership(householdId, userId);

            // Update the user's document with the new membership info
            userService.addMembershipToUser(userId, householdId, HouseholdRole.MEMBER, HouseholdMembershipStatus.ACCEPTED);

            return ResponseEntity.ok("Invite accepted successfully!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error while accepting invite: " + e.getMessage());
        }
    }

    @PostMapping("/decline-invite/{householdId}")
    public ResponseEntity<String> declineInvite(@PathVariable String householdId, @RequestHeader("Authorization") String userIdToken) {
        try {
            String userId = userService.verifyTokenAndGetUserId(userIdToken);

            householdService.declineMembership(householdId, userId);

            userService.removeMembershipFromUser(userId, householdId);

            return ResponseEntity.ok("Invite declined successfully!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error while declining invite: " + e.getMessage());
        }
    }

    @PostMapping("/{householdId}/cancel-incite/{userId}")
    public ResponseEntity<String> cancelInvitation(@PathVariable String householdId, @PathVariable String userId) {
        try {
            householdService.cancelInvitation(householdId, userId);
            return ResponseEntity.ok("Invite cancelled successfully.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to cancel the invitation.");
        }
    }

    @PostMapping("/requestHoin/{targetUserId}")
    public ResponseEntity<String> requestJoinHousehold(@RequestHeader("Authorization") String requestUserIdToken, @PathVariable String targetUserId) throws FirebaseAuthException {
        String requesterUserId = userService.verifyTokenAndGetUserId(requestUserIdToken);

        try {
            householdService.requestJoinHousehold(requesterUserId, targetUserId);
            return ResponseEntity.ok("Successfully sent join request.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to send join request.");
        }
    }

    @PostMapping("/acceptUser/{userIdToAccept}")
    public ResponseEntity<String> acceptUserToHousehold(@PathVariable String userIdToAccept, @RequestBody String householdId, @RequestHeader("Authorization") String ownerIdToken) throws FirebaseAuthException {
        String ownerId = userService.verifyTokenAndGetUserId(ownerIdToken);
        try {
            householdService.acceptUserToHousehold(userIdToAccept, householdId, ownerId);
            return ResponseEntity.ok("User successfully added to the household.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to add user to the household.");
        }
    }

    @PostMapping("/denyUser/{userIdToDeny}")
    public ResponseEntity<String> denyUserFromHousehold(@PathVariable String userIdToDeny, @RequestBody String householdId, @RequestHeader("Authorization") String ownerIdToken) throws FirebaseAuthException {
        String ownerId = userService.verifyTokenAndGetUserId(ownerIdToken);
        try {
            householdService.denyUserFromHousehold(userIdToDeny, householdId, ownerId);
            return ResponseEntity.ok("User's request denied successfully.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to deny user's request.");
        }
    }

    @PostMapping("/leaveHousehold/{householdId}")
    public ResponseEntity<String> leaveHousehold(@PathVariable String householdId, @RequestHeader("Authorization") String  ownerIdToken) throws FirebaseAuthException {
        String ownerId = userService.verifyTokenAndGetUserId(ownerIdToken);
        try {
            householdService.leaveHousehold(ownerId, householdId);
            return ResponseEntity.ok("Owner left successfully and new owner has been assigned.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to process owner's leaving request.");
        }
    }
}
