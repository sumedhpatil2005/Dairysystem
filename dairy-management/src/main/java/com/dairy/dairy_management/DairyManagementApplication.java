package com.dairy.dairy_management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DairyManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(DairyManagementApplication.class, args);
	}

}
