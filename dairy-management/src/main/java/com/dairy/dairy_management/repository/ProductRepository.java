package com.dairy.dairy_management.repository;

import com.dairy.dairy_management.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}