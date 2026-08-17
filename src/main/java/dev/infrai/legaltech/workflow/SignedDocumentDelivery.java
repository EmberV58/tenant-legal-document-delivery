package dev.infrai.legaltech.workflow;

import dev.infrai.legaltech.storage.StoragePort;
import java.time.Duration;
import java.util.Locale;

public final class SignedDocumentDelivery {
    private final StoragePort storage;

    public SignedDocumentDelivery(StoragePort storage) {
        this.storage = storage;
    }

    public DeliveryDecision assess(MatterIntake intake) {
        String bucket = tenantBucket(intake.tenantId());
        String key = "matters/" + safe(intake.matterId()) + "/signed/" + safe(intake.documentId()) + ".pdf";
        storage.createBucket(bucket);
        if (!storage.objectExists(bucket, key)) {
            return new DeliveryDecision(DeliveryDecision.Status.AWAITING_SIGNATURE, bucket, key,
                    null, intake.deadline().minus(Duration.ofDays(2)));
        }
        String url = storage.presignDownload(bucket, key, 300,
                "attachment; filename=\"signed-" + safe(intake.documentId()) + ".pdf\"");
        return new DeliveryDecision(DeliveryDecision.Status.READY_FOR_DELIVERY, bucket, key,
                url, null);
    }

    static String tenantBucket(String tenantId) {
        String normalized = safe(tenantId);
        return "legal-tenant-" + Integer.toUnsignedString(tenantId.hashCode(), 36) + "-" + normalized;
    }

    private static String safe(String value) {
        String normalized = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]", "-")
                .replaceAll("-+", "-").replaceAll("^-|-$", "");
        if (normalized.isBlank()) throw new IllegalArgumentException("Identifier has no storage-safe characters");
        return normalized;
    }
}
