package com.MediConnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;


@SpringBootApplication(scanBasePackages = "com.MediConnect")
@EnableCaching

public class MediConnectApplication {

    public static void main(String[] args) {
        SpringApplication.run(MediConnectApplication.class, args);
    }

}
