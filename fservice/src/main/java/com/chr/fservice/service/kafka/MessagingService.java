package com.chr.fservice.service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author RAY
 * @descriptions
 * @since 2020/11/10
 */
@Component
public class MessagingService {
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    /*public void sendRegistrationMessage(RegistrationMessage msg) throws IOException {
        send("topic_registration", msg);
    }

    public void sendLoginMessage(LoginMessage msg) throws IOException {
        send("topic_login", msg);
    }*/

    public void send(String topic, Object msg) throws IOException {
        ProducerRecord<String, String> pr = new ProducerRecord<>(topic, objectMapper.writeValueAsString(msg));
        //pr.headers().add("type", msg.getClass().getName().getBytes(StandardCharsets.UTF_8));
        kafkaTemplate.send(pr);
    }
}