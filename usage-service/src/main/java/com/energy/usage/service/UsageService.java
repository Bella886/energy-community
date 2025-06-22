package com.energy.usage.service;

import com.energy.usage.config.RabbitMQConfig;
import com.energy.usage.model.EnergyMessage;
import com.energy.usage.model.EnergyUsage;
import com.energy.usage.model.UpdateMessage;
import com.energy.usage.repository.EnergyUsageRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@Slf4j
public class UsageService {

    private final EnergyUsageRepository energyUsageRepository;
    private final RabbitTemplate rabbitTemplate;
    private final Gson gson;

    public UsageService(EnergyUsageRepository energyUsageRepository, RabbitTemplate rabbitTemplate) {
        this.energyUsageRepository = energyUsageRepository;
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
    }

    @RabbitListener(queues = RabbitMQConfig.ENERGY_QUEUE)
    public void processEnergyMessage(String message) {
        try {
            log.info("Received message: {}", message);
            EnergyMessage energyMessage = gson.fromJson(message, EnergyMessage.class);

            // Truncate datetime to hour
            LocalDateTime hourTimestamp = energyMessage.getDatetime().truncatedTo(ChronoUnit.HOURS);

            // Find or create energy usage record for this hour
            EnergyUsage energyUsage = findOrCreateEnergyUsage(hourTimestamp);

            // Update energy usage based on message type
            updateEnergyUsage(energyUsage, energyMessage);

            // Save updated energy usage
            energyUsageRepository.save(energyUsage);
            log.info("Updated energy usage for hour: {}", hourTimestamp);

            // Notify other services about the update
            sendUpdateNotification(hourTimestamp);

        } catch (Exception e) {
            log.error("Error processing energy message", e);
        }
    }

    private EnergyUsage findOrCreateEnergyUsage(LocalDateTime hour) {
        Optional<EnergyUsage> existingUsage = energyUsageRepository.findByHour(hour);

        if (existingUsage.isPresent()) {
            return existingUsage.get();
        } else {
            EnergyUsage newUsage = new EnergyUsage();
            newUsage.setHour(hour);
            newUsage.setCommunityProduced(0.0);
            newUsage.setCommunityUsed(0.0);
            newUsage.setGridUsed(0.0);
            return newUsage;
        }
    }

    private void updateEnergyUsage(EnergyUsage energyUsage, EnergyMessage message) {
        if ("PRODUCER".equals(message.getType()) && "COMMUNITY".equals(message.getAssociation())) {
            // Update community production
            energyUsage.setCommunityProduced(energyUsage.getCommunityProduced() + message.getKwh());
        } else if ("USER".equals(message.getType()) && "COMMUNITY".equals(message.getAssociation())) {
            // Update community usage
            double requestedUsage = message.getKwh();
            double availableFromCommunity = energyUsage.getCommunityProduced() - energyUsage.getCommunityUsed();

            if (availableFromCommunity >= requestedUsage) {
                // All energy can be supplied from community
                energyUsage.setCommunityUsed(energyUsage.getCommunityUsed() + requestedUsage);
            } else {
                // Need to use grid for the remaining energy
                double fromCommunity = Math.max(0, availableFromCommunity);
                double fromGrid = requestedUsage - fromCommunity;

                energyUsage.setCommunityUsed(energyUsage.getCommunityUsed() + fromCommunity);
                energyUsage.setGridUsed(energyUsage.getGridUsed() + fromGrid);
            }
        }
    }

    private void sendUpdateNotification(LocalDateTime hour) {
        UpdateMessage updateMessage = new UpdateMessage("USAGE_UPDATED", hour);
        String jsonMessage = gson.toJson(updateMessage);
        rabbitTemplate.convertAndSend(RabbitMQConfig.UPDATE_QUEUE, jsonMessage);
        log.info("Sent update notification for hour: {}", hour);
    }
}
