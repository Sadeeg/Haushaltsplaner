package com.haushaltsplaner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HaushaltsplanerApplication {

    public static void main(String[] args) {
        SpringApplication.run(HaushaltsplanerApplication.class, args);
    }
}
