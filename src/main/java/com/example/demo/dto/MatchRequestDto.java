package com.example.demo.dto;

import com.example.demo.constant.Food;
import com.example.demo.constant.Region;

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
public class MatchRequestDto {

    private int groupSize; // 인원 수
    private Region region; // 선호 지역
    private Food food; // 선호 음식
}