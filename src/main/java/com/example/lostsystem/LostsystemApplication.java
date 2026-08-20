package com.example.lostsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class LostsystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(LostsystemApplication.class, args);
    }

}
