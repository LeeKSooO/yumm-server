package com.example.demo.service;

import com.example.demo.domain.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.dto.ChangePasswordRequestDto;
import com.example.demo.dto.SignupRequestDto;
import com.example.demo.dto.UserInfoResponseDto;
import com.example.demo.dto.UpdateUserInfoDto;
//import com.example.demo.dto.ChangePasswordRequestDto;
import org.springframework.http.HttpStatus;
import com.example.demo.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

//import java.util.Optional;

@Service // 비즈니스 로직을 처리하는 서비스 계층
@RequiredArgsConstructor // final로 선언된 필드를 자동으로 생성자 주입
public class UserService {
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // signup
    public User signup(SignupRequestDto signupRequestDto) {
        if(userRepository.findByUsername(signupRequestDto.getUsername()).isPresent()) {
            throw new RuntimeException("이미 존재하는 사용자입니다.");
        }

        String encodedPasswowrd = passwordEncoder.encode(signupRequestDto.getPassword());

        User newUser = User.builder()
            .username(signupRequestDto.getUsername())
            .password(encodedPasswowrd)
            .name(signupRequestDto.getName())
            .phone(signupRequestDto.getPhone())
            .email(signupRequestDto.getEmail())
            .role("ROLE_USER") // Default
            .build();

        return userRepository.save(newUser);
    }

    // login
    // rawPassword(평문)와 BCrpytPasswordEncoder를 사용하여 암호화한 비밀번호를 비교하는 방식
    public String login(String username, String rawPassword) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(()-> new RuntimeException("사용자를 찾을 수 없습니다."));

        if(!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        return jwtUtil.generateToken(username);
    }

    // getMyInfo
    public UserInfoResponseDto getMyInfoByUsername(String username) {
        User user = userRepository.findByUsername(username)
                    .orElseThrow(()-> new RuntimeException("사용자를 찾을 수 없습니다."));

        return UserInfoResponseDto.builder()
            .username(user.getUsername())
            .name(user.getName())
            .phone(user.getPhone())
            .email(user.getEmail())
            .build();
    }
    
    // updateMyInfo
    public UserInfoResponseDto updateUserInfo(String username, UpdateUserInfoDto updateUserInfoDto) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(()-> new RuntimeException("사용자를 찾을 수 없습니다."));
            user.setName(updateUserInfoDto.getName());
            user.setPhone(updateUserInfoDto.getPhone());
            user.setEmail(updateUserInfoDto.getEmail());

            User saved = userRepository.save(user);

            return UserInfoResponseDto.builder()
                .username(saved.getUsername())
                .name(saved.getName())
                .phone(saved.getPhone())
                .email(saved.getEmail())
                .build();
    }

    // changePassword
    public void changePassword(String username, ChangePasswordRequestDto changePasswordRequestDto) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        // 현재 비밀번호 검증
        if(!passwordEncoder.matches(changePasswordRequestDto.getOldPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"현재 비밀번호가 일치하지 않습니다.");
        }

        // 새 비밀번호 암호화 후 저장
        String encodedNew = passwordEncoder.encode(changePasswordRequestDto.getNewPassword());
        user.setPassword(encodedNew);
        userRepository.save(user);
    }

    // 내 정보 조회(admin전용 userId 기준으로 search)
    /*
    public User getMyInfo(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(()-> new RuntimeException("사용자를 찾을 수 없습니다."));
    }
    */

}