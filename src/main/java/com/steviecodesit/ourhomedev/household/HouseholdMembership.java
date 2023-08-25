package com.steviecodesit.ourhomedev.household;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HouseholdMembership {
    private String householdId;
    private String userId;
    private HouseholdRole householdRole;
    private HouseholdMembershipStatus memberStatus;
}
