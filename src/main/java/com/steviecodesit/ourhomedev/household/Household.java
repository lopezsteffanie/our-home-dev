package com.steviecodesit.ourhomedev.household;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Household {
    String id;
    String householdName;
    List<HouseholdMembership> members; // List of household memberships
}
