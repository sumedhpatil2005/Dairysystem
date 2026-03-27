package com.dairy.dairy_management.entity;

public enum AdjustmentType {
    // Deductions
    DISPUTED_DELIVERY,    // Customer claims delivery didn't happen
    QUALITY_COMPLAINT,    // Milk was sour/spoiled
    QUANTITY_DISPUTE,     // Wrong quantity delivered
    DOUBLE_BILLING_FIX,   // Same date charged twice by mistake

    // Discounts
    LOYALTY_DISCOUNT,     // Long-term customer discount
    FESTIVAL_DISCOUNT,    // Seasonal/festival offer
    REFERRAL_DISCOUNT,    // Customer referred someone new

    // Surcharges
    LATE_PAYMENT_FEE,     // Bill overdue surcharge
    EXTRA_DELIVERY_FEE,   // Emergency delivery outside regular route
    PACKAGING_CHARGE,     // Special packaging requested

    // Credits
    GOODWILL_CREDIT,      // Compensation for inconvenience
    CREDIT_NOTE,          // Carry forward from previous dispute

    OTHER                 // Free-form / custom
}
