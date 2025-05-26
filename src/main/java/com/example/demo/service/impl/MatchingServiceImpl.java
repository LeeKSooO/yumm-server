package com.example.demo.service.impl;

import com.example.demo.domain.User;
import org.springframework.stereotype.Service;
import com.example.demo.dto.MatchRequestDto;
import com.example.demo.dto.MatchResponseDto;
import com.example.demo.dto.MatchStatusDto;
import com.example.demo.exception.CustomException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.service.ChatRoomService;
import com.example.demo.service.MatchingService;
import lombok.RequiredArgsConstructor;
import com.example.demo.constant.Food;
import com.example.demo.constant.Region;
import com.example.demo.constant.RequestStatus;
import com.example.demo.domain.Matching;
import com.example.demo.repository.MatchingRepository;
import com.example.demo.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchingServiceImpl implements MatchingService {
    private final MatchingRepository matchingRepo;
    private final UserRepository userRepo;
    private final ChatRoomService chatRoomService;
    /** 30초 대기 후 지역매치 */
    private static final Duration REGION_THRESHOLD = Duration.ofSeconds(30);
    /** 1분 대기 후 취소 */
    private static final Duration CANCEL_THRESHOLD = Duration.ofSeconds(60);

    RequestStatus pending = RequestStatus.PENDING;

    /* 매칭 요청 생성 */
    @Override
    public MatchResponseDto createRequest(Long id, MatchRequestDto requestDto) {

        User user = userRepo.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

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

    /* 5초마다 “풀매치(인원·지역·음식)” 시도 */
    @Override
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

                    List<User> matchingUsers = match.stream()
                            .map(m -> m.getUser())
                            .toList();
                    Long groupId = match.get(0).getId();
                    match.forEach(m -> {
                        // 상태 변경
                        m.setStatus(RequestStatus.MATCHED);
                        // 매칭 인원리스트에 유저 추가
                        m.setGroupId(groupId);
                        // 매칭 인원중 대표 인원 id 값을 groupId으로 지정
                        m.setMatchedUsers(matchingUsers);
                    });
                    matchingRepo.saveAll(match);

                }
            }
        }
    }

    /* 30초마다 "지역매치" 시도 */
    @Override
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

                List<User> matchingUsers = match.stream()
                        .map(m -> m.getUser())
                        .toList();
                Long groupId = match.get(0).getId();
                match.forEach(m -> {
                    // 상태 변경
                    m.setStatus(RequestStatus.MATCHED);
                    // 매칭 인원리스트에 유저 추가
                    m.setMatchedUsers(matchingUsers);
                    // 매칭 인원중 대표 인원 id 값을 groupId으로 지정
                    m.setGroupId(groupId);
                });
                // matchingRepository에 저장
                matchingRepo.saveAll(match);

            }
        }
    }

    /* 30초마다 “1분 이상 대기” 요청은 CANCELED로 전환 */
    @Override
    public void cancelTimedOutRequests() {
        LocalDateTime cutoff = LocalDateTime.now().minus(CANCEL_THRESHOLD);
        List<Matching> cancelMatching = matchingRepo.findByStatusAndCreatedAtBefore(RequestStatus.PENDING, cutoff);
        // 1분(또는 90초) 이상 대기 중인 요청이 없으면 바로 종료
        if (cancelMatching.isEmpty())
            return;
        // PENDING 상태였던 매칭 요청들을 CANCELED 로 변경
        cancelMatching.forEach(m -> m.setStatus(RequestStatus.CANCELED));

        matchingRepo.saveAll(cancelMatching);

    }

    /* 본인이 보낸 매칭 요청을 취소 */
    @Override
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

    /* 본인의 현재 매칭 상태 확인 */
    @Override
    public MatchStatusDto getRequestStatus(Long userId) {
        // 1) 가장 최근 요청 가져오기
        Matching matching = matchingRepo
                .findTopByUser_IdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.MATCHING_NOT_FOUND));

        // 2) 매칭 상태와 생성 시각은 공통
        Long id = matching.getId();
        RequestStatus status = matching.getStatus();
        LocalDateTime createdAt = matching.getCreatedAt();
        Long groupId = null;
        // 3) MATCHED 상태일 때만 matchedUserIds 채우기
        if (status == RequestStatus.MATCHED) {
            groupId = matching.getGroupId();

        }

        return new MatchStatusDto(id, status, createdAt, groupId);
    };
}
