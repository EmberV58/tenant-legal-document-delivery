package dev.infrai.legaltech;

import dev.infrai.legaltech.config.ApplicationConfig;
import dev.infrai.legaltech.workflow.DeliveryDecision;
import dev.infrai.legaltech.workflow.MatterIntake;
import java.time.Instant;

public final class SignedDeliveryApplication {
    private SignedDeliveryApplication() {}

    public static void main(String[] args) {
        if (args.length != 4) {
            System.err.println("Usage: signed-delivery <tenant> <matter> <document> <deadline-instant>");
            System.exit(2);
        }
        MatterIntake intake = new MatterIntake(args[0], args[1], args[2], Instant.parse(args[3]));
        DeliveryDecision result = ApplicationConfig.signedDocumentDelivery().assess(intake);
        System.out.printf("status=%s%n bucket=%s%n objectKey=%s%n followUpAt=%s%n",
                result.status(), result.bucket(), result.objectKey(), result.followUpAt());
        if (result.downloadUrl() != null) System.out.println("downloadUrl=" + result.downloadUrl());
    }
}
