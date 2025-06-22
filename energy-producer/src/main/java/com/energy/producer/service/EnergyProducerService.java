package com.energy.producer.service;

import com.energy.producer.config.RabbitMQConfig;
import com.energy.producer.model.EnergyMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class EnergyProducerService {

    private final RabbitTemplate rabbitTemplate;
    private final Gson gson;
    private final Random random;
    private final OkHttpClient httpClient;

    @Value("${energy.producer.interval.min}")
    private int minInterval;

    @Value("${energy.producer.interval.max}")
    private int maxInterval;

    @Value("${energy.producer.kwh.min}")
    private double minKwh;

    @Value("${energy.producer.kwh.max}")
    private double maxKwh;

    @Value("${weather.api.key:defaultkey}")
    private String weatherApiKey;

    @Value("${weather.api.location:Beijing}")
    private String weatherLocation;

    private double currentWeatherFactor = 1.0;
    private LocalDateTime lastWeatherCheck = LocalDateTime.MIN;

    public EnergyProducerService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new TypeAdapter<LocalDateTime>() {
                    @Override
                    public void write(JsonWriter out, LocalDateTime value) throws IOException {
                        out.value(value.toString());
                    }
                    @Override
                    public LocalDateTime read(JsonReader in) throws IOException {
                        return LocalDateTime.parse(in.nextString());
                    }
                })
                .create();
        this.random = new Random();
        this.httpClient = new OkHttpClient();
    }

    @Scheduled(fixedDelay = 1000)
    public void sendEnergyProductionMessage() {
        try {
            int interval = random.nextInt(maxInterval - minInterval + 1) + minInterval;
            TimeUnit.SECONDS.sleep(interval);

            updateWeatherFactorIfNeeded();

            double kwhProduced = generateKwhProduction();

            EnergyMessage message = new EnergyMessage(
                    "PRODUCER",
                    "COMMUNITY",
                    kwhProduced,
                    LocalDateTime.now()
            );

            String jsonMessage = gson.toJson(message);
            rabbitTemplate.convertAndSend(RabbitMQConfig.ENERGY_QUEUE, jsonMessage);

            log.info ("Send Energy Production Message: {} kWh (Weather Factor: {})", kwhProduced, currentWeatherFactor);        } catch (InterruptedException e) {
        } catch (Exception e) {
            log.error("Failed to send energy data", e);
        }
    }

    private void updateWeatherFactorIfNeeded() {
        if (LocalDateTime.now().minusMinutes(30).isAfter(lastWeatherCheck)) {
            try {
                currentWeatherFactor = fetchWeatherFactor();
                lastWeatherCheck = LocalDateTime.now();
                log.info("update weather factor: {}", currentWeatherFactor);
            } catch (Exception e) {
                log.error("Failed to acquire weather data", e);
            }
        }
    }

    private double fetchWeatherFactor() {
        try {
            // Simulate weather data-generate coefficients between 0.5 and 2.0
            // 0.5 cloudy, 2.0 sunny
            double weatherFactor = 0.5 + random.nextDouble() * 1.5;

            int hour = LocalDateTime.now().getHour();
            if (hour >= 6 && hour <= 18) {
                weatherFactor *= 1.5;
            } else {
                weatherFactor *= 1;
            }

            // Limit between 0.1 and 3.0
            return Math.max(0.1, Math.min(3.0, weatherFactor));

        } catch (Exception e) {
            log.error("Error getting weather data", e);
            return 1.0;
        }
    }

    private double generateKwhProduction() {
        double baseKwh = minKwh + (maxKwh - minKwh) * random.nextDouble(); // 0.08
        double weatherAdjustedKwh = baseKwh * currentWeatherFactor; // 2
        return Math.round(weatherAdjustedKwh * 1000.0) / 1000.0; // 2.000
    }
}
