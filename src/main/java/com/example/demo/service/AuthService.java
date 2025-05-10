package com.example.demo.service;

import com.example.demo.dto.AuthRequestDto;
import com.example.demo.dto.AuthResponseDto;

/**
 * 인증 관련 서비스 인터페이스.
 * 로그인, 로그아웃, 액세스 토큰 재발급 기능 정의.
 */
public interface AuthService {

    /** 사용자 로그인 처리 */
    AuthResponseDto login(AuthRequestDto authRequestDto);

        
    /** 사용자 로그아웃 처리 */
    void logout(String refreshToken);


    /** 리프레시 토큰을 이용한 액세스 토큰 재발급 */
    String refreshAccessToken(String authorizationHeader, String refreshTokenCookie);
 
}