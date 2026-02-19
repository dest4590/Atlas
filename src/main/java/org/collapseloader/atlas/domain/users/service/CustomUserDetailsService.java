package org.collapseloader.atlas.domain.users.service;

import lombok.RequiredArgsConstructor;
import org.collapseloader.atlas.domain.users.entity.User;
import org.collapseloader.atlas.domain.users.repository.UserRepository;
import org.collapseloader.atlas.domain.users.security.CachedUser;
import org.jspecify.annotations.NonNull;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Cacheable(value = "users", key = "#username.trim()")
    @NonNull
    public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        String trimmedUsername = username.trim();
        User user = userRepository.findByUsernameIgnoreCase(trimmedUsername)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return CachedUser.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(new HashSet<>(user.getAuthorities()))
                .enabled(user.isEnabled())
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();
    }
}
