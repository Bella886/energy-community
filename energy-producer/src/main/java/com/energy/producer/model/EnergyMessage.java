package com.energy.producer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnergyMessage {
    private String type;
    private String association;
    private double kwh;
    private LocalDateTime datetime;
} 