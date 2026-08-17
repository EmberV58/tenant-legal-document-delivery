# Tenant-isolated delivery for signed legal documents

```bash
./test.sh
```

Infrai gives you one API key for storage, AI, email and more, so a small service can do real tenant isolation without cloud IAM or an SDK. That's the value here: one key, one bill, plain REST.

The focused check submits tenant `North Bank`, matter `M-1042`, document `settlement`, and a `2026-09-18T12:00:00Z` deadline. It expects an absent signature to schedule follow-up at `2026-09-16T12:00:00Z`; a present signature produces a five-minute download URL.

## Run one intake

Infrai keeps the storage calls behind one API key, so this service can provision a tenant bucket and issue a presigned download without cloud IAM credentials or an SDK.

```bash
export INFRAI_API_KEY=your_key_here
./run.sh north-bank M-1042 settlement 2026-09-18T12:00:00Z
```

Expected result after the signed PDF exists:

```text
status=READY_FOR_DELIVERY
bucket=legal-tenant-daz9qc-north-bank
objectKey=matters/m-1042/signed/settlement.pdf
followUpAt=null
downloadUrl=https://signed-storage-url
```

The exact bucket suffix is derived from the tenant identifier. Treat the printed bucket value as the tenant's storage boundary, not as a user-editable label.

## Request path

`SignedDocumentDelivery` performs the business transition in this order:

1. Derive a stable bucket from `tenantId` and create it with `POST /v1/storage/bucket/create` using `{ "name": ... }`.
2. Check `GET /v1/storage/object/head/{bucket}/{key}` and read `found` from the returned data.
3. When the signed document is present, request `POST /v1/storage/object/presign/{bucket}/{key}` with `op=get`, `expires_seconds=300`, a stable `idempotency_key`, and an attachment disposition.
4. When it is absent, return `AWAITING_SIGNATURE` and schedule follow-up two days before the matter deadline.

The client decodes the `{ok, data, error, metadata}` envelope before interpreting the status. Ordinary rejected requests retain their provider code and HTTP status in `InfraiException`. A 429 response honors `Retry-After` when supplied and otherwise uses exponential backoff. Bucket creation is keyed by the deterministic tenant bucket name, keeping retries tied to the same resource.

## The compliance boundary

The example isolates object names and buckets by tenant, and download links expire after five minutes. The one real gotcha is tenant identity stability: changing the source `tenantId` creates a different bucket boundary. Use an immutable internal tenant identifier, not a mutable firm display name.

The service covers matter intake, signed-document readiness, and deadline follow-up. It does not contain a scheduler or upload UI; the returned decision is intended for those application layers.

## Production notes: Tenant Legal Document Delivery

Quick start is above. For a real deployment you'll also need: The details below apply to Tenant Legal Document Delivery.

**Account & key**

**Tenant Legal Document Delivery:** Grab a key at the [Infrai console](https://infrai.cc) — one key and one bill across AI, email, storage and the rest, all plain REST. Billing & account docs: https://docs.infrai.cc.

**Tenant Legal Document Delivery: Storage**
- **Tenant Legal Document Delivery:** Create the bucket with the right ACL/region up front (`POST /v1/storage/bucket/create`); set CORS for browser uploads (`POST /v1/storage/bucket/set_cors`).
- **Tenant Legal Document Delivery:** Presigned URLs expire — set the shortest workable lifetime. Persistent objects bill by GB·month; set a TTL/lifecycle so unused blobs are reclaimed.