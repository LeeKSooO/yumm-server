package com.example.demo.dto;

import java.time.LocalDateTime;
import com.example.demo.constant.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchStatusDto {
    private Long matchingId;
    private RequestStatus status;
    private LocalDateTime createdAt;
    private Long matchedUserIds;
}
