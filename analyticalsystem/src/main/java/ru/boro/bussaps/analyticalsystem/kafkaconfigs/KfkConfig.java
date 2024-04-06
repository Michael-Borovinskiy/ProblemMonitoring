package ru.boro.bussaps.analyticalsystem.kafkaconfigs;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import ru.boro.busapps.modellibs.models.AnalyticalEvent;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KfkConfig {

    @Bean
    public Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092,localhost:9093,localhost:9094");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "ru.boro.busapps.modellibs.models.AnalyticalEventDeserializer");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "batch-analytical-group-1");
        return props;
    }

    @Bean
    public ConsumerFactory<Long, AnalyticalEvent> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerConfigs());
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<Long, AnalyticalEvent>> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<Long, AnalyticalEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setBatchListener(true);
        factory.getContainerProperties().setIdleBetweenPolls(20000);
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);

        return factory;
    }
}
