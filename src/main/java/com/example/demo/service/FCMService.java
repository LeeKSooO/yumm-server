package com.example.demo.service;

public interface FCMService {
    
    void sendChatRoomNotification(Long userId, String title, String body);

    void sendMatchNotification(Long userId, String title, String body);
}
