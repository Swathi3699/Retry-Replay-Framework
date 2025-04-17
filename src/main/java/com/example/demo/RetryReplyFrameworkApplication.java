package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class RetryReplyFrameworkApplication {

	public static void main(String[] args) {
		SpringApplication.run(RetryReplyFrameworkApplication.class, args);
	}

}
