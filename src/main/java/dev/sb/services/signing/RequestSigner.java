package dev.sb.services.signing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tomitribe.auth.signatures.Signature;
import org.tomitribe.auth.signatures.Signer;

import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * HTTP request headers signer
 * Refer https://datatracker.ietf.org/doc/draft-cavage-http-signatures/?include_text=1 for details.
 */
@Slf4j
@Component
public class RequestSigner {

    private KeyTool keyTool;

    public RequestSigner(KeyTool keyTool) {
        this.keyTool = keyTool;
    }

    /** Create digest of body. */
    public String createDigest(String body) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digestedBytes = digest.digest(body.getBytes(StandardCharsets.UTF_8));
            return "SHA-256=" + new String(Base64.getEncoder().encode(digestedBytes));
        } catch (NoSuchAlgorithmException e) {
            throw new RequestSignerException(e);
        }
    }

    /** Create signature based on method, path and set of headers */
    public String createSignature(String method, String path, Map<String, String> headers) {
        String keyId = keyTool.readPrivateKeyId();

        if (keyId == null) {
            // fallback to keyId based on certificate data            
            X509Certificate x509Certificate = keyTool.readX509Certificate();
            keyId = resolveKeyIdFromCertificate(x509Certificate);
        }

        List<String> signHeaders = new LinkedList<>(headers.keySet());
        signHeaders.removeIf(k -> !k.toUpperCase().startsWith("X-COMPANY"));
        signHeaders.add("Date");
        if (headers.containsKey("Digest")) {
            signHeaders.add("Digest");
        }

        Signature signature = new Signature(keyId, "rsa-sha256", null, signHeaders);
        Signer signer = new Signer(keyTool.readPrivateKey(), signature);
        try {
            return signer.sign(method, path, headers)
                .toString()
                .replace("Signature ", "");
        } catch (IOException e) {
            throw new RequestSignerException(e);
        }
    }

    /**
     * Returns formatted keyId composed from certificate serial number and issuer
     * Format example: keyId="SN=D9EA5432EA92D254,CA=O=PSDNO-FSA-DEADBEEF,L=Trondheim,C=NO"
     *
     * @param certificate X509 certificate
     * @return formatted keyId string
     */
    public String resolveKeyIdFromCertificate(X509Certificate certificate) {
        String serialNumberAsHex = certificate.getSerialNumber().toString(16).toUpperCase();
        String issuer = readRfc2253Issuer(certificate.getIssuerX500Principal());

        return String.format("SN=%s,CA=%s", serialNumberAsHex, issuer);
    }

    private String readRfc2253Issuer(X500Principal principal) {
        String rfc2253issuer = null;
        if (principal != null) {
            rfc2253issuer = principal.getName(X500Principal.RFC2253);
        }

        if (rfc2253issuer != null) {
            return new String(rfc2253issuer.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
        } else {
            return null;
        }
    }
}
