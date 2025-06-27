package com.serviceAgence;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

//import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

// @EnableDiscoveryClient
@SpringBootApplication

public class ServiceAgenceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServiceAgenceApplication.class, args);
	}

}
