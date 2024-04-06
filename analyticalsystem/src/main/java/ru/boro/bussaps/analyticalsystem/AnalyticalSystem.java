package ru.boro.bussaps.analyticalsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AnalyticalSystem

{
    public static void main(String[] args) {
        System.setProperty("user.timezone", "Europe/Moscow");
        SpringApplication.run(AnalyticalSystem.class, args);
    }


}
