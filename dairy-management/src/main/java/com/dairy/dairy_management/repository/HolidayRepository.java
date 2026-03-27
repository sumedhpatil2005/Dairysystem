package com.dairy.dairy_management.repository;

import com.dairy.dairy_management.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    boolean existsByDate(LocalDate date);

    Optional<Holiday> findByDate(LocalDate date);
}
