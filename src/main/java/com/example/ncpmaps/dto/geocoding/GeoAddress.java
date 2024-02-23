package com.example.ncpmaps.dto.geocoding;

import lombok.Data;

import java.util.List;

@Data
public class GeoAddress {
  private String roadAddress;
  private String jibunAddress;
  private String englishAddress;
  // addressElements의 상세 컬럼들은 필요가 없어서 Object로 사용
  private List<Object> addressElements;
  private String x;
  private String y;
  private Double distance;
}
