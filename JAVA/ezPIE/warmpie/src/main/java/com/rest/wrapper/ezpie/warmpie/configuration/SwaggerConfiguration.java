package com.rest.wrapper.ezpie.warmpie.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.rest.wrapper.ezpie.warmpie.controllers.PieController;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@PropertySource("classpath:swagger.properties")
@ComponentScan(basePackageClasses = PieController.class)
@Configuration
public class SwaggerConfiguration {
	
	private static final String SWAGGER_API_VERSION = "1.0";
	private static final String LICENSE_TEXT = "Apache 2.0";
	private static final String title = "WarmPie REST API";
	private static final String description = "WarmnPie is a RESTful API that provides access to the ezPie component.";
	
	private ApiInfo apiInfo() {
		return new ApiInfoBuilder()
				.title(title)
				.description(description)
				.license(LICENSE_TEXT)
				.version(SWAGGER_API_VERSION)
				.build();
	}
	
	@Bean
	public Docket warmpieApi() {
		return new Docket(DocumentationType.SWAGGER_2)
				.apiInfo(apiInfo())
				.pathMapping("/")
				.select()
				.paths(PathSelectors.regex("/api.*"))
				.build();

	}

}