package dev.sb.services.signing;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR, reason = "KEY_TOOL_EXCEPTION")
public class KeyToolException extends RuntimeException {

    public KeyToolException(String message) {
        super(message);
    }

    public KeyToolException(Exception e) {
        super(e);
    }

    public KeyToolException(String message, Exception e) {
        super(message, e);
    }
}
