package com.example.demo.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.constant.*;
import com.example.demo.domain.Matching;

import jakarta.persistence.LockModeType;

public interface MatchingRepository extends JpaRepository<Matching, Long> {
  /**
   * 1) 풀매치: 인원수(count), 지역, 음식이 모두 일치하는 PENDING 요청을 size개만 잠금 조회
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE) // 동시성 제어를 하기 위한 기능
  @Query("""
        SELECT m
          FROM Matching m
         WHERE m.status = :status
           AND m.groupSize  = :size
           AND m.region = :region
           AND m.food   = :food
      """)
  List<Matching> findPendingMatchByAll(
      @Param("status") RequestStatus status,
      @Param("size") int size,
      @Param("region") Region region,
      @Param("food") Food food,
      Pageable pageable);

  /**
   * 2) 지역 매치: 인원수(count), 지역이 일치하는 PENDING 요청을 size개만 잠금 조회
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE) // 동시성 제어를 하기 위한 기능
  @Query("""
        SELECT m
          FROM Matching m
         WHERE m.status = :status
           AND m.groupSize  = :size
           AND m.region = :region
      """)
  List<Matching> findPendingMatchExceptFood(
      @Param("status") RequestStatus status,
      @Param("size") int size,
      @Param("region") Region region,
      Pageable pageable);

  /**
   * 4) createdAt 이전의 PENDING 요청 조회
   */
  @Query("""
        SELECT m
          FROM Matching m
         WHERE m.status    = :status
           AND m.createdAt < :cutoff
      """)
  List<Matching> findByStatusAndCreatedAtBefore(
      @Param("status") RequestStatus status,
      @Param("cutoff") LocalDateTime cutoff);

  /**
   * 5) 내 매칭 요청 중 가장 최근 것 한 건
   */
  Optional<Matching> findTopByUser_IdOrderByCreatedAtDesc(Long userId);
}
