package com.energy.usage.repository;

import com.energy.usage.model.EnergyUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EnergyUsageRepository extends JpaRepository<EnergyUsage, Long> {
    
    @Query("SELECT e FROM EnergyUsage e WHERE e.hour = :hour")
    Optional<EnergyUsage> findByHour(@Param("hour") LocalDateTime hour);
} 