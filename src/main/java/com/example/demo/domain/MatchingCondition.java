package com.example.demo.domain;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

@Entity
public class MatchingCondition {

    @Id @GeneratedValue
    private Long id;

    // 추후 수정
    private String dayOfWeek;   // 요일
    private String timeSlot;    // 시간대 (ex. 점심, 저녁)
    private String region;      // 지역
    private String foodType;    // 선호 음식

    // 매칭과 일대일
    @OneToOne(mappedBy = "condition")
    private Matching matching;
}
