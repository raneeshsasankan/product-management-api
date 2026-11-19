package com.nzuretech.product_management.security;

import com.nzuretech.product_management.entity.AppUser;
import com.nzuretech.product_management.entity.Permission;
import com.nzuretech.product_management.entity.Role;
import com.nzuretech.product_management.repository.AppUserRepository;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashSet;
import java.util.Set;

/**
 * Loads application users and their authorities from PostgreSQL
 * for Spring Security authentication and authorization.
 *
 * <p>Converts database roles and permissions into
 * Spring Security granted authorities.</p>
 *
 * @author Raneesh Sasankan
 * @since 1.0
 */
public class DatabaseUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    /**
     * Creates the database-backed user details service.
     *
     * @param appUserRepository repository used to retrieve users
     */
    public DatabaseUserDetailsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    /**
     * Loads a user by username, including assigned roles
     * and permissions.
     *
     * @param username the username supplied during authentication
     * @return Spring Security user details
     * @throws UsernameNotFoundException if the user does not exist
     */
    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        AppUser appUser = appUserRepository
                .findByNormalizedUsernameWithRolesAndPermissions(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Invalid username or password"
                ));

        Set<GrantedAuthority> authorities = new HashSet<>();

        for (Role role : appUser.getRoles()) {

            authorities.add(
                    new SimpleGrantedAuthority("ROLE_" + role.getName())
            );

            for (Permission permission : role.getPermissions()) {
                authorities.add(
                        new SimpleGrantedAuthority(permission.getName())
                );
            }
        }

        return User.withUsername(appUser.getUsername())
                .password(appUser.getPasswordHash())
                .disabled(!appUser.isEnabled())
                .authorities(authorities)
                .build();
    }
}
