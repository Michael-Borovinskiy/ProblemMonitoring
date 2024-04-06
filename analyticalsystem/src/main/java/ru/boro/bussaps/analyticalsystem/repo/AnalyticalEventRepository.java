package ru.boro.bussaps.analyticalsystem.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.boro.bussaps.analyticalsystem.entity.AnalyticalEvent;

@Repository
public interface AnalyticalEventRepository extends JpaRepository<AnalyticalEvent, Long> {
}
