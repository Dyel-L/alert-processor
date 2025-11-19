package com.ubisafe.alert_processor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ubisafe.alert_processor.domain.Alert;
import com.ubisafe.alert_processor.domain.AlertEntity;
import com.ubisafe.alert_processor.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço dedicado a registrar falhas de processamento de alertas
 * em uma nova transação independente do fluxo principal.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertFailureService {

    private final AlertRepository alertRepository;
    private final AlertMapper alertMapper;
    private final ObjectMapper objectMapper;

    /**
     * Registra uma falha de processamento a partir de um payload JSON que
     * conseguiu ser desserializado anteriormente.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerFailureFromAlertJson(String alertJson, String failureReason) {
        try {
            Alert alert = objectMapper.readValue(alertJson, Alert.class);
            AlertEntity failedEntity = alertMapper.toFailureEntityFromAlert(alert, failureReason);
            alertRepository.save(failedEntity);
            log.info("Failed alert saved (from valid JSON): reason={}", failureReason);
        } catch (com.fasterxml.jackson.core.JsonProcessingException jsonEx) {
            log.warn("Falha ao desserializar JSON para registrar erro; registrando apenas payload bruto.", jsonEx);
            registerFailureFromRawPayload(alertJson, failureReason + " (and JSON deserialization failed for failure logging)");
        } catch (Exception e) {
            log.error("Erro ao registrar falha de alerta (from valid JSON).", e);
        }
    }

    /**
     * Registra falha quando o JSON é inválido e não foi possível montar um Alert.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerFailureFromRawPayload(String rawPayload, String failureReason) {
        try {
            AlertEntity failedEntity = alertMapper.toFailureEntityFromRawPayload(rawPayload, failureReason);
            alertRepository.save(failedEntity);
            log.info("Failed alert saved (from raw payload): reason={}", failureReason);
        } catch (Exception e) {
            log.error("Erro ao registrar falha de alerta a partir de payload bruto.", e);
        }
    }
}
