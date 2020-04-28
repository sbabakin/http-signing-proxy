package dev.sb.services.proxy;

public class ProxyHeaders {
    public static final String DATE = "Date";
    public static final String DIGEST = "Digest";
    public static final String SIGNATURE = "Signature";

    // note that we try to sign only X-COMPANY- headers (check RequestSigner)
    public static final String X_COMPANY_SERVICE_NAME = "X-COMPANY-SERVICE-NAME";
    public static final String X_COMPANY_REQUEST_ID = "X-COMPANY-REQUEST-ID";
}
