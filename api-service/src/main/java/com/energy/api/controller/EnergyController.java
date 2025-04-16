package com.energy.api.controller;

import com.energy.api.model.CurrentEnergyUsage;
import com.energy.api.model.HistoricalEnergyUsage;
import com.energy.api.service.EnergyService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/energy")
public class EnergyController {

    private final EnergyService energyService;

    public EnergyController(EnergyService energyService) {
        this.energyService = energyService;
    }

    @GetMapping("/current")
    public CurrentEnergyUsage getCurrentHourUsage() {
        return energyService.getCurrentHourUsage();
    }

    @GetMapping("/historical")
    public HistoricalEnergyUsage getHistoricalUsage(String start, String end) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-dd-MM'T'HH:mm:ss");
        LocalDateTime startDateTime = LocalDateTime.parse(start, formatter);
        LocalDateTime endDateTime = LocalDateTime.parse(end, formatter);
        return energyService.getHistoricalUsage(startDateTime, endDateTime);
    }
}
