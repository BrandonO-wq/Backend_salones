package com.sportsbooking.mainapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbooking.mainapi.config.JwtProperties;
import com.sportsbooking.mainapi.config.OtpProperties;
import com.sportsbooking.mainapi.config.RedisProperties;
import com.sportsbooking.mainapi.config.VenueProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@MapperScan("com.sportsbooking.mainapi.jpa.myrepository")
@EnableConfigurationProperties({ JwtProperties.class, OtpProperties.class, RedisProperties.class, VenueProperties.class })
public class MainApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MainApiApplication.class, args);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}

