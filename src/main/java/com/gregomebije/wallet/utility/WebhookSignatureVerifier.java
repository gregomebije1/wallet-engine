package com.gregomebije.wallet.utility;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;


/*
HMAC (Hash-based Message Authentication Code) is a cryptographic technique that uses a shared secret key and a hash function (like SHA-256) to prove two things about a message:
1. Authenticity: It came from someone who actually knows the secret key (your payment gateway).
2. Integrity: Not a single byte of the message was altered in transit.
*/
@Component
public class WebhookSignatureVerifier {

    private final byte[] secretBytes;

    public WebhookSignatureVerifier(@Value("${gateway.webhook-secret}") String webhookSecret) {
        this.secretBytes = webhookSecret.getBytes(StandardCharsets.UTF_8);
    }

    public boolean isValid(String rawPayload, String signatureHeader) {
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretBytes, "HmacSHA256"));
            byte[] digest = mac.doFinal(rawPayload.getBytes(StandardCharsets.UTF_8));
            String expected = "sha256=" + HexFormat.of().formatHex(digest);

            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signatureHeader.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            return false;
        }
    }

    public String sign(String rawPayload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretBytes, "HmacSHA256"));
            byte[] digest = mac.doFinal(rawPayload.getBytes(StandardCharsets.UTF_8));
            return "sha256=" + HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute HMAC-SHA256 signature", e);
        }
    }

}
