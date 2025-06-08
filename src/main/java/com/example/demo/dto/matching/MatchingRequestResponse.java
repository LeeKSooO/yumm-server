package com.example.demo.dto.matching;

import java.time.LocalDateTime;
import com.example.demo.enums.Gender;
import com.example.demo.enums.MatchingRequestStatus;
import com.example.demo.enums.MatchingType;
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
public class MatchingRequestResponse {
    private Long requestId;
    private Long userId;
    private String preferredRegion;
    private LocalDateTime preferredDate;
    private LocalDateTime preferredTime;
    private Gender preferredGender;
    private int preferredAgeMin;
    private int preferredAgeMax;
    private String preferredFood;
    private MatchingType matchingType;
    private int groupSize;
    private MatchingRequestStatus status;
    private boolean isActive;
    private LocalDateTime requestTimestamp;
}