package com.rest.wrapper.ezpie.warmpie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 
 * WarmPie is simply a SprintBoot wrapper around the ezPie component, it does 
 * include a Swagger UI for testing the REST Endpoints.
 * 
 * @author Rick Monson (https://www.linkedin.com/in/rick-monson/)
 * @since 2018-09-27
 * 
 */

@SpringBootApplication
public class Warmpie {

	public static void main(String[] args) {
		SpringApplication.run(Warmpie.class, args);
	}
}
