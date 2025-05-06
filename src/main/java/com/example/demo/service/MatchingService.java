package com.example.demo.service;

//import com.example.demo.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MatchingService {

    private final ChatRoomService chatRoomService;

    // 인원 수별 대기열 저장: key = 2, 3, 4명 / value = 해당 인원 수의 대기 유저 리스트
    private final Map<Integer, Queue<Long>> queueMap = new HashMap<>();

    // 더미 매칭 ID 생성용 카운터
    private long fakeMatchingIdCounter = 1L;

    /**
     * 유저가 매칭을 요청했을 때 실행되는 로직
     * @param userId 유저 ID
     * @param requestedCount 희망 매칭 인원 (2~4)
     * @return 채팅방 ID (바로 매칭되지 않으면 null)
     */
    // synchronized << Java Keyword로 메서드에 동기화 적용,
    // 여러 스레드가 동시에 이 메서드를 호출할 때, 동시 접근을 막고 하나씩 처리하게 한다.
    public synchronized Long requestMatching(Long userId, int requestedCount) {
        // 유효한 요청인지 확인
        if (requestedCount < 2 || requestedCount > 4) {
            throw new IllegalArgumentException("매칭 인원은 2~4명 사이여야 합니다.");
        }

        // 인원 수에 맞는 큐 초기화 또는 불러오기
        queueMap.putIfAbsent(requestedCount, new LinkedList<>());
        Queue<Long> queue = queueMap.get(requestedCount);

        // 유저 추가
        queue.add(userId);

        // 큐가 가득 찼으면 매칭 실행
        if (queue.size() >= requestedCount) {
            List<Long> matchedUsers = new ArrayList<>();
            for (int i = 0; i < requestedCount; i++) {
                matchedUsers.add(queue.poll());
            }

            // 더미 매칭 ID 생성
            Long matchingId = fakeMatchingIdCounter++;
            return chatRoomService.createRoom(matchingId, matchedUsers);
        }

        return null; // 아직 매칭되지 않음
    }
}
