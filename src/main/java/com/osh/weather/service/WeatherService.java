package com.osh.weather.service;

import com.osh.weather.model.AmapWeatherResponse;
import org.springframework.beans.factory.annotation.Value;
import tools.jackson.databind.JsonNode;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

@Service
public class WeatherService {

    private static final String GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String FORECAST_URL = "https://api.open-meteo.com/v1/forecast";

    @Value("${api.key}")
    private String apiKey;

    private final RestClient restClient;

    // WMO 天气代码 → 中文描述
    private static final Map<Integer, String> WEATHER_CODE_MAP = Map.ofEntries(
            Map.entry(0, "晴天"), Map.entry(1, "大部晴朗"), Map.entry(2, "多云"), Map.entry(3, "阴天"),
            Map.entry(45, "雾"), Map.entry(48, "雾凇"),
            Map.entry(51, "小毛毛雨"), Map.entry(53, "毛毛雨"), Map.entry(55, "大毛毛雨"),
            Map.entry(56, "冻毛毛雨"), Map.entry(57, "冻雨"),
            Map.entry(61, "小雨"), Map.entry(63, "中雨"), Map.entry(65, "大雨"),
            Map.entry(66, "小冻雨"), Map.entry(67, "大冻雨"),
            Map.entry(71, "小雪"), Map.entry(73, "中雪"), Map.entry(75, "大雪"),
            Map.entry(77, "雪粒"), Map.entry(80, "小阵雨"), Map.entry(81, "阵雨"), Map.entry(82, "大阵雨"),
            Map.entry(85, "小阵雪"), Map.entry(86, "大阵雪"),
            Map.entry(95, "雷暴"), Map.entry(96, "雷暴伴冰雹"), Map.entry(99, "强雷暴伴冰雹")
    );

    public WeatherService() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @McpTool(description = "获取国外城市的实时天气信息，包括温度、湿度、风速和天气状况")
    public String getForeignWeather(@McpToolParam(description = "城市名称（支持中英文，如 Beijing、上海）", required = true) String city) {
        try {
            // 第一步：地理编码，将城市名转为经纬度
            JsonNode geoResult = geocodeCity(city);
            if (geoResult == null) {
                return "无法找到城市「" + city + "」的地理信息，请检查城市名称是否正确。";
            }

            double latitude = geoResult.get("latitude").asDouble();
            double longitude = geoResult.get("longitude").asDouble();
            String resolvedName = geoResult.has("name") ? geoResult.get("name").asText() : city;
            String country = geoResult.has("country") ? geoResult.get("country").asText() : "";

            // 第二步：查询实时天气
            JsonNode weather = fetchCurrentWeather(latitude, longitude);
            if (weather == null) {
                return "无法获取「" + resolvedName + "」的天气数据。";
            }

            // 第三步：解析并格式化结果
            return formatWeather(resolvedName, country, weather);

        } catch (Exception e) {
            return "查询天气时发生错误：" + e.getMessage();
        }
    }

    private JsonNode geocodeCity(String city) {
        String url = GEOCODING_URL + "?name=" + city + "&count=1&language=zh";
        JsonNode response = restClient.get()
                .uri(url)
                .retrieve()
                .body(JsonNode.class);

        if (response != null && response.has("results") && !response.get("results").isEmpty()) {
            return response.get("results").get(0);
        }
        // 降级：用英文再查一次
        String fallbackUrl = GEOCODING_URL + "?name=" + city + "&count=1&language=en";
        JsonNode fallbackResponse = restClient.get()
                .uri(fallbackUrl)
                .retrieve()
                .body(JsonNode.class);

        if (fallbackResponse != null && fallbackResponse.has("results") && !fallbackResponse.get("results").isEmpty()) {
            return fallbackResponse.get("results").get(0);
        }
        return null;
    }

    private JsonNode fetchCurrentWeather(double latitude, double longitude) {
        String url = FORECAST_URL + "?latitude=" + latitude + "&longitude=" + longitude
                + "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m"
                + "&timezone=auto";
        JsonNode response = restClient.get()
                .uri(url)
                .retrieve()
                .body(JsonNode.class);

        if (response != null && response.has("current")) {
            return response.get("current");
        }
        return null;
    }

    private String formatWeather(String cityName, String country, JsonNode current) {
        double temperature = current.get("temperature_2m").asDouble();
        double apparentTemp = current.has("apparent_temperature")
                ? current.get("apparent_temperature").asDouble() : temperature;
        int humidity = current.get("relative_humidity_2m").asInt();
        double windSpeed = current.get("wind_speed_10m").asDouble();
        int weatherCode = current.get("weather_code").asInt();

        String weatherDesc = WEATHER_CODE_MAP.getOrDefault(weatherCode, "未知（代码" + weatherCode + "）");

        return String.format(
                """
                        🌍 %s（%s）的实时天气：
                          天气状况：%s
                          当前温度：%.1f°C（体感 %.1f°C）
                          相对湿度：%d%%
                          风速：%.1f km/h""",
                cityName, country, weatherDesc, temperature, apparentTemp, humidity, windSpeed
        );
    }

    @McpTool(description = "查询国内天气")
    public AmapWeatherResponse getWeather(@McpToolParam(required = true,description = "城市") String city) {
        try {
            return restClient.get()
                    .uri("https://restapi.amap.com/v3/weather/weatherInfo?city={city}&key={key}&extensions=all", city, apiKey)
                    .retrieve()
                    .body(AmapWeatherResponse.class);
        } catch (RestClientException e) {
            throw new IllegalStateException("调用高德天气接口失败或超时：" + e.getMessage(), e);
        }
    }
}
