package com.serviceDemande;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

//@EnableDiscoveryClient
@SpringBootApplication
public class ServiceDemandeApplication {

	public static void main(String[] args) {
		System.out.println("🚀 Service Demande démarré avec succès!");
		System.out.println("📊 Dashboard disponible sur: http://localhost:8081/api/v1/demande/dashboard");
		System.out.println("📚 Documentation API: http://localhost:8081/swagger-ui.html");
		// ✅ Désactiver JMX au démarrage
		System.setProperty("spring.jmx.enabled", "false");
		System.setProperty("spring.application.admin.enabled", "false");

		SpringApplication.run(ServiceDemandeApplication.class, args);

	}

}
