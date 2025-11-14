package com.MediConnect.config;

import com.MediConnect.EntryRelated.entities.AccountStatus;
import com.MediConnect.EntryRelated.entities.Users;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UserPrincipal implements UserDetails {
    private Users user;

    public UserPrincipal(Users user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase().trim()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        AccountStatus status = user.getAccountStatus();
        return status != AccountStatus.ON_HOLD
                && status != AccountStatus.BANNED
                && status != AccountStatus.PENDING
                && status != AccountStatus.REJECTED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        AccountStatus status = user.getAccountStatus();
        return status == AccountStatus.ACTIVE;
    }
}
