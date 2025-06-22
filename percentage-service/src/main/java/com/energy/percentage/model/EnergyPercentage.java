package com.energy.percentage.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "energy_percentage")
public class EnergyPercentage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime hour;

    @Column(name = "community_depleted", nullable = false)
    private double communityDepleted;

    @Column(name = "grid_portion", nullable = false)
    private double gridPortion;
} 