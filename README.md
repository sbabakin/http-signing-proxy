# HTTP Signing Proxy Service
Non-blocking HTTP messages signing proxy based on Spring Cloud Gateway (Netty + Spring WebFlux)

## Use Case
A third-party services requires Signing HTTP Messages [(IETF specification)](https://datatracker.ietf.org/doc/draft-ietf-httpbis-message-signatures/).

But what if you don't want to (or simply can't) implement HTTP requests signing right in the code that originates a request?
Then you can use HTTP signing proxy.

HTTP request --> Signing Proxy --> signed HTTP request
  
## Implementation
The implementation we offer is based on Spring Cloud Gateway.

Benefits:
* non-blocking API (Netty with Spring WebFlux)
* Spring configuration features and Actuator endpoints (easy integration).

HTTP messages signing logic:
 * add HTTP header 'Date' (RFC-1123 format) 
 * calculate message digest for POST and PUT requests and add HTTP header 'Digest'
 * sign 'Date', 'Digest' and company specific HTTP headers 
 * add 'Signature' HTTP header
 
We use Tomitribe implementation for HTTP headers calculation.
    
* Signing HTTP Messages: [IETF specification](https://datatracker.ietf.org/doc/draft-ietf-httpbis-message-signatures/)
* Spring Cloud Gateway: [documentation](https://cloud.spring.io/spring-cloud-gateway/reference/html/)
* Java Client Library for HTTP Signatures: [Tomitribe GitHub](https://github.com/tomitribe/http-signatures-java)

## Application configuration

In Kubernetes we can use following environment variables to configure the app

| Env variable                   | Description                          | Sample Value             |
| --------------                 | --------------                       | --------------           |
| SIGNING_PROXY_GATEWAY_URI      | Third-party system gateway address   | https://system.org       |
| SIGNING_PROXY_GATEWAY_PATH     | Third-party system gateway path      | /complex/api/            |
| SIGNING_PROXY_CERT_FILE        | Certificate file content             | <cert content>           |
| SIGNING_PROXY_PRIVATE_KEY_FILE | RSA Private Key content              | <key content>            |
| SIGNING_PROXY_PRIVATE_KEY_PASS | Key file password                    | secret!                  |
| SIGNING_PROXY_PRIVATE_KEY_ID   | Key ID (part of a signature)         | ABC12345                 |

## Quick Start
You need a valid private RSA key to sign HTTP messages (see instructions below).
For development and debug purposes, you can use Postman Echo service https://postman-echo.com.

Postman Echo as external system and test keys are configured in 'dev' profile.     

```
mvn clean install
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Default configuration:
* http://localhost:8081/api/get --> https://postman-echo.com/get
* http://localhost:8081/api/post --> https://postman-echo.com/post

Use Postman or cURL for testing
```
curl --location --request POST 'localhost:8081/api/post' \
--header 'Content-Type: application/json' \
--data-raw '{
    "key1": "value1",
    "key2": "value2"
}'
```

### Generate Keys
Generate an RSA private key, of size 2048, and output it to a file named key.pem:
 ```
 openssl req -x509 -newkey rsa:2048 -keyout key.pem -out cert.pem -days 3650
 ```
