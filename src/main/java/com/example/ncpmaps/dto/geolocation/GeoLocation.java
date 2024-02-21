package com.example.ncpmaps.dto.geolocation;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class GeoLocation {
    private String country;
    private String code;
    private String r1;
    private String r2;
    private String r3;
    private Double lat;
    @JsonAlias("long")
    private Double lng;
    private String net;
}
