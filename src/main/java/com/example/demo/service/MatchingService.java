package com.example.demo.service;

import com.example.demo.dto.MatchRequestDto;
import com.example.demo.dto.MatchResponseDto;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

public interface MatchingService {

    /* 매칭 요청 생성 */
    MatchResponseDto createRequest(Long id, MatchRequestDto requestDto);

    /* 5초마다 “풀매치(인원·지역·음식)” 시도 */
    @Scheduled(fixedDelay = 5_000)
    @Transactional
    void matchByAllConditions();

    /* 30초마다 "지역매치" 시도 */
    @Scheduled(fixedDelay = 30_000)
    @Transactional
    void matchByRegionAndSize();

    /* 30초마다 “1분 이상 대기” 요청은 CANCELED로 전환 */
    @Scheduled(fixedDelay = 30_000)
    @Transactional
    void cancelTimedOutRequests();

    /* 본인이 보낸 매칭 요청을 취소 */
    @Transactional
    void cancelRequest(Long userId, Long matchingId);
}
