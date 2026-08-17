package dev.infrai.legaltech.workflow;

import java.time.Instant;

public record DeliveryDecision(Status status, String bucket, String objectKey,
                               String downloadUrl, Instant followUpAt) {
    public enum Status { READY_FOR_DELIVERY, AWAITING_SIGNATURE }
}
