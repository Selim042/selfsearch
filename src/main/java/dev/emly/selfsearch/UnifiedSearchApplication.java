package dev.emly.selfsearch;

import java.util.Map;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import dev.emly.selfsearch.service.SearchService;

@SpringBootApplication
public class UnifiedSearchApplication {

	public static void main(String... args) {
		SpringApplication.run(UnifiedSearchApplication.class, args);
	}

	@Bean
	public CommandLineRunner printDetectedServices(ApplicationContext ctx) {
		return args -> {
			Map<String, SearchService> services = ctx.getBeansOfType(SearchService.class);
			System.out.println("=============================");
			System.out.println("DETECTED SEARCH SERVICES COUNT: " + services.size());
			services.forEach((name, service) -> System.out
					.println(" -> Registered Bean: " + name + " (" + service.getServiceName() + ")"));
			System.out.println("=============================");
		};
	}

}
