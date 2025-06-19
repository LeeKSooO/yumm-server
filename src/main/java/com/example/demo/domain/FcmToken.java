package com.example.demo.domain;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * FCM(Firebase Cloud Messaging) 토큰을 관리하는 엔티티
 * 사용자별로 여러 개의 FCM 토큰을 가질 수 있으며, 각 토큰은 만료 시간과 유효성 상태를 가짐
 */
@Entity
@Table(name = "fcm_tokens")
@Getter
@Setter
public class FcmToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 이 토큰이 속한 사용자
     * ManyToOne 관계로 설정되어 있어 한 사용자가 여러 개의 FCM 토큰을 가질 수 있음
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * FCM 토큰 문자열
     * Firebase에서 발급받은 고유한 토큰 값
     */
    @Column(nullable = false, unique = true, length = 255)
    private String token;

    /**
     * 토큰 생성 시간
     * JPA가 자동으로 생성 시간을 주입
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 토큰 마지막 업데이트 시간
     * 토큰이 갱신되거나 상태가 변경될 때마다 자동으로 업데이트
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 토큰 만료 시간
     * 이 시간이 지나면 토큰은 자동으로 만료됨
     */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /**
     * 토큰 유효성 상태
     * false로 설정되면 토큰이 무효화됨
     */
    @Column(nullable = false)
    private boolean isValid = true;

    /**
     * 토큰이 만료되었는지 확인
     * 현재 시간이 만료 시간을 지났거나 토큰이 무효화된 경우 true 반환
     * @return 토큰 만료 여부
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt) || !isValid;
    }

    /**
     * 토큰을 무효화
     * 토큰이 더 이상 사용되지 않음을 표시하고 업데이트 시간을 갱신
     */
    public void invalidate() {
        this.isValid = false;
        this.updatedAt = LocalDateTime.now();
    }
}
