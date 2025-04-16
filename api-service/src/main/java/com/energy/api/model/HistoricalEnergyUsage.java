package com.energy.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HistoricalEnergyUsage {
    private double communityProduced;
    private double communityUsed;
    private double gridUsed;
}
