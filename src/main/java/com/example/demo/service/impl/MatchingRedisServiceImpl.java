package com.example.demo.service.impl;

import com.example.demo.domain.MatchingRequest;
import com.example.demo.enums.MatchingType;
import com.example.demo.service.MatchingRedisService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingRedisServiceImpl implements MatchingRedisService {

    private final RedisTemplate<String, Object> matchingRedisTemplate;
    private final ObjectMapper objectMapper;

    // Redis Key Prefix
    private static final String INSTANT_MATCHING_KEY = "instant:matching:";
    private static final String SCHEDULED_MATCHING_KEY = "scheduled:matching:";
    private static final String MATCHING_VECTOR_KEY = "matching:vector:";

    @Override
    public void saveInstantMatchingRequest(MatchingRequest request) {
        try {
            String key = INSTANT_MATCHING_KEY + request.getMatchingType().name().toLowerCase();
            String value = objectMapper.writeValueAsString(request);
            String memberKey = String.valueOf(request.getId());

            matchingRedisTemplate.opsForHash().put(key, memberKey, value);
            
            // 실시간 매칭 요청은 1분 후 만료
            // Hash 자료구조를 사용할 경우, TTL이 '매칭 큐 전체의 생존 시간'이 된다.
            // 즉 새로운 요청이 들어올 때마다, 해당 Key에 속한 모든 필드들의 TTL이 1분으로 재설정되기 때문에,
            // 예를 들어 요청1이 10분전에 들어왔어도, TTL = 1분이 지나기 전 계속해서 새로운 요청이 추가될 경우,
            // 요청1은 실질적으로 계속 살아있게 된다. 
            // 따라서 이 부분에 대해 요청 만료 여부를 TTL로 판단하는 것이 아니라, "requestedAt" 등의
            // 필드를 기준으로 만료 여부를 검토해야 한다.
            // 현재 설계상, 매칭 타입이 4개이기 때문에 hash 자료구조를 이용하여 탐색 성능을 높이고자,
            // hash+만료 시간 검증 으로 매칭 큐를 관리하려고 함.
            matchingRedisTemplate.expire(key, 1, TimeUnit.MINUTES);

            log.info("Saved instant matching request: {}", request.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to save instant matching request: {}", request.getId(), e);
            throw new RuntimeException("Failed to save instant matching request", e);
        }
    }

    @Override
    public void removeInstantMatchingRequest(Long requestId, MatchingType matchingType) {
        String key = INSTANT_MATCHING_KEY + matchingType.name().toLowerCase();
        String memberKey = String.valueOf(requestId);

        matchingRedisTemplate.opsForHash().delete(key, memberKey);
        log.info("Removed instant matching request: {}", requestId);
    }

    @Override
    public void saveScheduledMatchingRequest(MatchingRequest request) {
        try {
            String key = SCHEDULED_MATCHING_KEY + request.getMatchingType().name().toLowerCase();
            String value = objectMapper.writeValueAsString(request);
            String memberKey = String.valueOf(request.getId());

            matchingRedisTemplate.opsForHash().put(key, memberKey, value);
            // 예약 매칭 요청은 24시간 후 만료 (예시)
            matchingRedisTemplate.expire(key, 24, TimeUnit.HOURS);

            log.info("Saved scheduled matching request: {}", request.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to save scheduled matching request: {}", request.getId(), e);
            throw new RuntimeException("Failed to save scheduled matching request", e);
        }
    }

    @Override
    public void removeScheduledMatchingRequest(Long requestId, MatchingType matchingType) {
        String key = SCHEDULED_MATCHING_KEY + matchingType.name().toLowerCase();
        String memberKey = String.valueOf(requestId);

        matchingRedisTemplate.opsForHash().delete(key, memberKey);
        log.info("Removed scheduled matching request: {}", requestId);
    }

    @Override
    public List<MatchingRequest> getInstantMatchingRequests(MatchingType type) {
        String key = INSTANT_MATCHING_KEY + type.name().toLowerCase();
        List<Object> values = matchingRedisTemplate.opsForHash().values(key);
        List<MatchingRequest> result = new ArrayList<>();
        for (Object obj : values) {
            if (obj instanceof String) {
                try {
                    MatchingRequest req = objectMapper.readValue((String) obj, MatchingRequest.class);
                    result.add(req);
                } catch (Exception e) {
                    log.error("Failed to deserialize instant matching request", e);
                }
            }
        }
        return result;
    }

    @Override
    public List<MatchingRequest> getScheduledMatchingRequests(MatchingType type) {
        String key = SCHEDULED_MATCHING_KEY + type.name().toLowerCase();
        List<Object> values = matchingRedisTemplate.opsForHash().values(key);
        List<MatchingRequest> result = new ArrayList<>();
        for (Object obj : values) {
            if (obj instanceof String) {
                try {
                    MatchingRequest req = objectMapper.readValue((String) obj, MatchingRequest.class);
                    result.add(req);
                } catch (Exception e) {
                    log.error("Failed to deserialize scheduled matching request", e);
                }
            }
        }
        return result;
    }

    @Override
    public double[] getMatchingVector(Long requestId) {
        String key = MATCHING_VECTOR_KEY + requestId;
        Object value = matchingRedisTemplate.opsForValue().get(key);
        if (value instanceof String) {
            try {
                return objectMapper.readValue((String) value, double[].class);
            } catch (Exception e) {
                log.error("Failed to deserialize matching vector for request {}", requestId, e);
            }
        }
        return new double[0];
    }
} 