package com.steviecodesit.ourhomedev.auth;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Email;
import javax.validation.constraints.Size;

@Getter
@Setter
public class LoginRequest {
    @Email
    private String email;
}
