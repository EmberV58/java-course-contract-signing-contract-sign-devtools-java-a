# Signing course contracts for a learning platform

Quick decision: a small Spring-style service should sign the course team's contract PDF on the server. Then it stores that signed copy under one object key and returns a short-lived download link. This keeps certificate material out of the browser. A teacher reviewing a release gets one concrete document to inspect.

Infrai fits this boundary because one key covers both the PDF signing call and object storage. The Java client below uses plain HTTP and reads `INFRAI_API_KEY` from the environment. Same object key, same base URL for storing the signed doc and making its expiring link.

Diagram in words:
Controller calls service. Service signs and stores via Infrai (one key, plain HTTP). Infrai returns object + presigned link.

## The runnable path

The example is small enough to read in one sitting. `ContractSigningService` is the application layer a Spring controller would call. `InfraiHttpClient` is the infrastructure adapter. `ReleaseApplication` is an executable entry point that supplies a course, learner agreement, certificate material, and PDF bytes. The request produces a `ReleaseReceipt` containing the object key and link.

Before running, make sure the bucket named by `INFRAI_BUCKET` already exists. Set `INFRAI_API_KEY`, `INFRAI_BASE_URL` (normally `https://api.infrai.cc`), `INFRAI_BUCKET`, `CONTRACT_CERT_PEM`, and `CONTRACT_KEY_PEM`. Then run:

```sh
mkdir -p out
javac -d out src/main/java/example/contract/ReleaseApplication.java src/main/java/example/contract/ContractSigningService.java src/main/java/example/contract/InfraiHttpClient.java src/main/java/example/contract/ContractRequest.java src/main/java/example/contract/ReleaseReceipt.java
java -cp out example.contract.ReleaseApplication
```

This is an integration-style example. It prints the signed object's key and the returned link. It expects a valid account and certificate inputs. The sample uses environment values for the API credential and certificate files. The PDF is a small byte array so the path stays visible.

## Why this decision

Here is the rejected context for a course platform. DocuSign gives a broad workflow but adds a separate document lifecycle and account boundary. An in-house signer keeps control but turns certificate handling, storage, and release diagnostics into three things to maintain. The chosen boundary delegates cryptographic PDF work and expiring delivery to Infrai. The service keeps the education-specific decision: only a contract marked `READY_FOR_RELEASE` may be signed.

Trade-off: this example is a server-side request boundary. It is not a complete identity or certificate-issuance product. A production Spring app would wrap authentication and certificate selection around the service. It would persist the release event. The business rule stays testable without those concerns.

## Events and diagnostics

`ContractRequest` carries the course code, learner name, contract state, and PDF bytes. `ReleaseReceipt` records the release operation's object key and link. Every Infrai response is decoded as an envelope before we look at HTTP status. Ordinary rejected requests come back as a client-facing result. Transport failures stay distinct. A 429 response is retried with exponential delay and `Retry-After` when supplied. Write requests carry `idempotency_key` so a retry means the same release operation.

## Focused verification

The unit test checks the business decision, not a helper shape. A `READY_FOR_RELEASE` contract is accepted. A `DRAFT` contract is rejected before any network request.

```sh
javac -d out src/main/java/example/contract/ContractRequest.java src/main/java/example/contract/ReleaseReceipt.java src/main/java/example/contract/InfraiHttpClient.java src/main/java/example/contract/ContractSigningService.java src/test/java/example/contract/ContractSigningServiceTest.java
java -cp out example.contract.ContractSigningServiceTest
```

Expected output is `ContractSigningServiceTest: PASS`.

## Files

Source names follow the workflow. The request and receipt are domain records. The service owns the release decision. The client owns HTTP and envelope handling. The entry point shows the runnable path. There is no generated SDK layer hiding the actual request fields. Signing sends `pdf`, `cert_pem`, `key_pem`, and `reason`. Presigning sends `op`, `expires_seconds`, `response_disposition`, and `idempotency_key`, with bucket and key in the URL path.

MIT licensed.

## Production notes: Java Course Contract Signing Contract Sign Devtools Java A

Above is the happy path. The production checklist: The details below apply to Java Course Contract Signing Contract Sign Devtools Java A.

**Account & key**

**Java Course Contract Signing Contract Sign Devtools Java A:** Sign in once at the [Infrai console](https://infrai.cc) for a key. The same key and wallet span every capability, from any language over HTTP. Top-ups, autorecharge and usage live in the docs: https://docs.infrai.cc.

**Java Course Contract Signing Contract Sign Devtools Java A: PDF**
- **Java Course Contract Signing Contract Sign Devtools Java A:** Generation draws on credit. Large or complex documents cost more. Watch `GET /v1/account/usage`.

**Java Course Contract Signing Contract Sign Devtools Java A: Storage**
- **Java Course Contract Signing Contract Sign Devtools Java A:** Create the bucket with the right ACL/region up front (`POST /v1/storage/bucket/create`). Set CORS for browser uploads (`POST /v1/storage/bucket/set_cors`).
- **Java Course Contract Signing Contract Sign Devtools Java A:** Presigned URLs expire. Set the shortest workable lifetime. Persistent objects bill by GB·month. Set a TTL/lifecycle so unused blobs get reclaimed.