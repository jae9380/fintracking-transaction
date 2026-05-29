package com.ft.transaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class FintrackingTransactionApplication {

    public static void main(String[] args) {
        SpringApplication.run(FintrackingTransactionApplication.class, args);
    }

}
