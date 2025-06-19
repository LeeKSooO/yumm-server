package com.example.demo.service.impl;

import com.example.demo.service.MatchingService;
import com.example.demo.service.FCMService;
import com.example.demo.service.MatchingRedisService;
import com.example.demo.domain.MatchingRequest;
import com.example.demo.domain.MatchingResult;
import com.example.demo.domain.ChatRoom;
import com.example.demo.domain.User;
import com.example.demo.dto.matching.MatchingRequestCreateRequest;
import com.example.demo.dto.matching.MatchingResultResponse;
import com.example.demo.dto.users.ProfileResponse;
import com.example.demo.enums.Gender;
import com.example.demo.enums.MatchingRequestStatus;
import com.example.demo.enums.MatchingType;
import com.example.demo.enums.MatchingResultStatus;
import com.example.demo.exception.CustomException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.MatchingRequestRepository;
import com.example.demo.repository.MatchingResultRepository;
import com.example.demo.repository.ChatRoomRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.util.MatchingVectorUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingServiceImpl implements MatchingService {

    private final MatchingRequestRepository matchingRequestRepository;
    private final MatchingResultRepository matchingResultRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final MatchingRedisService matchingRedisService;
    private final MatchingVectorUtils matchingVectorUtils;
    private final SimpMessagingTemplate messagingTemplate;
    private final FCMService fcmService;

    /**
     * 실시간 매칭 요청을 처리합니다.
     */
    @Override
    @Transactional
    public void requestInstantMatching(Long userId, MatchingRequestCreateRequest request) {
        // 1. 사용자 조회
        User user = findUserByIdOrThrow(userId);

        // 2. 매칭 요청 생성 및 저장
        MatchingRequest matchingRequest = createMatchingRequest(user, request);
        matchingRequest = matchingRequestRepository.save(matchingRequest);

        // 3. Redis에 실시간 매칭 요청 저장
        matchingRedisService.saveInstantMatchingRequest(matchingRequest);

        // 4. 즉시 매칭 프로세스 시작
        processInstantMatching(matchingRequest);
    }

    /**
     * 실시간 매칭 요청을 취소합니다.
     */
    @Override
    @Transactional
    public void cancelInstantMatching(Long userId, Long requestId) {
        MatchingRequest request = findMatchingRequestOrThrow(requestId);
        
        // 요청자 본인인지 확인
        if (!request.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        // Redis에서 매칭 요청 제거
        matchingRedisService.removeInstantMatchingRequest(requestId, request.getMatchingType());

        // 상태 업데이트 및 저장
        request.updateStatus(MatchingRequestStatus.CANCELED);
        matchingRequestRepository.save(request);
    }

    /**
     * 매칭 등록제에 매칭을 등록합니다.
     */
    @Override
    @Transactional
    public void registerScheduledMatching(Long userId, MatchingRequestCreateRequest request) {
        // 1. 사용자 조회
        User user = findUserByIdOrThrow(userId);

        // 2. 매칭 요청 생성 및 저장
        MatchingRequest matchingRequest = createMatchingRequest(user, request);
        matchingRequest = matchingRequestRepository.save(matchingRequest);

        // 3. Redis에 예약 매칭 요청 저장
        matchingRedisService.saveScheduledMatchingRequest(matchingRequest);
    }

    /**
     * 등록된 매칭 요청을 취소합니다.
     */
    @Override
    @Transactional
    public void cancelScheduledMatching(Long userId, Long requestId) {
        MatchingRequest request = findMatchingRequestOrThrow(requestId);
        
        // 요청자 본인인지 확인
        if (!request.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        // Redis에서 매칭 요청 제거
        matchingRedisService.removeScheduledMatchingRequest(requestId, request.getMatchingType());

        // 상태 업데이트 및 저장
        request.updateStatus(MatchingRequestStatus.CANCELED);
        matchingRequestRepository.save(request);
    }

    /**
     * 매칭 결과를 조회합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public MatchingResultResponse getMatchingResult(Long requestId) {
        // 1. request_id를 통해 MatchingResult 조회
        //    MatchingResult 엔티티는 MatchingRequest 엔티티와 1:1 관계를 가지고 있으므로 
        //    requestId를 통해 조회 가능
        MatchingResult result = matchingResultRepository.findByMatchingRequestId(requestId)
                .orElseThrow(() -> new CustomException(ErrorCode.MATCHING_RESULT_NOT_FOUND));

        // 2. 매칭 결과 상태 확인
        //    MATCHED 상태가 아니라면 매칭된 사용자 목록을 제공할 필요 X
        //    result의 status가 MATCHED 상태인지 확인
        Long chatRoomId = null;
        if (result.getStatus().equals(MatchingResultStatus.MATCHED.name())) {
            // 3. 매칭 성공 시(MATCHED) MatchingResult와 연결된 ChatRoom 조회
            ChatRoom chatRoom = chatRoomRepository.findByMatchingResult(result)
                    .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));
            chatRoomId = chatRoom.getId();
        }

        // 4. ChatRoom에 참여한 현재 사용자들 조회 ( 아래 내용 꼭 추후 재확인 )
        //    근데 ChatRoom에 참여한 현재 사용자들 조회 로직이 혹시 몰라서 추가는 했으나,
        //    추후 필요 없을 시 삭제 필요.
        //    그렇게 되면 '3번'의 매칭 성공 시 채팅룸 조회도 지워야함.
        //    이유는 매칭 결과 조회 시 채팅방에 참여한 유저를 조회하려면 DB조회가 많이 필요한데,
        //    해당 로직이 UX에 쓰이려면 유저가 채팅방에 입장하기 전, 매칭 결과를 통해 유저들의 프로필을
        //    확인하는 방향으로 가야하는데, 일단은 현재 방향은, 매칭이 잡히면 프로필 확인 없이 바로
        //    채팅방으로 연결되는 방향이기 때문에, 매칭 유저 확인은, ChatRoomService 비즈니스 로직에 들어가야 맞음.
        //List<ProfileResponse> matchedUsers = new ArrayList<>();
        //if (result.getMatchingRequest().getMatchingType() == MatchingType.ONE_TO_ONE) {
            // 1:1 매칭의 경우
        //    User matchedUser = findMatchedUser(result);
        //    matchedUsers.add(ProfileResponse.from(matchedUser));
        //} else {
            // 그룹 매칭의 경우
        //    matchedUsers = findMatchedGroupUsers(result);
        //}

        // 4. 매칭된 사용자 목록 비우고, 매칭 결과 상태와 채팅방 ID만 반환.
        return MatchingResultResponse.builder()
                .matchId(result.getId())
                .requestId(requestId)
                .status(result.getStatus())
                .matchingTimestamp(result.getMatchingTimestamp())
                .chatRoomId(chatRoomId)
                .build();
    }

    /**
     * 실시간 매칭 처리를 수행합니다.
     * 매칭 요청이 들어올 때마다 호출됩니다.
     */
    private void processInstantMatching(MatchingRequest newRequest) {
        if (newRequest.getMatchingType() == MatchingType.ONE_TO_ONE) {
            processInstantOneToOneMatching(newRequest);
        } else {
            processInstantGroupMatching(newRequest);
        }
    }

    /**
     * 1:1 실시간 매칭을 처리합니다.
     * 새로운 매칭이 들어왔을 때, Redis에 저장된 기존 매칭 요청들 중에 적합한 상대 찾음.
     * 1. 성별 조건으로 필터링된 매칭 후보 목록 조회
     * 2. 거리 기반 필터링 (5km 이내)
     * 3. 벡터 유사도 계산 및 정렬
     * 4. 가장 유사도가 높은 상대와 매칭
     * 5. 매칭 성공 처리 
     */
    private void processInstantOneToOneMatching(MatchingRequest newRequest) {
        // 1. 성별 조건으로 필터링된 매칭 후보 목록 조회
        List<MatchingRequest> candidates = matchingRedisService.getInstantMatchingRequests(MatchingType.ONE_TO_ONE)
                .stream()
                // 성별 조건으로 필터링 
                .filter(candidate -> isGenderMatched(newRequest, candidate))
                // 자신의 매칭 요청은 제외
                .filter(candidate -> !candidate.getId().equals(newRequest.getId()))
                // 매칭 요청 목록 반환(필터링된 MatchingRequest 객체들을 List 형태로 반환)
                .collect(Collectors.toList());

        // 2. 거리 기반 필터링 (5km 이내)
        candidates = candidates.stream()
                .filter(candidate -> isWithinDistance(newRequest, candidate, 5.0))
                .collect(Collectors.toList());

        // 3. 매칭 후보가 없다면 매칭 실패(성별, 거리, 자기 자신 제외 조건을 만족하는 후보가 없는 경우)
        if (candidates.isEmpty()) {
            return; // 매칭 실패
        }

        // 4. 벡터 유사도 계산 및 정렬
        Map<MatchingRequest, Double> similarities = new HashMap<>();
        double[] newRequestVector = matchingRedisService.getMatchingVector(newRequest.getId());

        for (MatchingRequest candidate : candidates) {
            double[] candidateVector = matchingRedisService.getMatchingVector(candidate.getId());
            double similarity = matchingVectorUtils.calculateWeightedSimilarity(
                    newRequestVector, candidateVector, null);
            similarities.put(candidate, similarity);
        }

        // 5. 가장 유사도가 높은 상대와 매칭
        Optional<MatchingRequest> bestMatch = similarities.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);

        if (bestMatch.isPresent()) {
            // 매칭 성공 처리
            createMatchingResult(newRequest, List.of(bestMatch.get()));
        }
    }

    /**
     * 그룹 실시간 매칭을 처리합니다.
     */
    private void processInstantGroupMatching(MatchingRequest newRequest) {
        // 1. 성별 조건으로 필터링된 매칭 후보 목록 조회
        List<MatchingRequest> candidates = matchingRedisService.getInstantMatchingRequests(MatchingType.GROUP)
                .stream()
                .filter(candidate -> isGenderMatched(newRequest, candidate))
                .filter(candidate -> !candidate.getId().equals(newRequest.getId()))
                .collect(Collectors.toList());

        // 2. 거리 기반 필터링 (5km 이내)
        candidates = candidates.stream()
                .filter(candidate -> isWithinDistance(newRequest, candidate, 5.0))
                .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            return; // 매칭 실패
        }

        // 3. 벡터 유사도 계산
        double[] newRequestVector = matchingRedisService.getMatchingVector(newRequest.getId());
        Map<MatchingRequest, Double> similarities = new HashMap<>();

        for (MatchingRequest candidate : candidates) {
            double[] candidateVector = matchingRedisService.getMatchingVector(candidate.getId());
            double similarity = matchingVectorUtils.calculateWeightedSimilarity(
                    newRequestVector, candidateVector, null);
            similarities.put(candidate, similarity);
        }

        // 4. 그룹 크기에 맞게 가장 유사도가 높은 멤버들 선택
        List<MatchingRequest> groupMembers = new ArrayList<>();
        groupMembers.add(newRequest);

        // 휴리스틱: 이미 3명인 그룹을 우선적으로 4명으로 만들기
        if (candidates.size() >= newRequest.getGroupSize() - 1) {
            List<MatchingRequest> selectedMembers = similarities.entrySet().stream()
                    .sorted(Map.Entry.<MatchingRequest, Double>comparingByValue().reversed())
                    .limit(newRequest.getGroupSize() - 1)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            
            groupMembers.addAll(selectedMembers);
            
            // 매칭 성공 처리
            createMatchingResult(newRequest, groupMembers);
        }
    }

    /**
     * 매칭 등록제 요청을 처리합니다.
     * 매 시간 정각에 실행됩니다.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void processScheduledMatching() {
        // 1:1 매칭과 그룹 매칭 각각 처리
        processScheduledOneToOneMatching();
        processScheduledGroupMatching();
    }

    /**
     * 1:1 매칭 등록제 요청을 처리합니다.
     */
    private void processScheduledOneToOneMatching() {
        List<MatchingRequest> requests = matchingRedisService.getScheduledMatchingRequests(MatchingType.ONE_TO_ONE);
        
        // 1. 성별 선호도로 초기 그룹핑
        Map<String, List<MatchingRequest>> genderGroups = requests.stream()
                .collect(Collectors.groupingBy(request -> request.getPreferredGender().name()));

        // 2. 각 성별 그룹 내에서 매칭
        for (List<MatchingRequest> genderGroup : genderGroups.values()) {
            while (genderGroup.size() >= 2) {
                MatchingRequest request = genderGroup.get(0);
                
                // 벡터 유사도 계산
                double[] requestVector = matchingRedisService.getMatchingVector(request.getId());
                Map<MatchingRequest, Double> similarities = new HashMap<>();

                for (int i = 1; i < genderGroup.size(); i++) {
                    MatchingRequest candidate = genderGroup.get(i);
                    if (isGenderMatched(request, candidate)) {
                        double[] candidateVector = matchingRedisService.getMatchingVector(candidate.getId());
                        double similarity = matchingVectorUtils.calculateWeightedSimilarity(
                                requestVector, candidateVector, null);
                        similarities.put(candidate, similarity);
                    }
                }

                if (!similarities.isEmpty()) {
                    // 가장 유사도가 높은 상대와 매칭
                    MatchingRequest bestMatch = similarities.entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey)
                            .orElse(null);

                    if (bestMatch != null) {
                        createMatchingResult(request, List.of(bestMatch));
                        genderGroup.remove(request);
                        genderGroup.remove(bestMatch);
                        continue;
                    }
                }
                
                // 매칭 실패 처리
                request.updateStatus(MatchingRequestStatus.FAILED);
                matchingRequestRepository.save(request);
                genderGroup.remove(request);
            }
        }
    }

    /**
     * 그룹 매칭 등록제 요청을 처리합니다.
     */
    private void processScheduledGroupMatching() {
        List<MatchingRequest> requests = matchingRedisService.getScheduledMatchingRequests(MatchingType.GROUP);
        
        // 1. 지역 기반으로 초기 그룹핑
        Map<String, List<MatchingRequest>> regionGroups = requests.stream()
                .collect(Collectors.groupingBy(MatchingRequest::getPreferredRegion));

        // 2. 각 지역 그룹 내에서 매칭
        for (List<MatchingRequest> regionGroup : regionGroups.values()) {
            while (regionGroup.size() >= 3) {  // 최소 그룹 크기가 3
                MatchingRequest request = regionGroup.get(0);
                
                // 벡터 유사도 계산
                double[] requestVector = matchingRedisService.getMatchingVector(request.getId());
                Map<MatchingRequest, Double> similarities = new HashMap<>();

                for (int i = 1; i < regionGroup.size(); i++) {
                    MatchingRequest candidate = regionGroup.get(i);
                    if (isGenderMatched(request, candidate)) {
                        double[] candidateVector = matchingRedisService.getMatchingVector(candidate.getId());
                        double similarity = matchingVectorUtils.calculateWeightedSimilarity(
                                requestVector, candidateVector, null);
                        similarities.put(candidate, similarity);
                    }
                }

                // 그룹 크기에 맞는 가장 유사도가 높은 멤버들 선택
                if (similarities.size() >= request.getGroupSize() - 1) {
                    List<MatchingRequest> selectedMembers = similarities.entrySet().stream()
                            .sorted(Map.Entry.<MatchingRequest, Double>comparingByValue().reversed())
                            .limit(request.getGroupSize() - 1)
                            .map(Map.Entry::getKey)
                            .collect(Collectors.toList());

                    List<MatchingRequest> groupMembers = new ArrayList<>();
                    groupMembers.add(request);
                    groupMembers.addAll(selectedMembers);

                    createMatchingResult(request, groupMembers);
                    regionGroup.removeAll(groupMembers);
                    continue;
                }
                
                // 매칭 실패 처리
                request.updateStatus(MatchingRequestStatus.FAILED);
                matchingRequestRepository.save(request);
                regionGroup.remove(request);
            }
        }
    }


    /**
     * 만료된 매칭 요청을 처리합니다.
     * 매 분마다 실행됩니다.
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void expireOldRequests() {
        LocalDateTime now = LocalDateTime.now();

        // 실시간 매칭 요청 만료 처리 (요청 시각 기준 1분 경과)
        expireInstantRequests(now);

        // 예약 매칭 요청 만료 처리 (선호 시간 기준 2시간 경과)
        expireScheduledRequests(now);
    }

    /**
     * 실시간 매칭 요청 중 만료된 요청들을 찾아 처리합니다.
     * @param now 현재 시간
     */
    private void expireInstantRequests(LocalDateTime now) {
        LocalDateTime threshold = now.minusMinutes(1); // 1분 이상 지난 요청만 조회

        // 1:1 매칭 중 만료된 요청
        List<MatchingRequest> expiredOneToOne = matchingRequestRepository.findExpiredRequests(
                MatchingRequestStatus.PENDING, MatchingType.ONE_TO_ONE, threshold);

        // 그룹 매칭 중 만료된 요청
        List<MatchingRequest> expiredGroup = matchingRequestRepository.findExpiredRequests(
                MatchingRequestStatus.PENDING, MatchingType.GROUP, threshold);

        // 두 리스트 병합
        List<MatchingRequest> expiredRequests = new ArrayList<>();
        expiredRequests.addAll(expiredOneToOne);
        expiredRequests.addAll(expiredGroup);

        // 만료 처리
        for (MatchingRequest request : expiredRequests) {
            processExpiration(
                    request,
                    () -> matchingRedisService.removeInstantMatchingRequest(request.getId(), request.getMatchingType())
            );
        }
    }

    /**
     * 예약 매칭 요청 중 선호 시간이 2시간 이상 지난 요청들을 찾아 처리합니다.
     * @param now 현재 시간
     */
    private void expireScheduledRequests(LocalDateTime now) {
        LocalDateTime threshold = now.minusHours(2); // 선호 시간 기준 2시간 이전

        // JPQL 쿼리로 선호 시간 + 상태 조건을 만족하는 요청만 조회
        List<MatchingRequest> expiredScheduledRequests = matchingRequestRepository.findExpiredScheduledRequests(
                MatchingRequestStatus.PENDING, threshold);

        // 만료 처리
        for (MatchingRequest request : expiredScheduledRequests) {
            processExpiration(
                    request,
                    () -> matchingRedisService.removeScheduledMatchingRequest(request.getId(), request.getMatchingType())
            );
        }
    }

    /**
     * 매칭 요청을 만료 상태로 전환하고, DB 업데이트 및 Redis 제거, 알림 전송까지 처리합니다.
     * @param request 만료시킬 매칭 요청 객체
     * @param redisRemoval Redis 키 제거 로직 (lambda 전달)
     */
    private void processExpiration(MatchingRequest request, Runnable redisRemoval) {
        request.updateStatus(MatchingRequestStatus.EXPIRED);         // 상태 변경
        matchingRequestRepository.save(request);                     // DB 반영
        redisRemoval.run();                                          // Redis에서 제거
        notifyMatchingExpired(request);                              // 알림 전송
    }


    /**
     * 매칭 결과를 생성하고 처리합니다.
     */
    private void createMatchingResult(MatchingRequest request, List<MatchingRequest> matchedRequests) {
        // 1. 매칭 결과 생성
        MatchingResult result = MatchingResult.builder()
                .matchingRequest(request)
                .status("SUCCESS")
                .matchingTimestamp(LocalDateTime.now())
                .build();
        result = matchingResultRepository.save(result);

        // 2. 채팅방 생성
        ChatRoom chatRoom = ChatRoom.builder()
                .matchingResult(result)
                .createdAt(LocalDateTime.now())
                .isActive(true)
                .groupSize(matchedRequests.size())
                .build();
        chatRoomRepository.save(chatRoom);

        // 3. 매칭된 요청들의 상태 업데이트
        for (MatchingRequest matchedRequest : matchedRequests) {
            matchedRequest.updateStatus(MatchingRequestStatus.MATCHED);
            matchingRequestRepository.save(matchedRequest);
            
            // Redis에서 제거
            if (matchedRequest.getMatchingType() == MatchingType.ONE_TO_ONE) {
                matchingRedisService.removeInstantMatchingRequest(matchedRequest.getId(), matchedRequest.getMatchingType());
            } else {
                matchingRedisService.removeScheduledMatchingRequest(matchedRequest.getId(), matchedRequest.getMatchingType());
            }
            
            // 매칭 성공 알림 전송
            notifyMatchingSuccess(matchedRequest, chatRoom.getId());
        }
    }

    /**
     * 매칭 성공 알림을 전송합니다.
     */
    private void notifyMatchingSuccess(MatchingRequest request, Long chatRoomId) {
        // WebSocket을 통한 실시간 알림
        String destination = "/queue/matching/" + request.getUser().getId();
        Map<String, Object> message = new HashMap<>();
        message.put("type", "MATCHING_SUCCESS");
        message.put("chatRoomId", chatRoomId);
        messagingTemplate.convertAndSend(destination, message);

        // FCM을 통한 푸시 알림 전송
        fcmService.sendMatchNotification(
            request.getUser().getId(),
            "매칭 성공!",
            "새로운 매칭이 성사되었습니다. 채팅방으로 이동해보세요."
        );
    }

    /**
     * 매칭 만료 알림을 전송합니다.
     */
    private void notifyMatchingExpired(MatchingRequest request) {
        // WebSocket을 통한 실시간 알림
        String destination = "/queue/matching/" + request.getUser().getId();
        Map<String, Object> message = new HashMap<>();
        message.put("type", "MATCHING_EXPIRED");
        message.put("requestId", request.getId());
        messagingTemplate.convertAndSend(destination, message);

        // FCM을 통한 푸시 알림 전송
        fcmService.sendMatchNotification(
            request.getUser().getId(),
            "매칭 만료",
            "매칭 요청이 만료되었습니다. 새로운 매칭을 시도해보세요."
        );
    }

    /**
     * 두 매칭 요청의 성별 선호도가 일치하는지 확인합니다.
     */
    private boolean isGenderMatched(MatchingRequest request1, MatchingRequest request2) {
        return (request1.getPreferredGender() == request2.getUser().getGender() ||
                request1.getPreferredGender() == Gender.ANY) &&
               (request2.getPreferredGender() == request1.getUser().getGender() ||
                request2.getPreferredGender() == Gender.ANY);
    }

    /**
     * 두 지역 간의 거리가 지정된 거리 이내인지 확인합니다.
     */
    private boolean isWithinDistance(MatchingRequest request1, MatchingRequest request2, double maxDistanceKm) {
        double[] coords1 = getCoordinatesFromRegion(request1.getPreferredRegion());
        double[] coords2 = getCoordinatesFromRegion(request2.getPreferredRegion());
        
        return calculateDistance(coords1[0], coords1[1], coords2[0], coords2[1]) <= maxDistanceKm;
    }

    /**
     * 두 지점 간의 거리를 계산합니다. (Haversine 공식)
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 지구의 반지름 (km)

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }

    /**
     * 지역명으로부터 좌표를 가져옵니다.
     */
    private double[] getCoordinatesFromRegion(String region) {
        // TODO: 실제 구현에서는 DB나 외부 API를 통해 지역의 중심 좌표를 가져와야 함
        // 임시로 서울 중심 좌표 반환
        return new double[]{37.5665, 126.9780};
    }

    /**
     * 1:1 매칭에서 매칭된 상대방을 찾습니다.
     */
    private User findMatchedUser(MatchingResult result) {
        // TODO: 실제 구현에서는 매칭 결과와 연결된 채팅방 참가자 중에서 요청자가 아닌 사용자를 찾아야 함
        return result.getMatchingRequest().getUser();
    }

    /**
     * 그룹 매칭에서 매칭된 멤버들을 찾습니다.
     */
    private List<ProfileResponse> findMatchedGroupUsers(MatchingResult result) {
        // TODO: 실제 구현에서는 매칭 결과와 연결된 채팅방 참가자들의 프로필을 반환해야 함
        return List.of(ProfileResponse.from(result.getMatchingRequest().getUser()));
    }

    /**
     * 매칭 요청을 생성합니다.
     */
    private MatchingRequest createMatchingRequest(User user, MatchingRequestCreateRequest request) {
        return MatchingRequest.builder()
                .user(user)
                .preferredRegion(request.getPreferredRegion())
                .preferredDate(request.getPreferredDate())
                .preferredTime(request.getPreferredTime())
                .preferredGender(request.getPreferredGender())
                .preferredAgeMin(request.getPreferredAgeMin())
                .preferredAgeMax(request.getPreferredAgeMax())
                .preferredFood(request.getPreferredFood())
                .matchingType(request.getMatchingType())
                .groupSize(request.getGroupSize())
                .status(MatchingRequestStatus.PENDING)
                .isActive(true)
                .requestTimestamp(LocalDateTime.now())
                .build();
    }

    private User findUserByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private MatchingRequest findMatchingRequestOrThrow(Long requestId) {
        return matchingRequestRepository.findById(requestId)
                .orElseThrow(() -> new CustomException(ErrorCode.MATCHING_NOT_FOUND));
    }
}
