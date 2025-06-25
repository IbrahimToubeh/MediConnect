package com.MediConnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(scanBasePackages = "com.MediConnect")

public class MediConnectApplication {

	public static void main(String[] args) {
		SpringApplication.run(MediConnectApplication.class, args);
	}

}
