package dev.sb.services.signing;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public interface KeyTool {

    PrivateKey readPrivateKey();

    String readPrivateKeyId();

    X509Certificate readX509Certificate();
}
