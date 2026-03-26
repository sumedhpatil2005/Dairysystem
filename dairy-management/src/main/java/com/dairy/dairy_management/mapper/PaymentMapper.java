package com.dairy.dairy_management.mapper;

import com.dairy.dairy_management.dto.PaymentResponse;
import com.dairy.dairy_management.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(source = "bill.id", target = "billId")
    @Mapping(source = "bill.totalAmount", target = "totalAmount")
    @Mapping(source = "bill.paidAmount", target = "paidAmount")
    @Mapping(source = "bill.remainingAmount", target = "remainingAmount")
    PaymentResponse toDto(Payment payment);
}