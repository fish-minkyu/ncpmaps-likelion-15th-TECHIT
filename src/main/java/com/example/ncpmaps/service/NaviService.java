package com.example.ncpmaps.service;

import com.example.ncpmaps.dto.NaviRouteDto;
import com.example.ncpmaps.dto.NaviWithPointsDto;
import com.example.ncpmaps.dto.NaviWithQueryDto;
import com.example.ncpmaps.dto.PointDto;
import com.example.ncpmaps.dto.direction.DirectionNcpResponse;
import com.example.ncpmaps.dto.geocoding.GeoNcpResponse;
import com.example.ncpmaps.dto.rgeocoding.RGeoNcpResponse;
import com.example.ncpmaps.dto.rgeocoding.RGeoRegion;
import com.example.ncpmaps.dto.rgeocoding.RGeoResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NaviService {
    private final NcpMapApiService mapApiService;
    private final NcpGeolocationService geolocationService;

    public NaviRouteDto twoPointRoute(NaviWithPointsDto dto) {
        Map<String, Object> params = new HashMap<>();
        params.put("start", dto.getStart().toQueryValue());
        params.put("goal", dto.getGoal().toQueryValue());
        DirectionNcpResponse response = mapApiService.directions5(params);
        List<PointDto> path = new ArrayList<>();
        response.getRoute()
                .get("traoptimal")
                .get(0)
                .getPath()
                .forEach(point
                        -> path.add(new PointDto(point.get(1), point.get(0))));
        return new NaviRouteDto(path);
    }

    public RGeoResponseDto getAddress(PointDto pointDto) {
        Map<String, Object> params = new HashMap<>();
        params.put("coords", pointDto.toQueryValue());
        params.put("output", "json");
        RGeoNcpResponse response = mapApiService.reverseGeocode(params);
        RGeoRegion region = response.getResults()
                        .get(0)
                        .getRegion();

        String address = region.getArea0().getName() + " " +
                region.getArea1().getName() + " " +
                region.getArea2().getName() + " " +
                region.getArea3().getName() + " " +
                region.getArea4().getName();
        return new RGeoResponseDto(address.trim());
    }

    public NaviRouteDto startQuery(NaviWithQueryDto dto) {
        // 주소의 좌표부터 찾기
        Map<String, Object> params = new HashMap<>();
        params.put("query", dto.getQuery());
        params.put("coordinate", dto.getStart().toQueryValue());
        params.put("page", 1);
        params.put("count", 1);
        GeoNcpResponse response = mapApiService.geocode(params);
        log.info(response.toString());
        Double lat = Double.valueOf(response.getAddresses().get(0).getY());
        Double lng = Double.valueOf(response.getAddresses().get(0).getX());
        PointDto goal = new PointDto(lat, lng);
        // 경로를 찾아 반환하기
        return this.twoPointRoute(new NaviWithPointsDto(
                dto.getStart(),
                goal
        ));
    }
}



