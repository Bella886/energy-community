package com.energy.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CurrentEnergyUsage {
    private double communityDepleted;
    private double gridPortion;
}
