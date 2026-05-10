package com.homemakers.homemakers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HomemakersApplication {

	public static void main(String[] args) {
		SpringApplication.run(HomemakersApplication.class, args);
	}

}
