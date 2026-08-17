package dev.infrai.legaltech.storage;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public final class InfraiStorageClient implements StoragePort {
    private final HttpClient http;
    private final URI baseUri;
    private final String apiKey;
    private final int maxAttempts;
    private final Duration initialBackoff;

    public InfraiStorageClient(HttpClient http, URI baseUri, String apiKey,
                               int maxAttempts, Duration initialBackoff) {
        this.http = http;
        this.baseUri = baseUri;
        this.apiKey = apiKey;
        this.maxAttempts = maxAttempts;
        this.initialBackoff = initialBackoff;
    }

    @Override
    public void createBucket(String name) {
        call("POST", "/v1/storage/bucket/create", JsonCodec.object(Map.of("name", name)));
    }

    @Override
    public boolean objectExists(String bucket, String key) {
        Map<String, Object> data = call("GET", "/v1/storage/object/head/" + segment(bucket) + "/" + segment(key), null);
        return Boolean.TRUE.equals(data.get("found"));
    }

    @Override
    public String presignDownload(String bucket, String key, int expiresSeconds, String disposition) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("op", "get");
        body.put("expires_seconds", expiresSeconds);
        body.put("response_disposition", disposition);
        body.put("idempotency_key", "signed-download-" + Integer.toUnsignedString((bucket + "/" + key).hashCode(), 36));
        Map<String, Object> data = call("POST", "/v1/storage/object/presign/" + segment(bucket) + "/" + segment(key), JsonCodec.object(body));
        return requiredString(data, "url");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> call(String method, String path, String body) {
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            HttpRequest.BodyPublisher publisher = body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body);
            HttpRequest request = HttpRequest.newBuilder(baseUri.resolve(path))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .method(method, publisher)
                    .build();
            try {
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                Map<String, Object> envelope = JsonCodec.parseObject(response.body());
                if (!Boolean.TRUE.equals(envelope.get("ok"))) {
                    Map<String, Object> error = envelope.get("error") instanceof Map<?, ?> value
                            ? (Map<String, Object>) value : Map.of();
                    String code = String.valueOf(error.getOrDefault("code", "REQUEST_REJECTED"));
                    String message = String.valueOf(error.getOrDefault("message", code));
                    if (response.statusCode() == 429 && attempt + 1 < maxAttempts) {
                        pause(retryDelay(response, attempt));
                        continue;
                    }
                    throw new InfraiException(code, message, response.statusCode());
                }
                if (envelope.get("data") instanceof Map<?, ?> data) return (Map<String, Object>) data;
                return Map.of();
            } catch (IOException e) {
                throw new IllegalStateException("Storage transport failed", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Storage request interrupted", e);
            }
        }
        throw new IllegalStateException("Retry attempts exhausted");
    }

    private Duration retryDelay(HttpResponse<?> response, int attempt) {
        return response.headers().firstValue("Retry-After")
                .map(value -> Duration.ofSeconds(Long.parseLong(value)))
                .orElse(initialBackoff.multipliedBy(1L << attempt));
    }

    private static void pause(Duration duration) throws InterruptedException {
        Thread.sleep(duration.toMillis());
    }

    private static String segment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String requiredString(Map<String, Object> values, String field) {
        Object value = values.get(field);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException("Response is missing " + field);
        }
        return text;
    }
}
