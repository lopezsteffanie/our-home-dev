package com.steviecodesit.ourhomedev.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrationRequest {
    private String firstName;
    private String lastName;
    private String username;
    private String email;
    private String password;
}
