package com.energy.percentage.service;

import com.energy.percentage.config.RabbitMQConfig;
import com.energy.percentage.model.EnergyPercentage;
import com.energy.percentage.model.EnergyUsage;
import com.energy.percentage.model.UpdateMessage;
import com.energy.percentage.repository.EnergyPercentageRepository;
import com.energy.percentage.repository.EnergyUsageRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class PercentageService {

    private final EnergyUsageRepository energyUsageRepository;
    private final EnergyPercentageRepository energyPercentageRepository;
    private final Gson gson;

    public PercentageService(EnergyUsageRepository energyUsageRepository,
                           EnergyPercentageRepository energyPercentageRepository) {
        this.energyUsageRepository = energyUsageRepository;
        this.energyPercentageRepository = energyPercentageRepository;
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
    }

    @RabbitListener(queues = RabbitMQConfig.UPDATE_QUEUE)
    public void processUpdateMessage(String message) {
        try {
            log.info("Received update message: {}", message);
            UpdateMessage updateMessage = gson.fromJson(message, UpdateMessage.class);

            if ("USAGE_UPDATED".equals(updateMessage.getType())) {
                LocalDateTime hour = updateMessage.getHour();
                calculateAndSavePercentages(hour);
            }
        } catch (Exception e) {
            log.error("Error processing update message", e);
        }
    }

    private void calculateAndSavePercentages(LocalDateTime hour) {
        Optional<EnergyUsage> usageOpt = energyUsageRepository.findByHour(hour);

        if (usageOpt.isPresent()) {
            EnergyUsage usage = usageOpt.get();
            double total = usage.getCommunityProduced();

            if (total > 0) {
                double communityDepleted = round(usage.getCommunityUsed() / total * 100);
                double gridPortion = round(usage.getGridUsed() / total * 100);

                // Find or create percentage record
                EnergyPercentage percentage = findOrCreatePercentage(hour);
                percentage.setCommunityDepleted(communityDepleted);
                percentage.setGridPortion(gridPortion);

                // Save updated percentages
                energyPercentageRepository.save(percentage);
                log.info("Updated energy percentages for hour: {}, community: {}%, grid: {}%",
                        hour, communityDepleted, gridPortion);
            } else {
                log.info("No energy usage for hour: {}, skipping percentage calculation", hour);
            }
        } else {
            log.warn("No energy usage data found for hour: {}", hour);
        }
    }

    private EnergyPercentage findOrCreatePercentage(LocalDateTime hour) {
        Optional<EnergyPercentage> existingPercentage = energyPercentageRepository.findByHour(hour);

        if (existingPercentage.isPresent()) {
            return existingPercentage.get();
        } else {
            EnergyPercentage newPercentage = new EnergyPercentage();
            newPercentage.setHour(hour);
            newPercentage.setCommunityDepleted(0.0);
            newPercentage.setGridPortion(0.0);
            return newPercentage;
        }
    }

    private double round(double value) {
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
