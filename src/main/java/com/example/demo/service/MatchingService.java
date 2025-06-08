package com.example.demo.service;

import com.example.demo.dto.matching.MatchingRequestCreateRequest;
import com.example.demo.dto.matching.MatchingResultResponse;

/**
 * 매칭 시스템 관련 서비스 인터페이스
 * 
 * 실시간 매칭, 매칭 등록제 기능을 정의합니다.
 */
public interface MatchingService {
    
    /**
     * 실시간 매칭을 요청합니다.
     * 1분 이내에 매칭이 성공하면 채팅방이 생성되고, 실패하면 요청이 만료됩니다.
     *
     * @param userId 매칭을 요청한 사용자의 ID
     * @param request 매칭 요청 정보를 담은 DTO
     */
    void requestInstantMatching(Long userId, MatchingRequestCreateRequest request);

    /**
     * 실시간 매칭 요청을 취소합니다.
     *
     * @param userId 매칭을 취소하려는 사용자의 ID
     * @param requestId 취소할 매칭 요청의 ID
     */
    void cancelInstantMatching(Long userId, Long requestId);

    /**
     * 매칭 등록제에 매칭을 등록합니다.
     * 매 시간 배치 처리되며, 선호 시간 2시간 전까지 유효합니다.
     *
     * @param userId 매칭을 등록한 사용자의 ID
     * @param request 매칭 요청 정보를 담은 DTO
     */
    void registerScheduledMatching(Long userId, MatchingRequestCreateRequest request);

    /**
     * 등록된 매칭 요청을 취소합니다.
     *
     * @param userId 매칭을 취소하려는 사용자의 ID
     * @param requestId 취소할 매칭 요청의 ID
     */
    void cancelScheduledMatching(Long userId, Long requestId);

    /**
     * 매칭 결과를 조회합니다.
     *
     * @param requestId 조회할 매칭 요청의 ID
     * @return 매칭 결과 정보를 담은 DTO
     */
    MatchingResultResponse getMatchingResult(Long requestId);
}
