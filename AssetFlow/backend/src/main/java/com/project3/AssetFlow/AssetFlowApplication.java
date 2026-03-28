package com.project3.AssetFlow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AssetFlowApplication {

	public static void main(String[] args) {
		SpringApplication.run(AssetFlowApplication.class, args);
	}

}
