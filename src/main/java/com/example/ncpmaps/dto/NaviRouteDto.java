package com.example.ncpmaps.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class NaviRouteDto {
    private List<PointDto> path;
}
