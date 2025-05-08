package com.example.demo.service;

import com.example.demo.constant.Food;
import com.example.demo.constant.Region;
import com.example.demo.constant.RequestStatus;
import com.example.demo.domain.Matching;
import com.example.demo.domain.User;
import com.example.demo.repository.MatchingRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchingService {

    private final MatchingRepository matchingRepo;
    private final UserRepository userRepo;
    private final ChatRoomService chatRoomService;

    /** 30초 대기 후 지역매치 */
    private static final Duration REGION_THRESHOLD = Duration.ofSeconds(30);
    /** 1분 대기 후 취소 */
    private static final Duration CANCEL_THRESHOLD = Duration.ofMinutes(1);

    /**
     * 1) 클라이언트 요청을 DB에 PENDING 상태로 저장
     */
    public Matching createRequest(Long userId,
            int groupSize,
            Region region,
            Food food) {

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        Matching m = Matching.builder()
                .user(user)
                .groupSize(groupSize)
                .region(region)
                .food(food)
                .status(RequestStatus.PENDING)
                .build();
        return matchingRepo.save(m);
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
                            RequestStatus.PENDING,
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
}
