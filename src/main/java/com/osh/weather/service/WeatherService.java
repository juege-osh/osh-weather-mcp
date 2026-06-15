package com.osh.weather.service;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

@Service
public class WeatherService {

    @McpTool(description = "获取天气信息")
    public String getWeather(@McpToolParam(description = "城市", required = false) String city) {
        return "当前" + city + "的天气是晴天，温度25摄氏度，湿度60%。";
    }

}
