package com.energy.api.service;

import com.energy.api.model.CurrentEnergyUsage;
import com.energy.api.model.EnergyPercentage;
import com.energy.api.model.HistoricalEnergyUsage;
import com.energy.api.model.EnergyUsage;
import com.energy.api.repository.EnergyPercentageRepository;
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
    private final EnergyPercentageRepository energyPercentageRepository;

    public EnergyService(EnergyUsageRepository energyUsageRepository,
                        EnergyPercentageRepository energyPercentageRepository) {
        this.energyUsageRepository = energyUsageRepository;
        this.energyPercentageRepository = energyPercentageRepository;
    }

    public CurrentEnergyUsage getCurrentHourUsage() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfHour = now.truncatedTo(ChronoUnit.HOURS); // 当前到下一个小时
        LocalDateTime endOfHour = startOfHour.plusHours(1);

        List<EnergyPercentage> currentHourData = energyPercentageRepository.findCurrentHourData(startOfHour, endOfHour);

        if (currentHourData.isEmpty()) {
            return new CurrentEnergyUsage(0.0, 0.0);
        }

        EnergyPercentage latestRecord = currentHourData.get(0);

        return new CurrentEnergyUsage(
                latestRecord.getCommunityDepleted(),
                latestRecord.getGridPortion()
        );
    }

    public HistoricalEnergyUsage getHistoricalUsage(LocalDateTime start, LocalDateTime end) {
        List<EnergyUsage> historicalData = energyUsageRepository.findByTimestampBetween(start, end);

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
