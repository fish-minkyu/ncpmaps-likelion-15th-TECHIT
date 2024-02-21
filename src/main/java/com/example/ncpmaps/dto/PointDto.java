package com.example.ncpmaps.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PointDto {
    private Double lat;
    private Double lng;

    public String toQueryValue() {
        return String.format("%f,%f", lng, lat);
    }
}
