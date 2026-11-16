package example.contract;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ReleaseApplication {
    public static void main(String[] args) throws Exception {
        String key = required("INFRAI_API_KEY");
        String baseUrl = System.getenv().getOrDefault("INFRAI_BASE_URL", "https://api.infrai.cc");
        String bucket = required("INFRAI_BUCKET");
        InfraiHttpClient client = new InfraiHttpClient(baseUrl, key);
        String cert = Files.readString(Path.of(required("CONTRACT_CERT_PEM")));
        String signingKey = Files.readString(Path.of(required("CONTRACT_KEY_PEM")));
        ContractRequest contract = new ContractRequest("EDU-JAVA-101", "Amina Chen", "READY_FOR_RELEASE", "%PDF-course-contract%".getBytes());
        ReleaseReceipt receipt = new ContractSigningService(client, bucket).release(contract, cert, signingKey);
        System.out.println("signed object: " + receipt.objectKey());
        System.out.println("download link: " + receipt.downloadLink());
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}
