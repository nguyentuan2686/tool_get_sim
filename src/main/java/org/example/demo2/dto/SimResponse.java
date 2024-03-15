package org.example.demo2.dto;

import lombok.Data;

import java.util.List;

@Data
public class SimResponse {
    private String errorCode;
    private String message;
    private List<SimData> data;
}
