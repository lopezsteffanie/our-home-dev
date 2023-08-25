package com.steviecodesit.ourhomedev.household;

import com.google.api.client.util.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HouseholdInvite {
    private String householdId;
    private String householdName;
    private String inviteeUserId;
    private String inviterUserId;
    private Data timestamp;
}
