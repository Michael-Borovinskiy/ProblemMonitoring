package ru.boro.busapps.problmonitoring.kafkaconfigs;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import ru.boro.busapps.modellibs.models.*;


import java.util.HashMap;
import java.util.Map;

@Configuration
public class KfkConfig {

    @Bean
    public ProducerFactory<Long, AnalyticalEvent> producerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    @Bean
    public Map<String, Object> producerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092,localhost:9093,localhost:9094");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "ru.boro.busapps.modellibs.models.AnalyticalEventSerializer");
        return props;
    }

    @Bean
    public KafkaTemplate<Long, AnalyticalEvent> kafkaTemplate() {
        return new KafkaTemplate<Long, AnalyticalEvent>(producerFactory());
    }

    @Bean
    public Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092,localhost:9093,localhost:9094");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "ru.boro.busapps.modellibs.models.BusinessEventDeserializer");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "monitoringserv-group");
        return props;
    }

    @Bean
    public ConsumerFactory<Long, BusinessEvent> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerConfigs());
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<Long, BusinessEvent>> kafkaListenerContainerFactory(KafkaTemplate<Long, AnalyticalEvent> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<Long, BusinessEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setReplyTemplate(kafkaTemplate);
        return factory;
    }

}
