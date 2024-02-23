package com.example.ncpmaps;

import com.example.ncpmaps.dto.geolocation.GeoLocationNcpResponse;
import com.example.ncpmaps.service.NcpGeolocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("test")
@RequiredArgsConstructor
public class TestController {
  private final NcpGeolocationService geolocationService;

  @GetMapping("geolocation")
  public GeoLocationNcpResponse geoLocation(
      @RequestParam("ip")
      String ip
  ) {
      return geolocationService.geoLocation(Map.of(
        "ip", ip,
        "responseFormatType", "json",
        "ext", "t"
      ));
  }
}
