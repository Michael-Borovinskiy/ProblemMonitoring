package ru.boro.bussaps.analyticalsystem.service;

import lombok.AllArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import ru.boro.bussaps.analyticalsystem.entity.AnalyticalEvent;
import ru.boro.bussaps.analyticalsystem.repo.AnalyticalEventRepository;

import java.util.Date;
import java.util.List;

@Service
@AllArgsConstructor
public class AnalyticalService {

    private final AnalyticalEventRepository analyticalEventRepository;

    //@Transactional
    @KafkaListener(topics = "${topicAUTO}",
            groupId = "batch-analytical-group-1"
    )
    public void saveBatchMsg(@Payload List<ru.boro.busapps.modellibs.models.AnalyticalEvent> events
    ) {
        var load_datetime = new Date();

        for (int i = 0; i < events.size(); i++) {
            saveOrUpdate(
                    new AnalyticalEvent(
                            events.get(i).getTypeEvent(),
                            events.get(i).getKindEvent(),
                            events.get(i).getIdClient(),
                            events.get(i).getDateCreate(),
                            events.get(i).getApprovedBy(),
                            events.get(i).getApprovedDateTime(),
                            load_datetime
                    )
            );
        }
    }

    public void saveOrUpdate(AnalyticalEvent analyticalEvent) {
        analyticalEventRepository.save(analyticalEvent);
    }

}