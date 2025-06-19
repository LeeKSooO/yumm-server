package com.example.demo.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
@ConfigurationProperties(prefix = "fcm.retry")
public class FCMRetryConfig {
    
    /**
     * 최대 재시도 횟수
     */
    private int maxAttempts = 3;
    
    /**
     * 초기 재시도 대기 시간 (밀리초)
     */
    private long initialInterval = 1000;
    
    /**
     * 최대 재시도 대기 시간 (밀리초)
     */
    private long maxInterval = 10000;
    
    /**
     * 재시도 간격 증가 배수
     */
    private double multiplier = 2.0;
} 