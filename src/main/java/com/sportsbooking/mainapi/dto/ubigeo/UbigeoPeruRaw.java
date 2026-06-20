package com.sportsbooking.mainapi.dto.ubigeo;

import lombok.Data;

@Data
public class UbigeoPeruRaw {
    private String inei;
    private String departamento;
    private String provincia;
    private String distrito;
    private Double latitude;
    private Double longitude;
}
