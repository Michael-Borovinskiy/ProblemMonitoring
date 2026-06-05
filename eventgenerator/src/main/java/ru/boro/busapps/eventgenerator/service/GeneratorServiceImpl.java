package ru.boro.busapps.eventgenerator.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.boro.busapps.modellibs.enums.*;
import ru.boro.busapps.modellibs.models.*;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class GeneratorServiceImpl implements GeneratorService {

    @Value("${topicMonitoring}")
    private String topic;
    @Value("${topicAUTO}")
    private String topicAUTO;

    private final KafkaTemplate<Long, Object> kafkaTemplate;

    @Scheduled(fixedDelay = 1000)
    @Override
    public void send() {

        var eventType = TypeEvent.randomType().toString();

        if(eventType.equals("AUTO")) {
            kafkaTemplate.send(topicAUTO, new AnalyticalEvent(eventType, KindAutoType.randomType()
                    , RandomStringUtils.randomNumeric(2), new Date(), "None", new Date()));
        }
        else {
            kafkaTemplate.send(topic, new BusinessEvent(eventType, KindVerificatedType.randomType(),
                    RandomStringUtils.randomNumeric(2), new Date()));
        }
    }



}
