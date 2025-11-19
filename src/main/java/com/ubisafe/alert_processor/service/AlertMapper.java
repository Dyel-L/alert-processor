package com.ubisafe.alert_processor.service;

import com.ubisafe.alert_processor.domain.Alert;
import com.ubisafe.alert_processor.domain.AlertEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Mapper responsável por converter entre o modelo de domínio {@link Alert}
 * e a entidade de persistência {@link AlertEntity}.
 * -
 * Toda a lógica de construção da entidade fica centralizada aqui,
 * evitando duplicação e facilitando mudanças futuras.
 */
@Component
public class AlertMapper {

    /**
     * Constrói uma entidade {@link AlertEntity} em estado de sucesso
     * a partir de um {@link Alert} já validado.
     */
    public AlertEntity toSuccessEntity(Alert alert) {
        AlertEntity entity = mapAlertToEntity(alert);
        entity.markSuccess(LocalDateTime.now());
        return entity;
    }

    /**
     * Constrói uma entidade {@link AlertEntity} em estado de falha
     * a partir de um {@link Alert} ou de dados mínimos disponíveis.
     */
    public AlertEntity toFailureEntityFromAlert(Alert alert, String failureReason) {
        AlertEntity entity = mapAlertToEntity(alert);
        entity.markFailure(LocalDateTime.now(), failureReason);
        return entity;
    }

    /**
     * Método auxiliar que mapeia os campos comuns de {@link Alert} para {@link AlertEntity}.
     */
    private AlertEntity mapAlertToEntity(Alert alert) {
        AlertEntity entity = new AlertEntity();
        entity.setId(alert.getId());
        entity.setClientId(alert.getClientId());
        entity.setAlertType(alert.getAlertType());
        entity.setMessage(alert.getMessage());
        entity.setSeverity(alert.getSeverity());
        entity.setSource(alert.getSource());
        entity.setTimestamp(alert.getTimestamp());
        return entity;
    }

    /**
     * Constrói uma entidade de falha quando não é possível desserializar o JSON
     * em um {@link Alert}. Neste caso usamos apenas o payload bruto.
     */
    public AlertEntity toFailureEntityFromRawPayload(String rawPayload, String failureReason) {
        AlertEntity entity = new AlertEntity();
        entity.setId(null);
        entity.setClientId(null);
        entity.setAlertType("UNKNOWN");
        entity.setMessage(rawPayload);
        entity.setSeverity(null);
        entity.setSource(null);
        entity.setTimestamp(LocalDateTime.now());
        entity.markFailure(LocalDateTime.now(), failureReason);
        return entity;
    }
}
