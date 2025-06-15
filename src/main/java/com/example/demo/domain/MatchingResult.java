package com.example.demo.domain;

//import com.example.demo.enums.MatchingRequestStatus; // 상태 관리 필요시
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MatchingResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "match_id") // 컬럼명 명확화
    private Long id;

    @OneToOne(fetch = FetchType.LAZY) // 1:1 관계. OneToOne의 대상은 고유해야 함.
    @JoinColumn(name = "matching_request_id", nullable = false, unique = true) // FK 명시, unique=true로 1:1 보장
    private MatchingRequest matchingRequest; // MatchingRequest 엔티티 직접 참조

    @Column(nullable = false)
    private String status; // 매칭 결과 상태 (성공, 실패 등)

    @Column(nullable = false)
    private LocalDateTime matchingTimestamp; // 날짜/시간 타입으로 변경

    // 편의 메서드
    public void updateStatus(String status) {
        this.status = status;
    }
}