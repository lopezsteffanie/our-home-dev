package com.steviecodesit.ourhomedev.user;

import com.steviecodesit.ourhomedev.household.HouseholdMembership;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class User {
    private String email;
    private String displayName;
    private boolean isLoggedIn;
    private HouseholdMembership householdMembership;
}
