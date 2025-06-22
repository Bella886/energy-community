package com.energy.user.service;

import com.energy.user.config.RabbitMQConfig;
import com.energy.user.model.EnergyMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class EnergyUserService {

    private final RabbitTemplate rabbitTemplate;
    private final Gson gson;
    private final Random random;

    @Value("${energy.user.interval.min}")
    private int minInterval;

    @Value("${energy.user.interval.max}")
    private int maxInterval;

    @Value("${energy.user.kwh.min}")
    private double minKwh;

    @Value("${energy.user.kwh.max}")
    private double maxKwh;

    @Value("${energy.user.peak.morning.start}")
    private int peakMorningStart;

    @Value("${energy.user.peak.morning.end}")
    private int peakMorningEnd;

    @Value("${energy.user.peak.evening.start}")
    private int peakEveningStart;

    @Value("${energy.user.peak.evening.end}")
    private int peakEveningEnd;

    public EnergyUserService(RabbitTemplate rabbitTemplate) {
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
    }

    @Scheduled(fixedDelay = 1000)
    public void sendEnergyUsageMessage() {
        try {
            // Generate random interval between messages
            int interval = random.nextInt(maxInterval - minInterval + 1) + minInterval;
            TimeUnit.SECONDS.sleep(interval);

            // Generate energy usage data
            double kwhUsed = generateKwhUsage();

            // Create message
            EnergyMessage message = new EnergyMessage(
                    "USER",
                    "COMMUNITY",
                    kwhUsed,
                    LocalDateTime.now()
            );

            // Send message to queue
            String jsonMessage = gson.toJson(message);
            rabbitTemplate.convertAndSend(RabbitMQConfig.ENERGY_QUEUE, jsonMessage);

            log.info("Send Energy Usage Message: {} kWh (Period Factor: {})", kwhUsed, getTimeOfDayMultiplier());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Energy use scheduling error", e);
        }
    }

    private double generateKwhUsage() {
        // Generate random kWh value
        double baseKwh = minKwh + (maxKwh - minKwh) * random.nextDouble();

        // Apply time of day multiplier
        double timeAdjustedKwh = baseKwh * getTimeOfDayMultiplier();

        // Apply day of week multiplier
        timeAdjustedKwh *= getDayOfWeekMultiplier();

        // Apply random fluctuation (±10%)
        double randomFactor = 0.9 + random.nextDouble() * 0.2;
        timeAdjustedKwh *= randomFactor;

        // Round to 3 decimal places
        return Math.round(timeAdjustedKwh * 1000.0) / 1000.0;
    }

    private double getTimeOfDayMultiplier() {
        int hour = LocalDateTime.now().getHour();

        // Morning peak hours (7-10 AM)
        if (hour >= peakMorningStart && hour <= peakMorningEnd) {
            // Morning peak usage increase
            double peakFactor = 1.5;

            return peakFactor;
        }
        // Evening peak hours (5-10 PM)
        else if (hour >= peakEveningStart && hour <= peakEveningEnd) {
            // Evening peak usage increase
            double peakFactor = 1.8;
            return peakFactor;
        }
        // Nighttime low (23-5 AM)
        else if (hour == 23 || hour <= 5) {
            return 0.8;
        }
        // Other times (normal usage)
        else {
            return 1.0;
        }
    }

    private double getDayOfWeekMultiplier() {
        DayOfWeek day = LocalDateTime.now().getDayOfWeek();

        // Weekend usage pattern differs from weekdays
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            int hour = LocalDateTime.now().getHour();
            // Morning usage less on weekends
            if (hour >= 6 && hour <= 10) {
                return 0.7;
            }
            // Daytime usage more on weekends
            else if (hour >= 11 && hour <= 18) {
                return 1.3;
            }
            // Other times similar to weekdays
            else {
                return 1.0;
            }
        }

        // Default weekday multiplier
        return 1.0;
    }
}
