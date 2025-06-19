package com.example.demo.service;

import com.example.demo.service.impl.FCMServiceImpl;
import com.example.demo.repository.UserRepository;
import com.example.demo.domain.User;
import com.example.demo.domain.FcmToken;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
public class FCMServiceTest {

    @InjectMocks
    private FCMServiceImpl fcmService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSendMatchNotification() throws Exception {
        // Given
        Long userId = 1L;
        String title = "매칭 성공!";
        String body = "새로운 매칭이 성사되었습니다.";
        String fcmToken = "test_fcm_token";

        User user = new User();
        FcmToken token = new FcmToken();
        token.setToken(fcmToken);
        user.getFcmTokens().add(token);

        when(userRepository.findFcmTokenById(userId)).thenReturn(Optional.of(fcmToken));
        when(firebaseMessaging.send(any(Message.class))).thenReturn("message-id");

        // When
        fcmService.sendMatchNotification(userId, title, body);

        // Then
        verify(userRepository, times(1)).findFcmTokenById(userId);
        verify(firebaseMessaging, times(1)).send(any(Message.class));
    }

    @Test
    void testSendChatRoomNotification() throws Exception {
        // Given
        Long userId = 1L;
        String title = "새로운 메시지";
        String body = "채팅방에 새로운 메시지가 도착했습니다.";
        String fcmToken = "test_fcm_token";

        when(userRepository.findFcmTokenById(userId)).thenReturn(Optional.of(fcmToken));
        when(firebaseMessaging.send(any(Message.class))).thenReturn("message-id");

        // When
        fcmService.sendChatRoomNotification(userId, title, body);

        // Then
        verify(userRepository, times(1)).findFcmTokenById(userId);
        verify(firebaseMessaging, times(1)).send(any(Message.class));
    }
} 