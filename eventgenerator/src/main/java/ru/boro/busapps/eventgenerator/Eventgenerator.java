package ru.boro.busapps.eventgenerator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class Eventgenerator {

    public static void main(String[] args)  {
        System.setProperty("user.timezone", "Europe/Moscow");
        SpringApplication.run(Eventgenerator.class, args);
    }

}
