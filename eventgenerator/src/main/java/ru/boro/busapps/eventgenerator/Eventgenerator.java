package ru.boro.busapps.eventgenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class Eventgenerator {

    private Logger logger = LoggerFactory.getLogger(Eventgenerator.class);


    @Value("${topicAUTO}")
    String topicAUTO;
    @Value("${topicMonitoring}")
    String topicMonitoring;

    public static void main(String[] args)  {
        System.setProperty("user.timezone", "Europe/Moscow");
        SpringApplication.run(Eventgenerator.class, args);
    }

}
