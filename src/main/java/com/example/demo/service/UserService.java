package com.example.demo.service;

import com.example.demo.domain.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

//import java.util.Optional;

@Service // 비즈니스 로직을 처리하는 서비스 계층
@RequiredArgsConstructor // final로 선언된 필드를 자동으로 생성자 주입
public class UserService {
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // 회원가입
    public User signup(User user) {
        if(userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("이미 존재하는 사용자입니다.");
        }

        String encodedPasswowrd = passwordEncoder.encode(user.getPassword());

        User newUser = User.builder()
            .username(user.getUsername())
            .password(encodedPasswowrd)
            .name(user.getName())
            .phone(user.getPhone())
            .email(user.getEmail())
            .role("ROLE_USER") // Default
            .build();

        return userRepository.save(newUser);
    }

    // 로그인
    // rawPassword(평문)와 BCrpytPasswordEncoder를 사용하여 암호화한 비밀번호를 비교하는 방식
    public String login(String username, String rawPassword) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(()-> new RuntimeException("사용자를 찾을 수 없습니다."));

        if(!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        return jwtUtil.generateToken(username);
    }

    // 내 정보 조회
    public User getMyInfo(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(()-> new RuntimeException("사용자를 찾을 수 없습니다."));
    }

    public User getMyInfoByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
    }
    
}