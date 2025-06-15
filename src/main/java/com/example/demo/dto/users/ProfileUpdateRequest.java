package com.example.demo.dto.users;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProfileUpdateRequest {
    @Schema(description = "사용자 닉네임", example = "userNickname")
    private String nickname;
    @Schema(description = "사용자 프로필 이미지 URL", example = "https://example.com/profile.jpg")
    private String profileImageUrl;
} 