package com.example.demo.service;

import com.example.demo.constant.Food;
import com.example.demo.constant.Region;
import com.example.demo.constant.RequestStatus;
import com.example.demo.domain.Matching;
import com.example.demo.domain.User;
import com.example.demo.dto.MatchRequestDto;
import com.example.demo.dto.MatchResponseDto;
import com.example.demo.repository.MatchingRepository;
import com.example.demo.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchingService {

    private final MatchingRepository matchingRepo;
    private final UserRepository userRepo;

    /** 30초 대기 후 지역매치 */
    private static final Duration REGION_THRESHOLD = Duration.ofSeconds(30);
    /** 1분 대기 후 취소 */
    private static final Duration CANCEL_THRESHOLD = Duration.ofSeconds(60);

    RequestStatus pending = RequestStatus.PENDING;

    /**
     * 1) 클라이언트 요청을 DB에 PENDING 상태로 저장
     */
    public MatchResponseDto createRequest(Long id,
            MatchRequestDto requestDto) {

        User user = userRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + id));

        int groupSize = requestDto.getGroupSize();
        Region region = requestDto.getRegion();
        Food food = requestDto.getFood();

        Matching m = Matching.builder()
                .user(user)
                .groupSize(groupSize)
                .region(region)
                .food(food)
                .status(RequestStatus.PENDING)
                .build();
        matchingRepo.save(m);
        return new MatchResponseDto(m.getId(), m.getStatus(), m.getCreatedAt());
    }

    /**
     * 5초마다 “풀매치(인원·지역·음식)” 시도
     */
    @Scheduled(fixedDelay = 5_000)
    @Transactional
    public void matchByAllConditions() {
        List<Integer> sizes = List.of(2, 3, 4);

        for (int size : sizes) {
            for (Region region : Region.values()) {
                for (Food food : Food.values()) {
                    // PESSIMISTIC_WRITE 잠금 조회
                    List<Matching> match = matchingRepo.findPendingMatchByAll(
                            pending,
                            size, region, food,
                            PageRequest.of(0, size));
                    if (match.size() < size) {
                        continue; // 인원이 부족하면 반복
                    }

                    // 상태 변경
                    match.forEach(m -> m.setStatus(RequestStatus.MATCHED));
                    matchingRepo.saveAll(match);

                }
            }
        }
    }

    /*
     * 30초마다 "지역매치" 시도
     */
    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void matchByRegionAndSize() {
        // 1) 30초 전 시점 계산
        LocalDateTime cutoff = LocalDateTime.now().minus(REGION_THRESHOLD);

        List<Integer> sizes = List.of(2, 3, 4);
        for (int size : sizes) {
            for (Region region : Region.values()) {
                // 2) 인원+지역 기준 조회
                List<Matching> exceptFood = matchingRepo.findPendingMatchExceptFood(
                        pending, size, region,
                        PageRequest.of(0, size * 2));

                // 3) 30초 이상 대기한 것만 골라서 정확히 size개로 제한
                List<Matching> match = exceptFood.stream()
                        .filter(m -> m.getCreatedAt().isBefore(cutoff))
                        .limit(size)
                        .toList();

                // 4) 필터링된 match 기준으로만 매칭
                if (match.size() < size) {
                    continue;
                }

                match.forEach(m -> m.setStatus(RequestStatus.MATCHED));
                matchingRepo.saveAll(match);

            }
        }
    }

    /**
     * 30초마다 “1분 이상 대기” 요청은 CANCELED로 전환
     */
    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void cancelTimedOutRequests() {
        LocalDateTime cutoff = LocalDateTime.now().minus(CANCEL_THRESHOLD);
        List<Matching> cancelMatching = matchingRepo.findByStatusAndCreatedAtBefore(
                RequestStatus.PENDING, cutoff);
        // 1분(또는 90초) 이상 대기 중인 요청이 없으면 바로 종료
        if (cancelMatching.isEmpty()) {
            return;
        }
        // PENDING 상태였던 매칭 요청들을 CANCELED 로 변경
        cancelMatching.forEach(m -> m.setStatus(RequestStatus.CANCELED));
        matchingRepo.saveAll(cancelMatching);

    }

    /**
     * 본인이 보낸 PENDING 매칭 요청을 취소
     */
    @Transactional
    public void cancelRequest(Long userId, Long matchingId) {
        Matching m = matchingRepo.findById(matchingId)
                .orElseThrow(() -> new EntityNotFoundException("매칭 요청을 찾을 수 없습니다: " + matchingId));

        // 권한 체크: 본인 요청만
        if (!m.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("자신의 요청만 취소할 수 있습니다.");
        }
        // 상태 체크: 이미 MATCHED 혹은 CANCELED 상태면 취소 불가
        if (m.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("현재 상태에서는 취소할 수 없습니다: " + m.getStatus());
        }

        m.setStatus(RequestStatus.CANCELED);
        matchingRepo.save(m);
    }
}
