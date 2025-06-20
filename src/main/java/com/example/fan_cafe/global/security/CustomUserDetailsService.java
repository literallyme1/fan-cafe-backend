package com.example.fan_cafe.global.security;


import com.example.fan_cafe.user.domain.User;
import com.example.fan_cafe.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public  CustomUserDetails loadUserByUsername(String userIdStr) throws UsernameNotFoundException {
        Long userId = Long.valueOf(userIdStr);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new CustomUserDetails(user);
    }
}
