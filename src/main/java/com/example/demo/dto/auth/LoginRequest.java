package com.example.demo.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 로그인 요청 데이터
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {
    @Schema(description = "사용자 이메일",example = "user@example.com")
    private String email;
    @Schema(description = "사용자 비밀번호",example = "password123")
    private String password;
    @Schema(description = "FCM 토큰", example = "fcm_token_example")
    private String fcmToken;
}
