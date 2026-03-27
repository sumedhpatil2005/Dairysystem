package com.dairy.dairy_management.controller;

import com.dairy.dairy_management.dto.CreateHolidayRequest;
import com.dairy.dairy_management.entity.Holiday;
import com.dairy.dairy_management.service.HolidayService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/holidays")
public class HolidayController {

    private final HolidayService service;

    public HolidayController(HolidayService service) {
        this.service = service;
    }

    /**
     * Mark a date as a system-wide holiday (no deliveries for anyone).
     * Example: POST /holidays  { "date": "2026-11-01", "reason": "Diwali" }
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Holiday create(@Valid @RequestBody CreateHolidayRequest request) {
        return service.create(request);
    }

    /**
     * Get all registered holidays.
     */
    @GetMapping
    public List<Holiday> getAll() {
        return service.getAll();
    }

    /**
     * Get a holiday by ID.
     */
    @GetMapping("/{id}")
    public Holiday getById(@PathVariable Long id) {
        return service.getById(id);
    }

    /**
     * Remove a holiday (deliveries will no longer be blocked on that date).
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
