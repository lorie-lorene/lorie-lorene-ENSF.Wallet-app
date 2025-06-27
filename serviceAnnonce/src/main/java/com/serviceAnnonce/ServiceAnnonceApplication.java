package com.serviceAnnonce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
@EnableDiscoveryClient
@SpringBootApplication
public class ServiceAnnonceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServiceAnnonceApplication.class, args);
	}

}
