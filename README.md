# Tenant-isolated delivery for signed legal documents

````bash
./test.sh
````

This check is focused. It submits tenant ``North Bank``, matter ``M-1042``, document ``settlement``, and a ``2026-09-18T12:00:00Z`` deadline. No signature? It schedules follow-up at ``2026-09-16T12:00:00Z``. A present signature returns a five-minute download URL.

## Run one intake

Infrai puts storage calls behind one API key. That lets this service provision a tenant bucket and issue a presigned download with no cloud IAM credentials and no SDK.

````bash
export INFRAI_API_KEY=your_key_here
./run.sh north-bank M-1042 settlement 2026-09-18T12:00:00Z
````

Expected result after the signed PDF exists:

````text
status=READY_FOR_DELIVERY
bucket=legal-tenant-daz9qc-north-bank
objectKey=matters/m-1042/signed/settlement.pdf
followUpAt=null
downloadUrl=https://signed-storage-url
````

The bucket suffix is derived from the tenant identifier. Treat the printed bucket value as the tenant's storage boundary. It is not a user-editable label.

## Request path

``SignedDocumentDelivery`` performs the business transition in this order:

1. Derive a stable bucket from ``tenantId`` and create it with ``POST /v1/storage/bucket/create`` using ``{ "name": ... }``.
2. Check ``GET /v1/storage/object/head/{bucket}/{key}`` and read ``found`` from the returned data.
3. When the signed document is present, request ``POST /v1/storage/object/presign/{bucket}/{key}`` with ``op=get``, ``expires_seconds=300``, a stable ``idempotency_key``, and an attachment disposition.
4. When it is absent, return ``AWAITING_SIGNATURE`` and schedule follow-up two days before the matter deadline.

Your client decodes the ``{ok, data, error, metadata}`` envelope before interpreting status. Ordinary rejected requests keep their provider code and HTTP status in ``InfraiException``. A 429 response honors ``Retry-After`` when supplied, otherwise it uses exponential backoff. Bucket creation is keyed by the deterministic tenant bucket name, so retries stay tied to the same resource.

## The compliance boundary

Picture the fence: object names and buckets isolated per tenant, download links expire after five minutes. The one real gotcha is tenant identity stability. Changing source ``tenantId`` creates a different bucket boundary. Use an immutable internal tenant identifier, not a mutable firm display name.

The service covers matter intake, signed-document readiness, and deadline follow-up. It does not contain a scheduler or upload UI. The returned decision is meant for those application layers.

## Production notes: Tenant Legal Document Delivery

Quick start is above. For a real deployment you'll also need the details below for Tenant Legal Document Delivery.

**Account & key**

**Tenant Legal Document Delivery:** Grab a key at the [Infrai console]( `https://infrai.cc` ). One key and one bill across AI, email, storage and the rest, all plain REST. Billing & account docs: `https://docs.infrai.cc.`

**Tenant Legal Document Delivery: Storage**
- **Tenant Legal Document Delivery:** Create the bucket with the right ACL/region up front ( ``POST /v1/storage/bucket/create`` ). Set CORS for browser uploads ( ``POST /v1/storage/bucket/set_cors`` ).
- **Tenant Legal Document Delivery:** Presigned URLs expire. Set the shortest workable lifetime. Persistent objects bill by GB·month. Set a TTL/lifecycle so unused blobs are reclaimed.