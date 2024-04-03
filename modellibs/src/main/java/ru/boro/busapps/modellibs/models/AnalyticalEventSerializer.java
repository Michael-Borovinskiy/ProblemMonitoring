package ru.boro.busapps.modellibs.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

public class AnalyticalEventSerializer implements Serializer<AnalyticalEvent> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public byte[] serialize(String topic, AnalyticalEvent data) {
        try {
            if (data == null){
                System.out.println("Null received at serializing");
                return null;
            }
            return objectMapper.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new SerializationException("Error when serializing AnalyticalEvent to byte[]");
        }
    }
}