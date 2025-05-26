package com.example.demo.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessTokenResponseDto {

    private String accessToken;

    public static AccessTokenResponseDto of(String accessToken) {
        return AccessTokenResponseDto.builder()
            .accessToken(accessToken)
            .build();
    }
}
