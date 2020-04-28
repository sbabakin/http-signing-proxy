package dev.sb.services.generic;

import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class InfoController {

    private final InfoEndpoint endpoint;

    public InfoController(InfoEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    /** just expose application info at context root */
    @GetMapping("/")
    public Map<String, Object> info() {
        return endpoint.info();
    }
}
