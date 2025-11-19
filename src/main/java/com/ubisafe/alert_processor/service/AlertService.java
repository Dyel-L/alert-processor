package com.ubisafe.alert_processor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ubisafe.alert_processor.domain.Alert;
import com.ubisafe.alert_processor.domain.AlertEntity;
import com.ubisafe.alert_processor.exception.AlertProcessingException;
import com.ubisafe.alert_processor.exception.InvalidAlertJsonException;
import com.ubisafe.alert_processor.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço de aplicação responsável pelo fluxo principal de processamento
 * e persistência de alertas com sucesso.
 * -
 * Não conhece detalhes de transporte (Kafka) e pode ser reutilizado
 * por outros adaptadores (ex.: REST, filas diferentes, etc.).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final AlertMapper alertMapper;
    private final ObjectMapper objectMapper;

    /**
     * Processa e persiste um alerta em fluxo de sucesso.
     *
     * @param alertJson payload JSON recebido
     * @return entidade persistida
     */
    @Transactional
    public AlertEntity processAlert(String alertJson) {
        try {
            Alert alert = objectMapper.readValue(alertJson, Alert.class);

            if (alertRepository.existsById(alert.getId())) {
                log.warn("Alert {} já processado anteriormente. Ignorando duplicata.", alert.getId());
                return alertRepository.findById(alert.getId()).orElseThrow();
            }

            AlertEntity entity = alertMapper.toSuccessEntity(alert);
            return alertRepository.save(entity);

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.warn("Erro de desserialização de Alert. Payload inválido.");
            throw new InvalidAlertJsonException("Invalid alert JSON", e);
        } catch (DataAccessException e) {
            log.error("Erro técnico ao salvar alerta no banco.", e);
            throw new AlertProcessingException("Technical error when saving alert", e);
        } catch (Exception e) {
            log.error("Erro inesperado ao processar alerta.", e);
            throw new AlertProcessingException("Unexpected error when processing alert", e);
        }
    }
}

