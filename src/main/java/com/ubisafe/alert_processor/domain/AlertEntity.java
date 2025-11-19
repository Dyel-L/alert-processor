package com.ubisafe.alert_processor.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertEntity {

    @Id
    @Setter
    private String id;

    @Column(nullable = false)
    @Setter
    private String clientId;

    @Column(nullable = false)
    @Setter
    private String alertType;

    @Column(nullable = false, length = 1000)
    @Setter
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Setter
    private Severity severity;

    @Setter
    private String source;

    @Column(nullable = false)
    @Setter
    private LocalDateTime timestamp;

    @Setter(AccessLevel.PRIVATE)
    @Column(nullable = false)
    private LocalDateTime processedAt;

    @Setter(AccessLevel.PRIVATE)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessingStatus processingStatus;

    @Setter(AccessLevel.PRIVATE)
    private String failureReason;

    /**
     * Marca o alerta como processado com sucesso.
     */
    public void markSuccess(LocalDateTime processedAt) {
        this.processedAt = processedAt;
        this.processingStatus = ProcessingStatus.SUCCESS;
        this.failureReason = null;
    }

    /**
     * Marca o alerta como processado com falha.
     */
    public void markFailure(LocalDateTime processedAt, String failureReason) {
        this.processedAt = processedAt;
        this.processingStatus = ProcessingStatus.FAILURE;
        this.failureReason = failureReason;
    }
}

