package com.example.ncpmaps.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NaviWithIpsDto {
    private String startIp;
    private String goalIp;
}
