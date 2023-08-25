package com.steviecodesit.ourhomedev.household;

import lombok.*;

@Builder
@Getter
@Setter
public class HouseholdMembership {
    private String householdId;
    private String userId;
    private HouseholdRole householdRole;
    private HouseholdMembershipStatus memberStatus;
}
