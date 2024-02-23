package com.example.ncpmaps.service;

import com.example.ncpmaps.dto.direction.DirectionNcpResponse;
import com.example.ncpmaps.dto.geocoding.GeoNcpResponse;
import com.example.ncpmaps.dto.rgeocoding.RGeoNcpResponse;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

import java.util.Map;

// HTTP 요청을 보내는 방법
public interface NcpMapApiService {
  // directions5
  @GetExchange("/map-direction/v1/driving")
  DirectionNcpResponse directions5(
          @RequestParam
          Map<String, Object> params
  );

  // geocode
  @GetExchange("/map-geocode/v2/geocode")
  GeoNcpResponse geocode(
          @RequestParam
          Map<String, Object> params
  );

  @GetExchange("/map-reversegeocode/v2/gc")
  RGeoNcpResponse reverseGeocode(
          @RequestParam
          Map<String, Object> params
  );
}
