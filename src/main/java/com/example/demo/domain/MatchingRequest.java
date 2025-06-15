package com.example.demo.domain;

import com.example.demo.enums.Gender;
import com.example.demo.enums.MatchingRequestStatus;
import com.example.demo.enums.MatchingType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime; // 날짜/시간 타입으로 변경

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchingRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String preferredRegion; // '상관 없음' 포함

    @Column(nullable = false)
    private LocalDateTime preferredDate; // 날짜/시간 타입으로 변경

    @Column(nullable = false)
    private LocalDateTime preferredTime; // 날짜/시간 타입으로 변경

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender preferredGender; // Enum 타입으로 변경

    @Column(nullable = false)
    private int preferredAgeMin; // '상관 없음' 시 0 등으로 처리

    @Column(nullable = false)
    private int preferredAgeMax; // '상관 없음' 시 999 등으로 처리

    @Column(nullable = false)
    private String preferredFood; // '상관 없음' 포함

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchingType matchingType; // Enum 타입으로 변경

    @Column(nullable = false)
    private int groupSize; // 매칭 그룹 인원수, NOT NULL, DEFAULT 2 (DB DDL에서 설정)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchingRequestStatus status; // Enum 타입으로 변경

    @Column(nullable = false)
    private boolean isActive; // boolean 타입으로 변경

    @Column(nullable = false)
    private LocalDateTime requestTimestamp; // 날짜/시간 타입으로 변경

    @Column(columnDefinition = "jsonb") // PostgreSQL의 경우. 다른 DB는 TEXT 또는 JSON
    private String vectorRepresentation; // JSON 문자열로 저장 (예: "[0.1, 0.5, ...]")

    @Column(columnDefinition = "jsonb") // PostgreSQL의 경우. 다른 DB는 TEXT 또는 JSON
    private String weightConfig; // JSON 문자열로 저장 (예: "{\"region\": 0.3, ...}")

    // 편의 메서드 (상태 변경 등)
    public void updateStatus(MatchingRequestStatus status) {
        this.status = status;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }
}
