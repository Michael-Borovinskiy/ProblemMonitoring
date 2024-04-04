package ru.boro.busapps.problmonitoring.service.consumer;

import lombok.NoArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Service;
import ru.boro.busapps.modellibs.models.AnalyticalEvent;
import ru.boro.busapps.modellibs.models.BusinessEvent;

import java.util.Date;

@Service
@NoArgsConstructor
public class ProblMonitoringService implements KfkEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProblMonitoringService.class);

    @SendTo("${topicAUTO}")
    @KafkaListener(
            topics = "${topicMonitoring}",
            groupId = "monitoringserv-group"
    )
    @Override
    public AnalyticalEvent approve(BusinessEvent event) {
        LOGGER.info("message: {} received", event.toString());

        return new AnalyticalEvent(event.getTypeEvent(), event.getKindEvent(), event.getIdClient(),
                event.getDateCreate(), getEmployee(), new Date());
    }

    @Override
    public String getEmployee() {
        var idEmployee = RandomStringUtils.randomNumeric(2);

        return "employee " + idEmployee;
    }


}
