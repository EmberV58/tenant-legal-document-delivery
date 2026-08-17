package dev.infrai.legaltech.workflow;

import dev.infrai.legaltech.storage.StoragePort;
import java.time.Instant;

public final class SignedDocumentDeliveryTest {
    public static void main(String[] args) {
        missingSignatureSchedulesFollowUpBeforeDeadline();
        presentSignatureProducesScopedDownload();
        System.out.println("SignedDocumentDeliveryTest: PASS");
    }

    private static void missingSignatureSchedulesFollowUpBeforeDeadline() {
        FakeStorage storage = new FakeStorage(false);
        SignedDocumentDelivery service = service(storage);
        MatterIntake intake = new MatterIntake("North Bank", "M-1042", "settlement", Instant.parse("2026-09-18T12:00:00Z"));
        DeliveryDecision result = service.assess(intake);
        check(result.status() == DeliveryDecision.Status.AWAITING_SIGNATURE, "missing signature status");
        check(result.followUpAt().equals(Instant.parse("2026-09-16T12:00:00Z")), "follow-up date");
        check(storage.bucketCreated, "tenant bucket created before object check");
        check(result.objectKey().equals("matters/m-1042/signed/settlement.pdf"), "matter-scoped key");
    }

    private static void presentSignatureProducesScopedDownload() {
        FakeStorage storage = new FakeStorage(true);
        DeliveryDecision result = service(storage).assess(new MatterIntake(
                "North Bank", "M-1042", "settlement", Instant.parse("2026-09-18T12:00:00Z")));
        check(result.status() == DeliveryDecision.Status.READY_FOR_DELIVERY, "ready status");
        check(result.downloadUrl().equals("https://signed.example/document"), "signed download");
        check(storage.expiresSeconds == 300, "short expiry");
    }

    private static SignedDocumentDelivery service(StoragePort storage) {
        return new SignedDocumentDelivery(storage);
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }

    private static final class FakeStorage implements StoragePort {
        private final boolean found;
        private boolean bucketCreated;
        private int expiresSeconds;

        FakeStorage(boolean found) { this.found = found; }
        public void createBucket(String name) { bucketCreated = true; }
        public boolean objectExists(String bucket, String key) {
            check(bucketCreated, "bucket ordering");
            return found;
        }
        public String presignDownload(String bucket, String key, int expires, String disposition) {
            expiresSeconds = expires;
            return "https://signed.example/document";
        }
    }
}
