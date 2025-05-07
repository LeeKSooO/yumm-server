package com.example.demo.service;
import com.example.demo.constant.Food;
import com.example.demo.constant.Region;
import com.example.demo.constant.RequestStatus;
import com.example.demo.domain.Matching;
import com.example.demo.domain.User;
import com.example.demo.repository.MatchingRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.Duration;

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

        User user = userRepo.findById(userId).orElseThrow(()->new IllegalArgumentException("사용자를 찾을 수 없습니다: "+userId));
        Matching m = Matching.builder()
            .user(user)  // 또는 em.getReference(User.class, userId)
            .groupSize(groupSize)
            .region(region)
            .food(food)
            .status(RequestStatus.PENDING)
            .build();
        return matchingRepo.save(m);
    }

    
}
