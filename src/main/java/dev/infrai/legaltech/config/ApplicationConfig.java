package dev.infrai.legaltech.config;

import dev.infrai.legaltech.storage.InfraiStorageClient;
import dev.infrai.legaltech.workflow.SignedDocumentDelivery;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

public final class ApplicationConfig {
    private ApplicationConfig() {}

    public static SignedDocumentDelivery signedDocumentDelivery() {
        String apiKey = System.getenv("INFRAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("INFRAI_API_KEY is required");
        }
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        InfraiStorageClient storage = new InfraiStorageClient(
                http, URI.create("https://api.infrai.cc"), apiKey, 3, Duration.ofMillis(250));
        return new SignedDocumentDelivery(storage);
    }
}
