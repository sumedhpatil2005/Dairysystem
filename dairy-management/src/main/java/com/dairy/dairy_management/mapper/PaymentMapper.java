package com.dairy.dairy_management.mapper;

import com.dairy.dairy_management.dto.PaymentResponse;
import com.dairy.dairy_management.entity.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toDto(Payment payment) {
        PaymentResponse dto = new PaymentResponse();
        dto.setId(payment.getId());
        dto.setAmount(payment.getAmount());
        dto.setMode(payment.getMode());
        dto.setPaymentDate(payment.getPaymentDate());

        if (payment.getBill() != null) {
            dto.setBillId(payment.getBill().getId());
            dto.setTotalAmount(payment.getBill().getTotalAmount());
            dto.setPaidAmount(payment.getBill().getPaidAmount());
            dto.setRemainingAmount(payment.getBill().getRemainingAmount());
        }

        return dto;
    }
}
