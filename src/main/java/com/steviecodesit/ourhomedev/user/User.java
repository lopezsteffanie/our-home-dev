package com.steviecodesit.ourhomedev.user;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class User implements UserDetails {
    @Getter @Setter private String uid;
    @Getter @Setter private String email;
    @Getter @Setter private String displayName;

    // Constructors
    public User(String uid, String email, String displayName) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
    }

    // Implementations for the UserDetails interface
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList(); // No authorities for this user
    }

    @Override
    public String getPassword() {
        return null; // No password for this user
    }

    @Override
    public String getUsername() {
        return email; // Email as username
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Account never expires
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Account is never locked
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Credentials never expire
    }

    @Override
    public boolean isEnabled() {
        return true; // User is always enabled
    }
}
