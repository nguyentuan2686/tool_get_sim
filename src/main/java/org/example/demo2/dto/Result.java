package org.example.demo2.dto;

import lombok.Getter;

@Getter
public enum Result {
    GOOD("Cát"),
    VERY_GOOD("Đại Cát");

    private final String name;

    Result(String s) {
        name = s;
    }
}
