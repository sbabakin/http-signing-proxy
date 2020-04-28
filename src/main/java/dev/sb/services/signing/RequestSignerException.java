package dev.sb.services.signing;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR, reason = "REQUEST_SIGNER_EXCEPTION")
public class RequestSignerException extends RuntimeException {

    public RequestSignerException(Exception e) {
        super(e);
    }
}
