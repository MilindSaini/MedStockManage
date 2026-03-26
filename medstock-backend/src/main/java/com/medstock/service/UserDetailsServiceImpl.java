package com.medstock.service;

import com.medstock.entity.User;
import com.medstock.repository.UserRepository;
import com.medstock.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        String normalizedIdentity = normalizeIdentity(usernameOrEmail);
        User user = userRepository.findByUsernameOrEmail(normalizedIdentity, normalizedIdentity)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return UserPrincipal.from(user);
    }

    private String normalizeIdentity(String identity) {
        String trimmed = identity == null ? "" : identity.trim();
        if (trimmed.contains("@")) {
            return trimmed.toLowerCase();
        }
        return trimmed;
    }
}
