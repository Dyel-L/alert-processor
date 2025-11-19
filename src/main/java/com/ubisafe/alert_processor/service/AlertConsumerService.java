package com.ubisafe.alert_processor.service;

import com.ubisafe.alert_processor.exception.AlertProcessingException;
import com.ubisafe.alert_processor.exception.InvalidAlertJsonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertConsumerService {

    private static final long PROCESSING_DELAY_MS = 500;

    private final AlertService alertService;
    private final AlertFailureService alertFailureService;

    /**
     * Consumer Kafka responsável apenas por orquestrar o fluxo de processamento.
     * A lógica de negócio e de persistência fica em serviços dedicados.
     * -
     * A transação é gerenciada pelos serviços de aplicação, não pelo listener.
     * O commit de offset do Kafka ocorre apenas quando o método retorna sem exceção.
     */
    @KafkaListener(topics = "alerts", groupId = "processor-group")
    public void consumeAlert(String alertJson) {
        log.info("Received alert from Kafka: {}", alertJson);

        try {
            Thread.sleep(PROCESSING_DELAY_MS);

            alertService.processAlert(alertJson);

            log.info("Alert processed and saved successfully.");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String reason = "Alert processing was interrupted: " + e.getMessage();
            log.error(reason, e);
            alertFailureService.registerFailureFromAlertJson(alertJson, reason);
            throw new AlertProcessingException("Alert processing was interrupted", e);

        } catch (InvalidAlertJsonException e) {
            String reason = e.getMessage();
            log.error("Invalid JSON error: {}", reason, e);
            alertFailureService.registerFailureFromRawPayload(alertJson, reason);
            throw e;

        } catch (AlertProcessingException e) {
            String reason = e.getMessage();
            log.error("Business/technical processing error: {}", reason, e);
            alertFailureService.registerFailureFromAlertJson(alertJson, reason);
            throw e;

        } catch (Exception e) {
            String reason = "Failed to process alert: " + e.getMessage();
            log.error(reason, e);
            alertFailureService.registerFailureFromAlertJson(alertJson, reason);
            throw new AlertProcessingException("Failed to process alert", e);
        }
    }
}
