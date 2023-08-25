package com.steviecodesit.ourhomedev.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Email;

@Getter
@Setter
public class LoginRequest {
    @Email
    private String email;
}
