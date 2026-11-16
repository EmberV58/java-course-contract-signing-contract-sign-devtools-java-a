package example.contract;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InfraiHttpClient {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String baseUrl;
    private final String apiKey;

    public InfraiHttpClient(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.apiKey = apiKey;
    }

    public String signPdf(Map<String, String> fields, String idempotencyKey) {
        // The domain call represented here is infrai.pdf.sign.
        String body = "{\"pdf\":\"" + json(fields.get("pdf")) + "\",\"cert_pem\":\"" + json(fields.get("cert_pem")) + "\",\"key_pem\":\"" + json(fields.get("key_pem")) + "\",\"reason\":\"" + json(fields.get("reason")) + "\"}";
        String envelope = request("POST", "/v1/pdf/sign", body, idempotencyKey);
        Matcher signedPdf = Pattern.compile("\\\"signed_pdf(?:_base64)?\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(envelope);
        if (!signedPdf.find()) {
            throw new InfraiRequestException("sign response did not contain signed_pdf", 200);
        }
        return signedPdf.group(1);
    }

    public void putObject(String bucket, String key, String dataBase64, String idempotencyKey) {
        String body = "{\"data_base64\":\"" + json(dataBase64) + "\",\"content_type\":\"application/pdf\",\"idempotency_key\":\"" + json(idempotencyKey) + "\"}";
        request("PUT", "/v1/storage/object/put/" + path(bucket) + "/" + path(key), body, idempotencyKey);
    }

    public String presignGet(String bucket, String key, int expiresSeconds) {
        String body = "{\"op\":\"get\",\"expires_seconds\":" + expiresSeconds + ",\"response_disposition\":\"attachment\"}";
        String envelope = request("POST", "/v1/storage/object/presign/" + path(bucket) + "/" + path(key), body, "presign-" + key);
        Matcher link = Pattern.compile("\\\"(?:url|link|download_url|presigned_url)\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(envelope);
        if (!link.find()) {
            throw new InfraiRequestException("presign response did not contain a link", 200);
        }
        return link.group(1);
    }

    private String request(String method, String path, String body, String operationId) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                        .timeout(Duration.ofSeconds(30)).header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json").header("Idempotency-Key", operationId)
                        .method(method, HttpRequest.BodyPublishers.ofString(body)).build();
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                String envelope = response.body();
                if (response.statusCode() == 429 && attempt < 2) {
                    Thread.sleep(retryDelay(response, attempt));
                    continue;
                }
                if (!envelope.contains("\"ok\":true")) {
                    throw new InfraiRequestException(envelope, response.statusCode());
                }
                return envelope;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InfraiRequestException("request interrupted", 0);
            } catch (IOException e) {
                throw new InfraiRequestException(e.getMessage(), 0);
            }
        }
        throw new InfraiRequestException("request did not complete", 429);
    }

    private long retryDelay(HttpResponse<String> response, int attempt) {
        return response.headers().firstValue("Retry-After").map(Long::parseLong).orElse(1L << attempt) * 1000L;
    }

    private String json(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }
        return escaped.toString();
    }
    private String path(String value) { return value.replace("/", "%2F"); }

    public static final class InfraiRequestException extends RuntimeException {
        public InfraiRequestException(String message, int status) { super("Infrai response " + status + ": " + message); }
    }
}
