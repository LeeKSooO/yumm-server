package com.example.demo.constant;

public enum Food {
    KOREAN     ("한식"),
    CHINESE    ("중식"),
    JAPANESE   ("일식"),
    WESTERN    ("양식"),
    FASTFOOD   ("패스트푸드"),
    CAFE       ("카페·디저트"),
    MEXICAN    ("멕시칸"),
    INDIAN     ("인도식"),
    THAI       ("태국식"),
    VIETNAMESE ("베트남식");

    private final String description;

    Food(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
