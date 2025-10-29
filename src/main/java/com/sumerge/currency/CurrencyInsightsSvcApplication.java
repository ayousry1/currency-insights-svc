package com.sumerge.currency;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class CurrencyInsightsSvcApplication {
    public static void main(String[] args) {
        SpringApplication.run(CurrencyInsightsSvcApplication.class, args);
    }
}