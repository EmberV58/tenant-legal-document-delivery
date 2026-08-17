package dev.infrai.legaltech.storage;

public interface StoragePort {
    void createBucket(String name);
    boolean objectExists(String bucket, String key);
    String presignDownload(String bucket, String key, int expiresSeconds, String disposition);
}
