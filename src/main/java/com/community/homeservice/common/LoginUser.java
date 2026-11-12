package com.community.homeservice.common;

import com.community.homeservice.entity.SysUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 登录态用户，额外携带业务主键与姓名，避免每次请求回查数据库。
 */
public class LoginUser implements UserDetails {

    private final SysUser user;

    public LoginUser(SysUser user) {
        this.user = user;
    }

    public SysUser getUser() {
        return user;
    }

    public Long getId() {
        return user.getId();
    }

    public String getRealName() {
        String name = user.getRealName();
        return (name == null || name.isBlank()) ? user.getUsername() : name;
    }

    public String getRole() {
        return user.getRole();
    }

    public String getPhone() {
        return user.getPhone();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
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
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() == null || user.getStatus() == 1;
    }
}
