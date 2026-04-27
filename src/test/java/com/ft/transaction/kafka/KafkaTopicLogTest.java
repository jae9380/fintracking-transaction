package com.ft.transaction.kafka;

import com.ft.common.event.TransactionCreatedEvent;
import com.ft.common.kafka.KafkaTopic;
import com.ft.transaction.application.TransactionEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * Kafka 토픽 발행 로그 테스트 — transaction-service
 *
 * 실제 Kafka 브로커 없이 KafkaTemplate을 Mock 처리하고,
 * ArgumentCaptor로 실제 발행될 토픽명과 페이로드를 캡처하여 콘솔에 출력한다.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
@DisplayName("Kafka 토픽 발행 로그 테스트 - transaction-service")
class KafkaTopicLogTest {

    @Mock
    KafkaTemplate<String, Object> kafkaTemplate;

    TransactionEventPublisher publisher;

    @BeforeEach
    void setUp() {
        given(kafkaTemplate.send(anyString(), any())).willReturn(null);
        publisher = new TransactionEventPublisher(kafkaTemplate);
    }

    @Test
    @DisplayName("성공 - 수입 이벤트 발행 시 transaction.created 토픽으로 전송된다")
    void publish_whenIncome_sendsToTransactionCreatedTopic() {
        // given
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                "evt-001", 1L, 10L, null, 100L,
                new BigDecimal("50000"), "INCOME", 5L, "급여", LocalDateTime.now()
        );

        // when
        publisher.publish(event);

        // then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        then(kafkaTemplate).should().send(topicCaptor.capture(), payloadCaptor.capture());

        TransactionCreatedEvent captured = (TransactionCreatedEvent) payloadCaptor.getValue();

        log.info("┌─────────────────────────────────────────────────");
        log.info("│ [KAFKA-LOG] transaction-service → PUBLISH");
        log.info("│ topic        : {}", topicCaptor.getValue());
        log.info("│ type         : {}", captured.type());
        log.info("│ amount       : {}", captured.amount());
        log.info("│ categoryName : {}", captured.categoryName());
        log.info("└─────────────────────────────────────────────────");

        assertThat(topicCaptor.getValue()).isEqualTo(KafkaTopic.TRANSACTION_CREATED);
        assertThat(captured.type()).isEqualTo("INCOME");
    }

    @Test
    @DisplayName("성공 - 지출 이벤트 발행 시 transaction.created 토픽으로 전송된다")
    void publish_whenExpense_sendsToTransactionCreatedTopic() {
        // given
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                "evt-002", 1L, 10L, null, 101L,
                new BigDecimal("12000"), "EXPENSE", 3L, "식비", LocalDateTime.now()
        );

        // when
        publisher.publish(event);

        // then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        then(kafkaTemplate).should().send(topicCaptor.capture(), payloadCaptor.capture());

        TransactionCreatedEvent captured = (TransactionCreatedEvent) payloadCaptor.getValue();

        log.info("┌─────────────────────────────────────────────────");
        log.info("│ [KAFKA-LOG] transaction-service → PUBLISH");
        log.info("│ topic        : {}", topicCaptor.getValue());
        log.info("│ type         : {}", captured.type());
        log.info("│ amount       : {}", captured.amount());
        log.info("│ categoryName : {}", captured.categoryName());
        log.info("└─────────────────────────────────────────────────");

        assertThat(topicCaptor.getValue()).isEqualTo(KafkaTopic.TRANSACTION_CREATED);
        assertThat(captured.type()).isEqualTo("EXPENSE");
    }

    @Test
    @DisplayName("성공 - 이체 이벤트 발행 시 transaction.created 토픽으로 전송된다")
    void publish_whenTransfer_sendsToTransactionCreatedTopic() {
        // given
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                "evt-003", 1L, 10L, 20L, 102L,
                new BigDecimal("30000"), "TRANSFER", null, null, LocalDateTime.now()
        );

        // when
        publisher.publish(event);

        // then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        then(kafkaTemplate).should().send(topicCaptor.capture(), payloadCaptor.capture());

        TransactionCreatedEvent captured = (TransactionCreatedEvent) payloadCaptor.getValue();

        log.info("┌─────────────────────────────────────────────────");
        log.info("│ [KAFKA-LOG] transaction-service → PUBLISH");
        log.info("│ topic        : {}", topicCaptor.getValue());
        log.info("│ type         : {}", captured.type());
        log.info("│ accountId    : {} (출금)", captured.accountId());
        log.info("│ toAccountId  : {} (입금)", captured.toAccountId());
        log.info("└─────────────────────────────────────────────────");

        assertThat(topicCaptor.getValue()).isEqualTo(KafkaTopic.TRANSACTION_CREATED);
        assertThat(captured.type()).isEqualTo("TRANSFER");
    }
}
