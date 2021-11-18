package com.tencent.scfspringbootjava8;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties
public class ScfSpringbootJava8Application {

	public static void main(String[] args) {
		SpringApplication.run(ScfSpringbootJava8Application.class, args);
	}
}
