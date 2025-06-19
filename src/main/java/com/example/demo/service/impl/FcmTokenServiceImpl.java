package com.example.demo.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import com.example.demo.domain.FcmToken;
import com.example.demo.service.FcmTokenService;
import com.example.demo.repository.FcmTokenRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.domain.User;
import com.example.demo.exception.CustomException;
import com.example.demo.exception.ErrorCode;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * FCM 토큰 관리 서비스의 구현체
 * 토큰의 저장, 조회, 만료 처리 등의 기능을 실제로 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FcmTokenServiceImpl implements FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;

    // FCM 토큰 형식 검증을 위한 정규식 패턴
    private static final Pattern FCM_TOKEN_PATTERN = Pattern.compile("^[A-Za-z0-9-_]+$");

    /**
     * 새로운 FCM 토큰을 저장하거나 기존 토큰을 업데이트
     * 토큰은 1개월 후 자동으로 만료되도록 설정됨
     * 
     * @param userId 사용자 ID
     * @param newToken 새로운 FCM 토큰
     * @throws CustomException 사용자를 찾을 수 없는 경우
     */
    @Override
    public void saveOrUpdateToken(Long userId, String newToken) {
        if (!validateTokenFormat(newToken)) {
            log.warn("유효하지 않은 FCM 토큰 형식입니다. 사용자 ID: {}", userId);
            return;
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Optional<FcmToken> existingToken = fcmTokenRepository.findByUserIdAndToken(userId, newToken);
        if (existingToken.isEmpty()) {
            FcmToken fcmToken = new FcmToken();
            fcmToken.setUser(user);
            fcmToken.setToken(newToken);
            fcmToken.setExpiresAt(LocalDateTime.now().plusMonths(1)); // 1개월 후 만료
            fcmTokenRepository.save(fcmToken);
            log.info("새로운 FCM 토큰이 저장되었습니다. 사용자 ID: {}", userId);
        }
    }

    /**
     * 사용자의 모든 유효한 FCM 토큰을 조회
     * 만료된 토큰은 자동으로 제외됨
     * 
     * @param userId 사용자 ID
     * @return 유효한 FCM 토큰 목록
     */
    @Override
    public List<String> getTokensByUserId(Long userId) {
        return fcmTokenRepository.findByUserId(userId)
            .stream()
            .filter(token -> !token.isExpired())
            .map(FcmToken::getToken) 
            .collect(Collectors.toList());
    }

    /**
     * 특정 FCM 토큰을 만료 처리
     * 토큰이 존재하는 경우에만 처리됨
     * 
     * @param token 만료 처리할 FCM 토큰
     */
    @Override
    public void invalidateToken(String token) {
        fcmTokenRepository.findByToken(token)
            .ifPresent(fcmToken -> {
                fcmToken.invalidate();
                fcmTokenRepository.save(fcmToken);
                log.info("FCM 토큰이 만료 처리되었습니다: {}", token);
            });
    }

    /**
     * 만료된 모든 FCM 토큰을 정리
     * 매일 자정에 자동으로 실행됨
     * 만료된 토큰은 데이터베이스에서 완전히 삭제됨
     */
    @Override
    @Scheduled(cron = "0 0 0 * * ?") // 매일 자정에 실행
    public void cleanupExpiredTokens() {
        List<FcmToken> expiredTokens = fcmTokenRepository.findAll().stream()
            .filter(FcmToken::isExpired)
            .collect(Collectors.toList());

        for (FcmToken token : expiredTokens) {
            fcmTokenRepository.delete(token);
            log.info("만료된 FCM 토큰이 삭제되었습니다: {}", token.getToken());
        }
    }

    @Override
    public boolean validateToken(String token) {
        if (!validateTokenFormat(token)) {
            return false;
        }

        try {
            // Firebase 서버에 테스트 메시지 전송 시도
            Message message = Message.builder()
                .setToken(token)
                .build();
            
            FirebaseMessaging.getInstance().send(message);
            return true;
        } catch (Exception e) {
            log.warn("FCM 토큰 유효성 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean validateTokenFormat(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        // FCM 토큰은 일반적으로 140자 이상
        if (token.length() < 140) {
            return false;
        }

        // FCM 토큰은 알파벳, 숫자, 하이픈, 언더스코어로만 구성
        return FCM_TOKEN_PATTERN.matcher(token).matches();
    }

    @Override
    public String getValidToken(Long userId) {
        log.debug("사용자의 유효한 FCM 토큰 조회 - userId: {}", userId);
        
        FcmToken token = fcmTokenRepository.findByUserIdAndIsValidTrue(userId)
                .orElse(null);
                
        if (token == null || token.isExpired()) {
            log.debug("유효한 FCM 토큰이 없거나 만료됨 - userId: {}", userId);
            return null;
        }
        
        return token.getToken();
    }
}
