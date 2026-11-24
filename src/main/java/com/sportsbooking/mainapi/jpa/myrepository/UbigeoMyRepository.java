package com.sportsbooking.mainapi.jpa.myrepository;

import com.sportsbooking.mainapi.dto.ubigeo.UbigeoPeruRaw;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UbigeoMyRepository {
    List<UbigeoPeruRaw> findAll();

    @Select("SELECT CONCAT(distrito, ', ', provincia, ', ', departamento) FROM ubigeo_peru WHERE inei = #{inei}")
    String getUbigeoText(String inei);
}
