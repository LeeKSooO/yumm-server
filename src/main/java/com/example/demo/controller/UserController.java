package com.example.demo.controller;

import com.example.demo.domain.User;
import com.example.demo.service.UserService;
import com.example.demo.dto.UserInfoResponseDto;
import com.example.demo.security.CustomUserDetails;
import com.example.demo.dto.SignupRequestDto;
import com.example.demo.dto.UpdateUserInfoDto;
import com.example.demo.dto.ChangePasswordRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;


@RestController // 이 클래스가 REST API를 처리하는 컨트롤러임을 명시
@RequestMapping("/api/user") 
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
    public UserInfoResponseDto getMyInfo(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return userService.getMyInfoByUsername(userDetails.getUsername());
    }

    // 내 정보 수정
    @PutMapping("/me")
    public UserInfoResponseDto updateMyInfo(@AuthenticationPrincipal CustomUserDetails userDetails,
                                            @RequestBody UpdateUserInfoDto updateUserInfoDto) {
        return userService.updateUserInfo(userDetails.getUsername(), updateUserInfoDto);
    }

    // 비밀번호 변경
    @PutMapping("/password")
    public ResponseEntity<String> changePassword(@AuthenticationPrincipal CustomUserDetails userDetails, 
                                                 @RequestBody ChangePasswordRequestDto changePasswordRequestDto) {
        userService.changePassword(userDetails.getUsername(), changePasswordRequestDto);
        return ResponseEntity.ok("Password changed successfully");
    }

    // 회원탈퇴
    @DeleteMapping("/withdraw")
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal CustomUserDetails userDetails) {
        userService.withdraw(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}