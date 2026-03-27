package com.dairy.dairy_management.entity;

public enum OverrideType {
    PAUSED,            // Skip delivery for this customer on this date
    QUANTITY_MODIFIED  // Change delivery quantity for this customer on this date
}
