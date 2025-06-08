package com.example.demo.dto.matching;

import java.time.LocalDateTime;
import com.example.demo.enums.Gender;
import com.example.demo.enums.MatchingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Setter;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchingRequestCreateRequest {
    @NotBlank(message = "선호 지역은 필수입니다.")
    private String preferredRegion;

    @NotNull(message = "선호 날짜는 필수입니다.")
    private LocalDateTime preferredDate;

    @NotNull(message = "선호 시간은 필수입니다.")
    private LocalDateTime preferredTime;

    @NotNull(message = "선호 성별은 필수입니다.")
    private Gender preferredGender;

    @Min(value = 0, message = "최소 연령은 0 이상이어야 합니다.") // '상관없음' 처리 시 0 등
    private int preferredAgeMin;

    @Max(value = 999, message = "최대 연령은 너무 크지 않습니다.") // '상관없음' 처리 시 999 등
    private int preferredAgeMax;

    @NotBlank(message = "선호 음식은 필수입니다.")
    private String preferredFood;

    @NotNull(message = "매칭 타입은 필수입니다.")
    private MatchingType matchingType;

    @Min(value = 2, message = "그룹 인원수는 최소 2명 이상이어야 합니다.") // 1:1 매칭 2명으로 가정
    private int groupSize;

    // DTO에서 벡터 및 가중치 정보는 일반적으로 직접 받지 않고, Service 계층에서 생성/계산
}