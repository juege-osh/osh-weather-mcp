package com.osh.weather.controller;

import com.osh.weather.model.AmapWeatherResponse;
import com.osh.weather.service.WeatherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @program: mcp-server-weather
 * @packageName: com.osh.weather.controller
 * @ClassName WeatherController
 * @author: liu
 * @date: 2026-06-16 11:21
 * @description:
 **/

@RestController
public class WeatherController {

    @Autowired
    private WeatherService weatherService;

    @GetMapping("/getForeignWeather")
    public String getForeignWeather(String city) {
        return weatherService.getForeignWeather(city);
    }

    @GetMapping("/getWeather")
    public AmapWeatherResponse getWeather(String city) {
        return weatherService.getWeather(city);
    }

}
