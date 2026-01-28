package com.app.signflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
@EnableAutoConfiguration
public class MyApplication extends SpringBootServletInitializer{

	
	@Override
	protected SpringApplicationBuilder configure (SpringApplicationBuilder myAppBuilder) {
		return myAppBuilder.sources(MyApplication.class);
	}
	
	public static void main (String [] args) {
		SpringApplication.run(MyApplication.class, args);
	}
	

}
