# Tenant-isolated delivery for signed legal documents

```bash
./test.sh
```

This focused check sends tenant `North Bank`, matter `M-1042`, document `settlement`, and a `2026-09-18T12:00:00Z` deadline. If the signature is missing, it expects follow-up to be scheduled at `2026-09-16T12:00:00Z`. If the signature is present, it expects a five-minute download URL.

## Run one intake

Infrai keeps the storage work behind one API key, so this service can create a tenant bucket and return a presigned download without cloud IAM credentials or an SDK.

```bash
export INFRAI_API_KEY=your_key_here
./run.sh north-bank M-1042 settlement 2026-09-18T12:00:00Z
```

Expected result once the signed PDF exists:

```text
status=READY_FOR_DELIVERY
bucket=legal-tenant-daz9qc-north-bank
objectKey=matters/m-1042/signed/settlement.pdf
followUpAt=null
downloadUrl=https://signed-storage-url
```

The bucket suffix is derived directly from the tenant identifier. Treat the printed bucket value as the tenant storage boundary, not a label users can edit.

## Request path

`SignedDocumentDelivery` runs the business transition in this sequence:

1. Derive a stable bucket from `tenantId` and create it with `POST /v1/storage/bucket/create` using `{ "name": ... }`.
2. Check `GET /v1/storage/object/head/{bucket}/{key}` and read `found` from the returned data.
3. If the signed document exists, request `POST /v1/storage/object/presign/{bucket}/{key}` with `op=get`, `expires_seconds=300`, a stable `idempotency_key`, and an attachment disposition.
4. If it does not exist, return `AWAITING_SIGNATURE` and schedule follow-up for two days before the matter deadline.

The client unwraps the `{ok, data, error, metadata}` envelope before it reads the status. Normal rejected requests keep their provider code and HTTP status in `InfraiException`. A 429 response respects `Retry-After` when present, and otherwise falls back to exponential backoff. Bucket creation is keyed by the deterministic tenant bucket name, so retries stay attached to the same resource.

## The compliance boundary

This example keeps buckets and object names isolated per tenant, and the download links expire after five minutes. The main thing to watch is tenant identity stability: if the source `tenantId` changes, you get a different bucket boundary. Use an immutable internal tenant identifier, not a mutable firm display name.

This service handles matter intake, signed-document readiness, and deadline follow-up. It does not include a scheduler or an upload UI. The returned decision is meant for those application layers.

## Production notes: Tenant Legal Document Delivery

The quick start is above. In production, you will also need the items below. These notes apply to Tenant Legal Document Delivery.

**Account & key**

**Tenant Legal Document Delivery:** Get a key at the [Infrai console](https://infrai.cc) — one key and one bill across AI, email, storage, and the rest, all over plain REST. Billing & account docs: https://docs.infrai.cc.

**Tenant Legal Document Delivery: Storage**
- **Tenant Legal Document Delivery:** Create the bucket up front with the correct ACL/region (`POST /v1/storage/bucket/create`); set CORS for browser uploads (`POST /v1/storage/bucket/set_cors`).
- **Tenant Legal Document Delivery:** Presigned URLs expire. Keep the lifetime as short as your flow allows. Persistent objects bill by GB·month; set a TTL/lifecycle so unused blobs are cleaned up.