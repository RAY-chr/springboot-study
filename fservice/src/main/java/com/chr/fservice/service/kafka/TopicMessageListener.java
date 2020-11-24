package com.chr.fservice.service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;

/**
 * @author RAY
 * @descriptions
 * @since 2020/11/10
 */
//@Component
public class TopicMessageListener {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(topics = "topic_registration", groupId = "group1")
    public void onRegistrationMessage(@Payload String message/*, @Header("type") String type*/) throws Exception {
        logger.info("received message: {}", message);
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> getType(String type) {
        // TODO: use cache:
        try {
            return (Class<T>) Class.forName(type);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
