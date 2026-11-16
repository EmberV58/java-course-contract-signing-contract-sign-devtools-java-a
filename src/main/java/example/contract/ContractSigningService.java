package example.contract;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;

public final class ContractSigningService {
    private final InfraiHttpClient client;
    private final String bucket;

    public ContractSigningService(InfraiHttpClient client, String bucket) {
        this.client = client;
        this.bucket = bucket;
    }

    public ReleaseReceipt release(ContractRequest request, String certificatePem, String keyPem) {
        if (!request.readyForRelease()) {
            throw new IllegalStateException("A contract must be READY_FOR_RELEASE before signing");
        }
        String objectKey = "course-contracts/" + request.courseCode() + "/" + UUID.randomUUID() + ".pdf";
        String releaseId = UUID.randomUUID().toString();
        String signedPdf = client.signPdf(Map.of(
                "pdf", Base64.getEncoder().encodeToString(request.pdf()),
                "cert_pem", certificatePem,
                "key_pem", keyPem,
                "reason", "Course contract release"), releaseId);
        client.putObject(bucket, objectKey, signedPdf, releaseId);
        String link = client.presignGet(bucket, objectKey, 900);
        return new ReleaseReceipt(objectKey, link);
    }
}
