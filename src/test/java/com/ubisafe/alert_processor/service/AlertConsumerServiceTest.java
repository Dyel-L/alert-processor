package com.ubisafe.alert_processor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ubisafe.alert_processor.domain.Alert;
import com.ubisafe.alert_processor.domain.AlertEntity;
import com.ubisafe.alert_processor.exception.AlertProcessingException;
import com.ubisafe.alert_processor.exception.InvalidAlertJsonException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static com.ubisafe.alert_processor.domain.Severity.HIGH;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertConsumerServiceTest {

    @Mock
    private AlertService alertService;

    @Mock
    private AlertFailureService alertFailureService;

    @InjectMocks
    private AlertConsumerService alertConsumerService;

    private String testAlertJson;

    @BeforeEach
    void setUp() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        Alert alert = new Alert(
                "test-id-123",
                "client-id-123",
                "SYSTEM",
                "Test alert message",
                HIGH,
                "test-source",
                LocalDateTime.now()
        );

        testAlertJson = objectMapper.writeValueAsString(alert);
    }

    @Test
    void consumeAlert_ShouldSaveAlert_WhenValidJson() {
        when(alertService.processAlert(testAlertJson)).thenReturn(buildSuccessEntity());

        alertConsumerService.consumeAlert(testAlertJson);

        verify(alertService, times(1)).processAlert(testAlertJson);
        verify(alertFailureService, never()).registerFailureFromAlertJson(any(), any());
        verify(alertFailureService, never()).registerFailureFromRawPayload(any(), any());
    }

    @Test
    void consumeAlert_ShouldThrowException_WhenInvalidJson() {
        when(alertService.processAlert(any()))
                .thenThrow(new InvalidAlertJsonException("Invalid alert JSON"));

        assertThrows(InvalidAlertJsonException.class, () -> alertConsumerService.consumeAlert("{ invalid json }"));

        verify(alertFailureService, times(1))
                .registerFailureFromRawPayload(eq("{ invalid json }"), contains("Invalid alert JSON"));
    }

    @Test
    void consumeAlert_ShouldSimulateDelay() {
        when(alertService.processAlert(testAlertJson)).thenReturn(buildSuccessEntity());
        long startTime = System.currentTimeMillis();

        alertConsumerService.consumeAlert(testAlertJson);
        long endTime = System.currentTimeMillis();

        long duration = endTime - startTime;
        assertTrue(duration >= 500, "Processing should take at least 500ms");
        verify(alertService, times(1)).processAlert(testAlertJson);
    }

    @Test
    void consumeAlert_ShouldPersistFailedAlert_WhenProcessingFails() {
        when(alertService.processAlert(testAlertJson))
                .thenThrow(new AlertProcessingException("Technical error when saving alert"));

        assertThrows(AlertProcessingException.class,
                () -> alertConsumerService.consumeAlert(testAlertJson));

        verify(alertFailureService, times(1))
                .registerFailureFromAlertJson(eq(testAlertJson), contains("Technical error when saving alert"));
    }

    @Test
    void consumeAlert_ShouldSetFailureStatus_WhenExceptionOccurs() {
        when(alertService.processAlert(testAlertJson))
                .thenThrow(new AlertProcessingException("Failed to process alert"));

        assertThrows(AlertProcessingException.class,
                () -> alertConsumerService.consumeAlert(testAlertJson));

        verify(alertFailureService, times(1))
                .registerFailureFromAlertJson(eq(testAlertJson), contains("Failed to process alert"));
    }

    @Test
    void consumeAlert_ShouldHandleInterruption_Correctly() {
        Thread.currentThread().interrupt();

        assertThrows(AlertProcessingException.class,
                () -> alertConsumerService.consumeAlert(testAlertJson));

        verify(alertFailureService, atLeastOnce()).registerFailureFromAlertJson(eq(testAlertJson), contains("interrupted"));
        assertTrue(Thread.interrupted());
    }

    private AlertEntity buildSuccessEntity() {
        AlertEntity entity = new AlertEntity();
        entity.setId("test-id-123");
        entity.setClientId("client-id-123");
        entity.setAlertType("SYSTEM");
        entity.setMessage("Test alert message");
        entity.setSeverity(HIGH);
        entity.setSource("test-source");
        entity.setTimestamp(LocalDateTime.now());
        entity.markSuccess(LocalDateTime.now());
        return entity;
    }
}
