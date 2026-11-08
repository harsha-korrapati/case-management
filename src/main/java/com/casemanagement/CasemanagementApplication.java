package com.casemanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CasemanagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(CasemanagementApplication.class, args);
	}

}
