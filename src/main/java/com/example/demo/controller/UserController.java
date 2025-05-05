package com.example.demo.controller;

import com.example.demo.domain.User;
import com.example.demo.service.UserService;
import com.example.demo.dto.UserInfoResponseDto;
import com.example.demo.dto.SignupRequestDto;
import com.example.demo.dto.UpdateUserInfoDto;
import com.example.demo.dto.ChangePasswordRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;


@RestController // 이 클래스가 REST API를 처리하는 컨트롤러임을 명시
@RequestMapping("/api/user") // Common URL Prefix 설정
@RequiredArgsConstructor // final field 자동 주입
public class UserController {

    private final UserService userService;

    // 회원가입
    @PostMapping("/signup")
    public User signup(@RequestBody SignupRequestDto signupRequest) {
        return userService.signup(signupRequest);
    }
    
    // 내 정보 조회
    @GetMapping("/me")
    public UserInfoResponseDto getMyInfo() {
        //SecurityContext에 저장된 username 꺼내기
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userService.getMyInfoByUsername(username);
    }

    // 내 정보 수정
    @PutMapping("/me")
    public UserInfoResponseDto updateMyInfo(@RequestBody UpdateUserInfoDto updateUserInfoDto) {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userService.updateUserInfo(username, updateUserInfoDto);
    }

    // 비밀번호 변경
    @PutMapping("/password")
    public ResponseEntity<String> changePassword(@RequestBody ChangePasswordRequestDto changePasswordRequestDto) {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        userService.changePassword(username, changePasswordRequestDto);
        return ResponseEntity.ok("Password changed successfully");
    }


    /*이렇게 하면, 스프링 시큐리티가 
    SecurityContext 에 저장된 현재 인증된 UserDetails 객체를 파라미터로 주입해 줍니다.
     */
    // 회원탈퇴
    @DeleteMapping("/withdraw")
    public ResponseEntity<Void> withdraw(Authentication auth) {
        String username = auth.getName();
        userService.withdraw(username);
        return ResponseEntity.noContent().build();
    }
}