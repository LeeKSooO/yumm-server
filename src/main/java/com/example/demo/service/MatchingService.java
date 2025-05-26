package com.example.demo.service;

import com.example.demo.constant.RequestStatus;
import com.example.demo.dto.MatchRequestDto;
import com.example.demo.dto.MatchResponseDto;
import com.example.demo.dto.MatchStatusDto;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public interface MatchingService {

    RequestStatus pending = RequestStatus.PENDING;

    /**
     * 1) 클라이언트 요청을 DB에 PENDING 상태로 저장
     */
    public MatchResponseDto createRequest(Long id,
            MatchRequestDto requestDto);

    /**
     * 5초마다 “풀매치(인원·지역·음식)” 시도
     */
    @Scheduled(fixedDelay = 5_000)
    @Transactional
    public void matchByAllConditions();

    /*
     * 30초마다 "지역매치" 시도
     */
    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void matchByRegionAndSize();

    /**
     * 30초마다 “1분 이상 대기” 요청은 CANCELED로 전환
     */
    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void cancelTimedOutRequests();

    /**
     * 본인이 보낸 매칭 요청을 취소
     */
    @Transactional
    public void cancelRequest(Long userId, Long matchingId);

    /*
     * 내 매칭 상태 조회
     */
    @Transactional(readOnly = true)
    public MatchStatusDto getRequestStatus(Long userId);
}
