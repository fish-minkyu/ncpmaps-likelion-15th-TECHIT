package com.example.ncpmaps.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
@Data
@AllArgsConstructor
public class NaviWithPointsDto {
    private PointDto start;
    private PointDto goal;
}
