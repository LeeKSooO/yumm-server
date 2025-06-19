package com.example.demo.dto.users;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class SignupRequest {

    @Schema(description = "사용자 이메일", example = "user@example.com")
    private String email;

    @Schema(description = "비밀번호", example = "password123")
    private String password;

    @Schema(description = "닉네임", example = "nickName")
    private String nickname;

    @Schema(description = "성별", example = "male")
    private String gender;

    @Schema(description = "나이", example = "25")
    private Integer age;

    @Schema(description = "전화번호", example = "010-1010-1010")
    private String phoneNumber;

    @Schema(description = "프로필 이미지 URL", example = "https://example.com/image.jpg")
    private String profileImageUrl;

    @Schema(description = "FCM 토큰", example = "fcm_token_example")
    private String fcmToken;
} 