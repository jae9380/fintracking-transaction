package com.ft.transaction.application;

import com.ft.common.event.TransactionCreatedEvent;
import com.ft.common.kafka.AbstractEventPublisher;
import com.ft.common.kafka.KafkaTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TransactionEventPublisher extends AbstractEventPublisher<TransactionCreatedEvent> {

    public TransactionEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        super(kafkaTemplate);
    }

    @Override
    public String topic() {
        return KafkaTopic.TRANSACTION_CREATED;
    }
}
