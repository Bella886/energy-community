package com.energy.percentage.repository;

import com.energy.percentage.model.EnergyPercentage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EnergyPercentageRepository extends JpaRepository<EnergyPercentage, Long> {
    
    @Query("SELECT p FROM EnergyPercentage p WHERE p.hour = :hour")
    Optional<EnergyPercentage> findByHour(@Param("hour") LocalDateTime hour);
} 