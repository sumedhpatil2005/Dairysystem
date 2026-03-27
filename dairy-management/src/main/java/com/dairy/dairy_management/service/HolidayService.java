package com.dairy.dairy_management.service;

import com.dairy.dairy_management.dto.CreateHolidayRequest;
import com.dairy.dairy_management.entity.Holiday;
import com.dairy.dairy_management.repository.HolidayRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class HolidayService {

    private final HolidayRepository repo;

    public HolidayService(HolidayRepository repo) {
        this.repo = repo;
    }

    public Holiday create(CreateHolidayRequest request) {
        if (repo.existsByDate(request.getDate())) {
            throw new RuntimeException("Holiday already exists for date: " + request.getDate());
        }

        Holiday holiday = new Holiday();
        holiday.setDate(request.getDate());
        holiday.setReason(request.getReason());
        return repo.save(holiday);
    }

    public List<Holiday> getAll() {
        return repo.findAll();
    }

    public Holiday getById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Holiday not found"));
    }

    public boolean isHoliday(LocalDate date) {
        return repo.existsByDate(date);
    }

    public void delete(Long id) {
        Holiday holiday = getById(id);
        repo.delete(holiday);
    }
}
