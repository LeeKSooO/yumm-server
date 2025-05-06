package com.example.demo.security;

import com.example.demo.domain.User;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor  //UserDetailsService : Spring Security 인증에 반드시 필요한 인터페이스
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                     .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없음"));
        return new CustomUserDetails(user); // User Entity 래핑하여 Spring Security에 전달
    }
}