package com.sportsbooking.mainapi.dto.parameter;

import lombok.Data;

/**
 * DTO de resultado MyBatis para la tabla {@code parameter}.
 *
 * <p>Usado como {@code resultType} en {@code parameter-mapper.xml}.
 */
@Data
public class ParameterDto {

    private Integer idParam;
    private Integer idParamParent;
    private Integer idParamChild;
    private String parentName;
    private String childName;
    private String description;
    private String icon;
    /** Tipo de icono: 1=nativo Flutter, 2=asset imagen, 3=URL. Null si no tiene icon. */
    private Integer iconType;
    private Integer rowOrder;
    private Integer status;
    private Double value;
}
