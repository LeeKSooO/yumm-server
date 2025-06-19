package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;

/**
 * FCM 토큰 관리를 위한 서비스 인터페이스
 * 토큰의 저장, 조회, 만료 처리 등의 기능을 제공
 */
@Service
public interface FcmTokenService {

    /**
     * 새로운 FCM 토큰을 저장하거나 기존 토큰을 업데이트
     * @param userId 사용자 ID
     * @param newToken 새로운 FCM 토큰
     */
    void saveOrUpdateToken(Long userId, String newToken);

    /**
     * 사용자의 모든 유효한 FCM 토큰을 조회
     * 만료된 토큰은 제외됨
     * @param userId 사용자 ID
     * @return 유효한 FCM 토큰 목록
     */
    List<String> getTokensByUserId(Long userId);

    /**
     * 특정 FCM 토큰을 만료 처리
     * 토큰이 더 이상 유효하지 않음을 표시
     * @param token 만료 처리할 FCM 토큰
     */
    void invalidateToken(String token);

    /**
     * 만료된 모든 FCM 토큰을 정리
     * 매일 자정에 자동으로 실행됨
     */
    void cleanupExpiredTokens();

    /**
     * FCM 토큰의 유효성을 검증
     * @param token 검증할 FCM 토큰
     * @return 토큰이 유효하면 true, 아니면 false
     */
    boolean validateToken(String token);

    /**
     * FCM 토큰의 형식을 검증
     * @param token 검증할 FCM 토큰
     * @return 토큰 형식이 올바르면 true, 아니면 false
     */
    boolean validateTokenFormat(String token);

    /**
     * 사용자의 유효한 FCM 토큰을 조회
     * @param userId 사용자 ID
     * @return 유효한 FCM 토큰 문자열, 없으면 null
     */
    String getValidToken(Long userId);
}
