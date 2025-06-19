package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.FcmToken;

/**
 * FCM 토큰 관리를 위한 레포지토리 인터페이스
 * 토큰의 조회, 삭제 등의 기능을 제공
 */
@Repository
public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {
    /**
     * 사용자 ID와 토큰으로 FCM 토큰을 조회
     * @param userId 사용자 ID
     * @param token FCM 토큰
     * @return FCM 토큰 엔티티
     */
    Optional<FcmToken> findByUserIdAndToken(Long userId, String token);

    /**
     * 사용자 ID로 모든 FCM 토큰을 조회
     * @param userId 사용자 ID
     * @return FCM 토큰 목록
     */
    List<FcmToken> findByUserId(Long userId);

    /**
     * 토큰으로 FCM 토큰을 조회
     * @param token FCM 토큰
     * @return FCM 토큰 엔티티
     */
    Optional<FcmToken> findByToken(String token);

    /**
     * 토큰으로 FCM 토큰을 삭제
     * 로그아웃이나 토큰 만료 시 사용
     * @param token 삭제할 FCM 토큰
     */
    void deleteByToken(String token);

    /**
     * 사용자 ID와 유효성 상태로 FCM 토큰을 조회
     * @param userId 사용자 ID
     * @param isValid 유효성 상태
     * @return FCM 토큰 Optional
     */
    Optional<FcmToken> findByUserIdAndIsValidTrue(Long userId);
}
