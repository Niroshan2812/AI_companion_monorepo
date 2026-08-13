package com.pm.javagateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JavaGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(JavaGatewayApplication.class, args);
	}

}
