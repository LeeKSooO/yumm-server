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