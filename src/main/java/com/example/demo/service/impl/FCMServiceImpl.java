package com.example.demo.service.impl;

import com.example.demo.repository.UserRepository;
import com.example.demo.service.FCMService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


/**
 * FCMService 인터페이스의 구현체. Firebase Cloud Messaging을 사용하여 푸시 알림을 전송하는 기능을 제공합니다.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class FCMServiceImpl implements FCMService {

    private final UserRepository userRepository;

    /**
     * 채팅방 관련 알림을 특정 사용자에게 전송합니다.
     *
     * @param userId 알림을 받을 사용자의 ID
     * @param title  알림의 제목
     * @param body   알림의 내용
     */
    @Override
    public void sendChatRoomNotification(Long userId, String title, String body) {
        try {
            Message message = Message.builder()
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putData("type", "CHAT_ROOM")
                    .putData("userId", userId.toString())
                    .setToken(getUserFCMToken(userId)) // 사용자의 FCM 토큰을 가져오는 메서드 필요
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Successfully sent chat room notification: {}", response);
        } catch (Exception e) {
            log.error("Failed to send chat room notification", e);
        }
    }

    /**
     * 매칭 관련 알림을 특정 사용자에게 전송합니다.
     *
     * @param userId 알림을 받을 사용자의 ID
     * @param title  알림의 제목
     * @param body   알림의 내용
     */
    @Override
    public void sendMatchNotification(Long userId, String title, String body) {
        try {
            Message message = Message.builder()
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putData("type", "MATCHED")
                    .putData("userId", userId.toString())
                    .setToken(getUserFCMToken(userId)) // 사용자의 FCM 토큰을 가져오는 메서드 필요
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Successfully sent match notification: {}", response);
        } catch (Exception e) {
            log.error("Failed to send match notification", e);
        }
    }

    private String getUserFCMToken(Long userId) {
        return userRepository.findFcmTokenById(userId)
        .orElse(null); // 토큰이 없을 경우 null 반환
    }
} 