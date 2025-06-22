package com.energy.api.repository;

import com.energy.api.model.EnergyPercentage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnergyPercentageRepository extends JpaRepository<EnergyPercentage, Long> {
    
    @Query("SELECT p FROM EnergyPercentage p WHERE p.hour = :hour")
    Optional<EnergyPercentage> findByHour(@Param("hour") LocalDateTime hour);
    
    @Query("SELECT p FROM EnergyPercentage p WHERE p.hour >= :startOfHour AND p.hour < :endOfHour ORDER BY p.hour DESC")
    List<EnergyPercentage> findCurrentHourData(@Param("startOfHour") LocalDateTime startOfHour, @Param("endOfHour") LocalDateTime endOfHour);
    
    @Query("SELECT p FROM EnergyPercentage p WHERE p.hour >= :start AND p.hour <= :end ORDER BY p.hour")
    List<EnergyPercentage> findByTimestampBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
} 