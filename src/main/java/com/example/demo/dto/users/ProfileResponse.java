package com.example.demo.dto.users;

import com.example.demo.domain.User;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel; // <--- 이 라인을 추가해야 합니다.
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Jacksonized
public class ProfileResponse implements java.io.Serializable {
    @Schema(description = "닉네임",example = "user123")
    private final String nickname;
    @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
    private final String profileImageUrl;

    public static ProfileResponse from(User user) {
        return ProfileResponse.builder()
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }
}

