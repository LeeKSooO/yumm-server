package com.example.demo.dto.users;

import com.example.demo.enums.Gender;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.demo.domain.User;
import com.example.demo.domain.UserRole;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserInfoDetailsResponse {
    @Schema(description = "사용자 이메일", example = "user@example.com")
    private final String email;
    @Schema(description = "사용자 닉네임", example = "userNickname")
    private final String nickname;
    @Schema(description = "사용자 성별", example = "male")
    private final Gender gender;
    @Schema(description = "사용자 나이", example = "25")
    private final Integer age;
    @Schema(description = "사용자 전화번호", example = "010-1234-5678")
    private final String phoneNumber;
    @Schema(description = "사용자 프로필 이미지 URL", example = "https://example.com/profile.jpg")
    private final String profileImageUrl;
    @Schema(description = "사용자 역할", example = "ROLE_USER")
    private final UserRole role;

    public static UserInfoDetailsResponse from(User user) {
        return UserInfoDetailsResponse.builder()
                .email(user.getEmail())
                .nickname(user.getNickname())
                .gender(user.getGender())
                .age(user.getAge())
                .phoneNumber(user.getPhoneNumber())
                .profileImageUrl(user.getProfileImageUrl())
                .role(user.getRole())
                .build();
    }
} 