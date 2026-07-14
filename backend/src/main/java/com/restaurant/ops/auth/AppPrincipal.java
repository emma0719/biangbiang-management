package com.restaurant.ops.auth;

import com.restaurant.ops.employee.Employee;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AppPrincipal implements UserDetails {
  private final Employee employee;
  private final Collection<? extends GrantedAuthority> authorities;

  public AppPrincipal(Employee employee, Collection<? extends GrantedAuthority> authorities) {
    this.employee = employee;
    this.authorities = authorities;
  }

  public Employee employee() {
    return employee;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return employee.getPasswordHash();
  }

  @Override
  public String getUsername() {
    return String.valueOf(employee.getId());
  }
}
