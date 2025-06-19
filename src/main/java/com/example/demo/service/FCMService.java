package com.example.demo.service;

import java.util.List;

/**
 * FCM(Firebase Cloud Messaging) 서비스 인터페이스
 * 푸시 알림 전송 및 성능 최적화 기능을 제공
 */
public interface FCMService {
    
    /**
     * 채팅방 관련 알림을 특정 사용자에게 전송
     * @param userId 알림을 받을 사용자의 ID
     * @param title 알림의 제목
     * @param body 알림의 내용
     */
    void sendChatRoomNotification(Long userId, String title, String body);

    /**
     * 매칭 관련 알림을 특정 사용자에게 전송
     * @param userId 알림을 받을 사용자의 ID
     * @param title 알림의 제목
     * @param body 알림의 내용
     */
    void sendMatchNotification(Long userId, String title, String body);

    /**
     * 여러 사용자에게 동일한 알림을 배치로 전송
     * @param userIds 알림을 받을 사용자 ID 목록
     * @param title 알림의 제목
     * @param body 알림의 내용
     * @return 성공적으로 전송된 알림 수
     */
    int sendBatchNotification(List<Long> userIds, String title, String body);

    /**
     * 여러 사용자에게 동일한 알림을 비동기로 전송
     * @param userIds 알림을 받을 사용자 ID 목록
     * @param title 알림의 제목
     * @param body 알림의 내용
     */
    void sendBatchNotificationAsync(List<Long> userIds, String title, String body);

    void saveToken(Long userId, String token);

    String getToken(Long userId);
}
