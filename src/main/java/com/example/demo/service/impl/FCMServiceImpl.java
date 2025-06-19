package com.example.demo.service.impl;

import com.example.demo.repository.UserRepository;
import com.example.demo.service.FCMService;
import com.example.demo.service.FcmTokenService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.BatchResponse;
import com.example.demo.config.FCMRetryConfig;
import com.example.demo.util.EncryptionUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * FCM(Firebase Cloud Messaging) 서비스의 구현체
 * Firebase Cloud Messaging을 사용하여 푸시 알림을 전송하는 기능을 제공
 * 토큰 만료 처리 및 에러 핸들링을 포함
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class FCMServiceImpl implements FCMService {

    private final UserRepository userRepository;
    private final FcmTokenService fcmTokenService;
    private final FCMRetryConfig retryConfig;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private static final int BATCH_SIZE = 500;
    private final EncryptionUtil encryptionUtil;

    /**
     * 채팅방 관련 알림을 특정 사용자에게 전송
     * 토큰이 없거나 만료된 경우 알림을 전송하지 않음
     * 
     * @param userId 알림을 받을 사용자의 ID
     * @param title 알림의 제목
     * @param body 알림의 내용
     */
    @Override
    public void sendChatRoomNotification(Long userId, String title, String body) {
        String token = fcmTokenService.getValidToken(userId);
        if (token == null) {
            log.warn("유효한 FCM 토큰이 없습니다 - userId: {}", userId);
            return;
        }

        Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();

        sendWithRetry(message, 1);
    }

    /**
     * 매칭 관련 알림을 특정 사용자에게 전송
     * 토큰이 없거나 만료된 경우 알림을 전송하지 않음
     * 
     * @param userId 알림을 받을 사용자의 ID
     * @param title 알림의 제목
     * @param body 알림의 내용
     */
    @Override
    public void sendMatchNotification(Long userId, String title, String body) {
        try {
            String token = getUserFCMToken(userId);
            if (token == null) {
                log.warn("사용자 {}의 FCM 토큰이 없습니다.", userId);
                return;
            }

            Message message = Message.builder()
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putData("type", "MATCHED")
                    .putData("userId", userId.toString())
                    .setToken(token)
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Successfully sent match notification: {}", response);
        } catch (FirebaseMessagingException e) {
            handleFCMError(e, userId);
        } catch (Exception e) {
            log.error("Failed to send match notification", e);
        }
    }

    /**
     * 사용자의 FCM 토큰을 조회
     * 
     * @param userId 사용자 ID
     * @return FCM 토큰 문자열, 토큰이 없는 경우 null
     */
    private String getUserFCMToken(Long userId) {
        return userRepository.findFcmTokenById(userId)
            .orElse(null);
    }

    /**
     * FCM 메시지 전송 실패 시 에러 처리
     * 토큰이 유효하지 않거나 등록되지 않은 경우 토큰을 만료 처리
     * 
     * @param e Firebase 메시징 예외
     * @param userId 사용자 ID
     */
    private void handleFCMError(FirebaseMessagingException e, Long userId) {
        String token = getUserFCMToken(userId);
        if (token != null) {
            if (e.getMessage().contains("INVALID_ARGUMENT") || 
                e.getMessage().contains("UNREGISTERED")) {
                log.warn("FCM 토큰이 만료되었습니다. 사용자 ID: {}, 토큰: {}", userId, token);
                fcmTokenService.invalidateToken(token);
            } else {
                log.error("FCM 메시지 전송 실패. 사용자 ID: {}, 에러: {}", userId, e.getMessage());
            }
        }
    }

    /**
     * FCM 메시지 전송을 재시도하는 메서드
     * @param message 전송할 메시지
     * @param attempt 현재 시도 횟수
     * @return 전송 성공 여부
     */
    private boolean sendWithRetry(Message message, int attempt) {
        try {
            FirebaseMessaging.getInstance().send(message);
            return true;
        } catch (FirebaseMessagingException e) {
            if (attempt < retryConfig.getMaxAttempts() && isRetryableError(e)) {
                long waitTime = calculateWaitTime(attempt);
                log.warn("FCM 메시지 전송 실패, {}ms 후 재시도 (시도 {}/{}) - 에러: {}", 
                        waitTime, attempt + 1, retryConfig.getMaxAttempts(), e.getMessage());
                
                try {
                    Thread.sleep(waitTime);
                    return sendWithRetry(message, attempt + 1);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("재시도 대기 중 인터럽트 발생", ie);
                    return false;
                }
            }
            log.error("FCM 메시지 전송 최종 실패 (시도 {}/{})", 
                    attempt, retryConfig.getMaxAttempts(), e);
            return false;
        }
    }

    /**
     * 배치 메시지 전송을 재시도하는 메서드
     * @param messages 전송할 메시지 목록
     * @param attempt 현재 시도 횟수
     * @return 성공적으로 전송된 메시지 수
     */
    private int sendBatchWithRetry(List<Message> messages, int attempt) {
        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendAll(messages);
            return response.getSuccessCount();
        } catch (FirebaseMessagingException e) {
            if (attempt < retryConfig.getMaxAttempts() && isRetryableError(e)) {
                long waitTime = calculateWaitTime(attempt);
                log.warn("FCM 배치 메시지 전송 실패, {}ms 후 재시도 (시도 {}/{}) - 에러: {}", 
                        waitTime, attempt + 1, retryConfig.getMaxAttempts(), e.getMessage());
                
                try {
                    Thread.sleep(waitTime);
                    return sendBatchWithRetry(messages, attempt + 1);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("재시도 대기 중 인터럽트 발생", ie);
                    return 0;
                }
            }
            log.error("FCM 배치 메시지 전송 최종 실패 (시도 {}/{})", 
                    attempt, retryConfig.getMaxAttempts(), e);
            return 0;
        }
    }

    /**
     * 재시도 가능한 에러인지 확인
     * @param e FirebaseMessagingException
     * @return 재시도 가능 여부
     */
    private boolean isRetryableError(FirebaseMessagingException e) {
        String errorMessage = e.getMessage().toLowerCase();
        return errorMessage.contains("internal") || 
               errorMessage.contains("unavailable") ||
               errorMessage.contains("timeout") ||
               errorMessage.contains("network");
    }

    /**
     * 재시도 대기 시간 계산 (지수 백오프)
     * @param attempt 현재 시도 횟수
     * @return 대기 시간 (밀리초)
     */
    private long calculateWaitTime(int attempt) {
        long waitTime = (long) (retryConfig.getInitialInterval() * 
                Math.pow(retryConfig.getMultiplier(), attempt - 1));
        return Math.min(waitTime, retryConfig.getMaxInterval());
    }

    @Override
    public int sendBatchNotification(List<Long> userIds, String title, String body) {
        log.info("배치 알림 전송 시작 - 사용자 수: {}", userIds.size());
        
        List<String> tokens = userIds.stream()
                .map(fcmTokenService::getValidToken)
                .filter(token -> token != null && !token.isEmpty())
                .collect(Collectors.toList());

        if (tokens.isEmpty()) {
            log.warn("유효한 FCM 토큰이 없습니다.");
            return 0;
        }

        List<Message> messages = tokens.stream()
                .map(token -> Message.builder()
                        .setToken(token)
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .build())
                .collect(Collectors.toList());

        return sendBatchWithRetry(messages, 1);
    }

    @Override
    @Async
    public void sendBatchNotificationAsync(List<Long> userIds, String title, String body) {
        log.info("비동기 배치 알림 전송 시작 - 사용자 수: {}", userIds.size());
        
        // 사용자 목록을 배치 크기로 분할
        List<List<Long>> batches = new ArrayList<>();
        for (int i = 0; i < userIds.size(); i += BATCH_SIZE) {
            batches.add(userIds.subList(i, Math.min(i + BATCH_SIZE, userIds.size())));
        }

        // 각 배치를 비동기로 처리
        List<CompletableFuture<Integer>> futures = batches.stream()
                .map(batch -> CompletableFuture.supplyAsync(() -> 
                    sendBatchNotification(batch, title, body), executorService))
                .collect(Collectors.toList());

        // 모든 배치의 처리 결과를 기다림
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    int totalSuccess = futures.stream()
                            .mapToInt(CompletableFuture::join)
                            .sum();
                    log.info("비동기 배치 알림 전송 완료 - 총 성공: {}", totalSuccess);
                })
                .exceptionally(throwable -> {
                    log.error("비동기 배치 알림 전송 중 오류 발생", throwable);
                    return null;
                });
    }

    @Override
    public void saveToken(Long userId, String token) {
        String encryptedToken = encryptionUtil.encrypt(token);
        userRepository.updateFcmToken(userId, encryptedToken);
    }
    
    @Override
    public String getToken(Long userId) {
        String encryptedToken = userRepository.findFcmTokenById(userId)
            .orElse(null);
        return encryptionUtil.decrypt(encryptedToken);
    }
} 