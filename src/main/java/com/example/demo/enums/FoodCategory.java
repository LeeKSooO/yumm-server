package com.example.demo.enums;

public enum FoodCategory {
    KOREAN("한식"),
    CHINESE("중식"),
    JAPANESE("일식"),
    WESTERN("양식"),
    ASIAN("아시안"),
    FUSION("퓨전"),
    FAST_FOOD("패스트푸드"),
    CAFE("카페/디저트"),
    SNACK("분식"),
    BBQ("고기/구이"),
    SEAFOOD("해산물"),
    VEGETARIAN("채식"),
    ANY("상관없음");

    private final String displayName;

    FoodCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
} 