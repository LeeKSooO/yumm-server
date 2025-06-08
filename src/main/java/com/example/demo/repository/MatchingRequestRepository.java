package com.example.demo.repository;

import com.example.demo.domain.*;
import com.example.demo.enums.MatchingRequestStatus;
import com.example.demo.enums.MatchingType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
//import java.util.Optional;

@Repository
public interface MatchingRequestRepository extends JpaRepository<MatchingRequest, Long> {

    // 특정 사용자의 활성/대기 중인 매칭 요청 조회
    List<MatchingRequest> findByUserAndIsActiveTrueAndStatus(User user, MatchingRequestStatus status);

    // 활성 상태의 대기 중인 모든 매칭 요청 조회 (매칭 알고리즘용)
    List<MatchingRequest> findByIsActiveTrueAndStatus(MatchingRequestStatus status);

    // 특정 날짜/시간에 매칭될 요청 조회 (예약 매칭용)
    List<MatchingRequest> findByIsActiveTrueAndStatusAndPreferredDateAndPreferredTime(
        MatchingRequestStatus status, LocalDateTime preferredDate, LocalDateTime preferredTime);

    // 실시간 매칭 - 매칭 상태, 매칭 타입, 만료된 요청에 대해 조회
    @Query("SELECT r FROM MatchingRequest r " +
           "WHERE r.status = :status " +
           "AND r.matchingType = :type " +
           "AND r.requestTimestamp <= :threshold")
    List<MatchingRequest> findExpiredRequests(
            @Param("status") MatchingRequestStatus status,
            @Param("type") MatchingType type,
            @Param("threshold") LocalDateTime threshold);
    
    
    // 매칭 등록 - 선호 시간이 만료 기준보다 과거인 요청 조회
    @Query("SELECT r FROM MatchingRequest r " +
           "WHERE r.status = :status " +
           "AND r.preferredTime <= :threshold")
    List<MatchingRequest> findExpiredScheduledRequests(
        @Param("status") MatchingRequestStatus status,
        @Param("threshold") LocalDateTime threshold
    );

}
