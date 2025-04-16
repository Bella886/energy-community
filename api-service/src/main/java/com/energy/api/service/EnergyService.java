package com.energy.api.service;


import com.energy.api.model.CurrentEnergyUsage;
import com.energy.api.model.HistoricalEnergyUsage;
import com.energy.api.model.EnergyUsage;
import com.energy.api.repository.EnergyUsageRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class EnergyService {

    private final EnergyUsageRepository energyUsageRepository;

    public EnergyService(EnergyUsageRepository energyUsageRepository) {
        this.energyUsageRepository = energyUsageRepository;
    }

    public CurrentEnergyUsage getCurrentHourUsage() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfHour = now.truncatedTo(ChronoUnit.HOURS);
        LocalDateTime endOfHour = startOfHour.plusHours(1);

        List<EnergyUsage> currentHourData = energyUsageRepository.findCurrentHourData(startOfHour, endOfHour);

        if (currentHourData.isEmpty()) {
            return new CurrentEnergyUsage(0.0, 0.0);
        }

        // Get the latest record for the current hour
        EnergyUsage latestRecord = currentHourData.get(0);

        double totalUsed = latestRecord.getCommunityUsed() + latestRecord.getGridUsed();

        if (totalUsed == 0) {
            return new CurrentEnergyUsage(0.0, 0.0);
        }

        double communityDepleted = round(latestRecord.getCommunityUsed() / totalUsed * 100);
        double gridPortion = round(latestRecord.getGridUsed() / totalUsed * 100);

        return new CurrentEnergyUsage(communityDepleted, gridPortion);
    }

    public HistoricalEnergyUsage getHistoricalUsage(LocalDateTime start, LocalDateTime end) {
        List<EnergyUsage> historicalData = energyUsageRepository.findByTimestampBetween(start, end);

        System.out.println(start + " " + end);
        double communityProduced = 0.0;
        double communityUsed = 0.0;
        double gridUsed = 0.0;

        for (EnergyUsage record : historicalData) {
            communityProduced += record.getCommunityProduced();
            communityUsed += record.getCommunityUsed();
            gridUsed += record.getGridUsed();
        }

        return new HistoricalEnergyUsage(
                round(communityProduced),
                round(communityUsed),
                round(gridUsed)
        );
    }

    private double round(double value) {
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
