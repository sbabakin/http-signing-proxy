package dev.sb.services.signing;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;
import org.bouncycastle.util.io.pem.PemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/** Implementation the allows load private keys and certificates created by OpenSSL */
@Component
@Slf4j
public class OpenSSLKeyTool implements KeyTool {

    private final X509Certificate certificate;
    private final PrivateKey privateKey;
    private final String privateKeyId;

    public OpenSSLKeyTool(@Value("${application.signing.privateKey}") String privateKey,
                          @Value("${application.signing.privateKeyPass}") String privateKeyPass,
                          @Value("${application.signing.privateKeyId}") String privateKeyId,
                          @Value("${application.signing.certificate}") String certificate) {

        this.certificate = loadX509Certificate(certificate);
        this.privateKey = loadPrivateKey(privateKey, privateKeyPass);
        this.privateKeyId = privateKeyId;

        log.info("RSA key and certificate are configured");
        log.info("RSA keyId: {}", privateKeyId);
    }

    @Override
    public String readPrivateKeyId() {
        return privateKeyId;
    }

    @Override
    public PrivateKey readPrivateKey() {
        return privateKey;
    }

    @Override
    public X509Certificate readX509Certificate() {
        return certificate;
    }

    /** Have to use BouncyCastle due to algorithms used in OpenSSL */
    private PrivateKey loadPrivateKey(String privateKeyPEM, String privateKeyPass) {
        try {

            InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(privateKeyPEM.getBytes()));
            final PEMParser pemParser = new PEMParser(new PemReader(reader));

            PrivateKey key;
            Object pemContent = pemParser.readObject();
            if (pemContent instanceof PKCS8EncryptedPrivateKeyInfo) {
                final PKCS8EncryptedPrivateKeyInfo encPrivateKeyInfo = (PKCS8EncryptedPrivateKeyInfo) pemContent;
                InputDecryptorProvider decryptorProvider = new JceOpenSSLPKCS8DecryptorProviderBuilder()
                    .build(privateKeyPass.toCharArray());
                final PrivateKeyInfo privateKeyInfo = encPrivateKeyInfo.decryptPrivateKeyInfo(decryptorProvider);

                key = new JcaPEMKeyConverter().getPrivateKey(privateKeyInfo);
            } else {
                throw new KeyToolException("Unsupported private key format: " + pemContent.getClass().getSimpleName());
            }

            pemParser.close();
            return key;

        } catch (IOException e) {
            throw new KeyToolException("PEM file read problem", e);
        } catch (OperatorCreationException | PKCSException e) {
            throw new KeyToolException("PEM file decryption problem", e);
        }
    }

    private X509Certificate loadX509Certificate(String certificateContent) {
        try {
            InputStream in = new ByteArrayInputStream(certificateContent.getBytes());
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) factory.generateCertificate(in);
        } catch (CertificateException e) {
            throw new KeyToolException(e);
        }
    }
}
