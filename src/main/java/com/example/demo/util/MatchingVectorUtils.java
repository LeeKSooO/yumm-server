package com.example.demo.util;

import com.example.demo.domain.MatchingRequest;
import com.example.demo.enums.FoodCategory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Component
public class MatchingVectorUtils {
    private static final int MIN_AGE = 0;
    private static final int MAX_AGE = 100;
    private static final double MIN_LATITUDE = 33.0;  // 한국 최남단
    private static final double MAX_LATITUDE = 38.0;  // 한국 최북단
    private static final double MIN_LONGITUDE = 125.0;  // 한국 최서단
    private static final double MAX_LONGITUDE = 132.0;  // 한국 최동단
    
    private final ObjectMapper objectMapper;

    public MatchingVectorUtils(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 매칭 요청을 벡터로 변환합니다.
     */
    public double[] convertToVector(MatchingRequest request) {
        List<Double> vector = new ArrayList<>();

        // 1. 나이 벡터 (2차원)
        if (request.getPreferredAgeMin() == 0 && request.getPreferredAgeMax() == 999) {
            // 상관없음의 경우 -1로 설정하여 유사도 계산에서 제외
            vector.add(-1.0);
            vector.add(-1.0);
        } else {
            vector.add(normalizeAge(request.getPreferredAgeMin()));
            vector.add(normalizeAge(request.getPreferredAgeMax()));
        }

        // 2. 지역 벡터 (2차원) - 위도, 경도
        double[] coordinates = getCoordinatesFromRegion(request.getPreferredRegion());
        vector.add(normalizeLatitude(coordinates[0]));
        vector.add(normalizeLongitude(coordinates[1]));

        // 3. 음식 카테고리 벡터 (N차원)
        double[] foodVector = convertFoodToVector(request.getPreferredFood());
        Arrays.stream(foodVector).forEach(vector::add);

        // 4. 요일 벡터 (7차원)
        double[] dayVector = convertDayToVector(request.getPreferredDate());
        Arrays.stream(dayVector).forEach(vector::add);

        // 5. 시간 벡터 (2차원)
        double[] timeVector = convertTimeToVector(request.getPreferredTime());
        Arrays.stream(timeVector).forEach(vector::add);

        // 6. 날짜 간격 벡터 (1차원)
        vector.add(normalizeDateDifference(request.getPreferredDate()));

        return vector.stream().mapToDouble(Double::doubleValue).toArray();
    }

    /**
     * 두 벡터 간의 코사인 유사도를 계산합니다.
     */
    public double calculateCosineSimilarity(double[] vectorA, double[] vectorB) {
        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException("Vectors must be of same length");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            // -1은 '상관없음'을 나타내므로 유사도 계산에서 제외
            if (vectorA[i] == -1 || vectorB[i] == -1) {
                continue;
            }
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }

        if (normA == 0 || normB == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 가중치를 적용한 유사도를 계산합니다.
     */
    public double calculateWeightedSimilarity(double[] vectorA, double[] vectorB, Map<String, Double> weights) {
        // 기본 가중치 설정
        Map<String, Double> defaultWeights = new HashMap<>();
        defaultWeights.put("age", 0.25);
        defaultWeights.put("region", 0.25);
        defaultWeights.put("food", 0.15);
        defaultWeights.put("day", 0.15);
        defaultWeights.put("time", 0.15);
        defaultWeights.put("date", 0.05);

        // 사용자 정의 가중치가 있으면 적용
        if (weights != null) {
            defaultWeights.putAll(weights);
        }

        // 각 특성별 시작 인덱스
        int ageStart = 0;           // 2차원
        int regionStart = 2;        // 2차원
        int foodStart = 4;          // FoodCategory.values().length 차원
        int dayStart = 4 + FoodCategory.values().length;  // 7차원
        int timeStart = dayStart + 7;  // 2차원
        int dateStart = timeStart + 2;  // 1차원

        double similarity = 0.0;
        
        // 나이 유사도
        similarity += calculatePartialCosineSimilarity(vectorA, vectorB, ageStart, 2) * defaultWeights.get("age");
        
        // 지역 유사도
        similarity += calculatePartialCosineSimilarity(vectorA, vectorB, regionStart, 2) * defaultWeights.get("region");
        
        // 음식 유사도
        similarity += calculatePartialCosineSimilarity(vectorA, vectorB, foodStart, FoodCategory.values().length) * defaultWeights.get("food");
        
        // 요일 유사도
        similarity += calculatePartialCosineSimilarity(vectorA, vectorB, dayStart, 7) * defaultWeights.get("day");
        
        // 시간 유사도
        similarity += calculatePartialCosineSimilarity(vectorA, vectorB, timeStart, 2) * defaultWeights.get("time");
        
        // 날짜 간격 유사도
        similarity += calculatePartialCosineSimilarity(vectorA, vectorB, dateStart, 1) * defaultWeights.get("date");

        return similarity;
    }

    private double calculatePartialCosineSimilarity(double[] vectorA, double[] vectorB, int start, int length) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = start; i < start + length; i++) {
            if (vectorA[i] == -1 || vectorB[i] == -1) {
                continue;
            }
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }

        if (normA == 0 || normB == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private double normalizeAge(int age) {
        return (double) (age - MIN_AGE) / (MAX_AGE - MIN_AGE);
    }

    private double normalizeLatitude(double latitude) {
        return (latitude - MIN_LATITUDE) / (MAX_LATITUDE - MIN_LATITUDE);
    }

    private double normalizeLongitude(double longitude) {
        return (longitude - MIN_LONGITUDE) / (MAX_LONGITUDE - MIN_LONGITUDE);
    }

    private double[] getCoordinatesFromRegion(String region) {
        // TODO: 실제 구현에서는 DB나 외부 API를 통해 지역의 중심 좌표를 가져와야 함
        // 임시로 서울 중심 좌표 반환
        return new double[]{37.5665, 126.9780};
    }

    private double[] convertFoodToVector(String food) {
        double[] vector = new double[FoodCategory.values().length];
        Arrays.fill(vector, 0);
        
        if (food.equals("상관없음")) {
            Arrays.fill(vector, -1);  // 상관없음은 -1로 표시
            return vector;
        }

        // 해당하는 카테고리만 1로 설정 (원-핫 인코딩)
        for (int i = 0; i < FoodCategory.values().length; i++) {
            if (FoodCategory.values()[i].getDisplayName().equals(food)) {
                vector[i] = 1;
                break;
            }
        }
        return vector;
    }

    private double[] convertDayToVector(LocalDateTime dateTime) {
        double[] vector = new double[7];  // 월~일
        Arrays.fill(vector, 0);
        
        DayOfWeek dayOfWeek = dateTime.getDayOfWeek();
        vector[dayOfWeek.getValue() - 1] = 1;  // 월요일이 1부터 시작
        
        return vector;
    }

    private double[] convertTimeToVector(LocalDateTime dateTime) {
        int hour = dateTime.getHour();
        double normalizedHour = (double) hour / 24.0 * 2.0 * Math.PI;
        
        return new double[]{
            Math.sin(normalizedHour),
            Math.cos(normalizedHour)
        };
    }

    private double normalizeDateDifference(LocalDateTime dateTime) {
        long daysDifference = ChronoUnit.DAYS.between(LocalDateTime.now(), dateTime);
        // 최대 30일까지 정규화
        return Math.min(1.0, (double) daysDifference / 30.0);
    }

    public String vectorToJson(double[] vector) throws JsonProcessingException {
        return objectMapper.writeValueAsString(vector);
    }

    public double[] jsonToVector(String json) throws JsonProcessingException {
        return objectMapper.readValue(json, double[].class);
    }
} 