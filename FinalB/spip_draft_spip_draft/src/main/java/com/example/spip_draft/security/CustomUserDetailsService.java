package com.example.spip_draft.security;

import com.example.spip_draft.entity.User;
import com.example.spip_draft.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));

        if (!user.isActive()) {
            throw new UsernameNotFoundException("User inactive");
        }

        // ✅ Return custom user details that includes tenant (builderId)
        return new CustomUserDetails(user);
    }
}