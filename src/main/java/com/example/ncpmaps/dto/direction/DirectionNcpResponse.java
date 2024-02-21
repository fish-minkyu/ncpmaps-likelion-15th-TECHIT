package com.example.ncpmaps.dto.direction;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DirectionNcpResponse {
    private Integer code;
    private String message;
    private String currentDateTime;
    private Map<String, List<DirectionRoute>> route;
}
