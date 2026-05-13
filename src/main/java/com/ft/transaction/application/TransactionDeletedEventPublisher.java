package com.ft.transaction.application;

import com.ft.common.event.TransactionDeletedEvent;
import com.ft.common.kafka.AbstractEventPublisher;
import com.ft.common.kafka.KafkaTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TransactionDeletedEventPublisher extends AbstractEventPublisher<TransactionDeletedEvent> {

    public TransactionDeletedEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        super(kafkaTemplate);
    }

    @Override
    public String topic() {
        return KafkaTopic.TRANSACTION_DELETED;
    }
}
