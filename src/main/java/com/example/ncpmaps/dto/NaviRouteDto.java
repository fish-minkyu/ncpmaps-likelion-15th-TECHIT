package com.example.ncpmaps.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
// 경로를 나타내기 위한 Dto
// point란 점들을 모아놓고 길을 찍어준다.
public class NaviRouteDto {
  private List<PointDto> path;
}
