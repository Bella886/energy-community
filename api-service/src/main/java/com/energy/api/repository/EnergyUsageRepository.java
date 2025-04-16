package com.energy.api.repository;

import com.energy.api.model.EnergyUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EnergyUsageRepository extends JpaRepository<EnergyUsage, Long> {

    @Query("SELECT e FROM EnergyUsage e WHERE e.hour >= :start AND e.hour <= :end ORDER BY e.hour")
    List<EnergyUsage> findByTimestampBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT e FROM EnergyUsage e WHERE e.hour >= :startOfHour AND e.hour < :endOfHour ORDER BY e.hour DESC")
    List<EnergyUsage> findCurrentHourData(@Param("startOfHour") LocalDateTime startOfHour, @Param("endOfHour") LocalDateTime endOfHour);
}
