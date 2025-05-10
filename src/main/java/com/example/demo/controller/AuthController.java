package com.example.demo.controller;

import com.example.demo.dto.AccessTokenResponseDto;
import com.example.demo.dto.AuthRequestDto;
import com.example.demo.dto.AuthResponseDto;
import com.example.demo.service.AuthService;
import com.example.demo.util.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Operation;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;


    // 로그인 : accessToken, refreshToken 반환
    @Operation(summary = "사용자 로그인", description = "아이디/비밀번호를 통해 로그인하고 JWT를 반환합니다.")
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody AuthRequestDto authRequestDto) {

        AuthResponseDto tokens = authService.login(authRequestDto);

        return ResponseEntity.ok(tokens);
    }


    // 로그아웃
    @Operation(summary = "사용자 로그아웃", description = "헤더에서 전달된 refreshToken을 삭제하여 로그아웃 처리합니다.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String bearerToken) {
        
        String refreshToken = JwtUtils.extractTokenFrom(bearerToken);

        authService.logout(refreshToken);

        return ResponseEntity.noContent().build();
    }


    // Access token 재발급
    @Operation(summary = "액세스 토큰 재발급", description = "리프레시 토큰을 기반으로 새로운 액세스 토큰을 발급합니다. 헤더 또는 쿠키에서 refreshToken을 전달합니다.")
    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponseDto> refreshAccessToken(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader, 
            @CookieValue(name = "refreshToken", required = false) String refreshTokenCookie) {
     
        String newAccessToken = authService.refreshAccessToken(authorizationHeader, refreshTokenCookie);

        log.debug("New access token generated");

        return ResponseEntity.ok(
            AccessTokenResponseDto.builder()
                .accessToken(newAccessToken)
                .build()
        );        
    }

}