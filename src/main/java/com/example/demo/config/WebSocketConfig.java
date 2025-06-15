package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 설정을 담당하는 Configuration 클래스
 * STOMP 프로토콜을 사용하여 메시지 브로커링을 구현
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 메시지 브로커 설정
     * - /topic: 구독(Subscribe) 경로
     * - /app: 메시지 발행(Publish) 경로
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic"); // 구독 경로
        config.setApplicationDestinationPrefixes("/app"); // 메시지 발행 경로
    }

    /**
     * WebSocket 엔드포인트 설정
     * - /ws: WebSocket 연결 엔드포인트
     * - SockJS를 사용하여 WebSocket을 지원하지 않는 브라우저에서도 동작하도록 설정
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // CORS 설정
                .withSockJS(); // SockJS 지원 추가
    }
}
