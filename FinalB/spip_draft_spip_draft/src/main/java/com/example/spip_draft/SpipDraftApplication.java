package com.example.spip_draft;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
@EnableScheduling
@SpringBootApplication
public class SpipDraftApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpipDraftApplication.class, args);
	}

}
