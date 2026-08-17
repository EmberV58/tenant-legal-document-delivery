package dev.infrai.legaltech.workflow;

import java.time.Instant;

public record MatterIntake(String tenantId, String matterId, String documentId, Instant deadline) {
    public MatterIntake {
        if (tenantId.isBlank() || matterId.isBlank() || documentId.isBlank()) {
            throw new IllegalArgumentException("Tenant, matter, and document identifiers are required");
        }
    }
}
