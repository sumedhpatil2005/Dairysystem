package com.dairy.dairy_management.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Returned by mark-as-mistake endpoints.
 * Contains the updated item and the recalculated bill (if one existed for that month).
 */
@Data
@AllArgsConstructor
public class MistakeCorrectionResponse {

    private String message;

    // true if a bill existed for that month and was recalculated
    private boolean billRecalculated;

    // The recalculated bill — null if no bill existed yet for that month
    private BillResponse updatedBill;
}
