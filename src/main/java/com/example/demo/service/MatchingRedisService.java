package com.example.demo.service;

import com.example.demo.domain.MatchingRequest;
import com.example.demo.enums.MatchingType;
import java.util.List;

public interface MatchingRedisService {
    // 실시간 매칭 요청 저장
    void saveInstantMatchingRequest(MatchingRequest request);
    // 실시간 매칭 요청 삭제
    void removeInstantMatchingRequest(Long requestId, MatchingType type);
    // 예약(등록제) 매칭 요청 저장
    void saveScheduledMatchingRequest(MatchingRequest request);
    // 예약(등록제) 매칭 요청 삭제
    void removeScheduledMatchingRequest(Long requestId, MatchingType type);
    // 실시간 매칭 요청 전체 조회
    List<MatchingRequest> getInstantMatchingRequests(MatchingType type);
    // 예약(등록제) 매칭 요청 전체 조회
    List<MatchingRequest> getScheduledMatchingRequests(MatchingType type);
    // 매칭 요청별 벡터 조회
    double[] getMatchingVector(Long requestId);
} 