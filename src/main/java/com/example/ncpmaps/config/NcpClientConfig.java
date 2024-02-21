package com.example.ncpmaps.config;

import com.example.ncpmaps.service.NcpGeolocationService;
import com.example.ncpmaps.service.NcpMapApiService;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Configuration
public class NcpClientConfig {
    // NCP Map API Rest Client
    private static final String NCP_APIGW_KEY_ID = "X-NCP-APIGW-API-KEY-ID";
    private static final String NCP_APIGW_KEY = "X-NCP-APIGW-API-KEY";
    @Value("${ncp.api.client-id}")
    private String ncpMapClientId;
    @Value("${ncp.api.client-secret}")
    private String ncpMapClientSecret;

    @Bean
    public RestClient ncpMapClient() {
        return RestClient.builder()
                .baseUrl("https://naveropenapi.apigw.ntruss.com")
                .defaultHeader(NCP_APIGW_KEY_ID, ncpMapClientId)
                .defaultHeader(NCP_APIGW_KEY, ncpMapClientSecret)
                .build();
    }

    @Bean
    public NcpMapApiService mapApiService() {
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(ncpMapClient()))
                .build()
                .createClient(NcpMapApiService.class);
    }


    // Geolocation API Rest Client
    private static final String X_TIMESTAMP_HEADER = "x-ncp-apigw-timestamp";
    private static final String X_IAM_ACCESS_KEY = "x-ncp-iam-access-key";
    private static final String X_APIGW_SIGNATURE = "x-ncp-apigw-signature-v2";
    @Value("${ncp.api.api-access}")
    private String accessKey;
    @Value("${ncp.api.api-secret}")
    private String secretKey;

    @Bean
    public RestClient ncpGeolocationClient() {
        return RestClient.builder()
                .baseUrl("https://geolocation.apigw.ntruss.com/geolocation/v2/geoLocation")
                .requestInitializer(request -> {
                    HttpHeaders requestHeaders = request.getHeaders();

                    // 1. 요청을 보내는 Unix Time
                    long timestamp = System.currentTimeMillis();
                    requestHeaders.add(X_TIMESTAMP_HEADER, Long.toString(timestamp));
                    // 2. Access Key
                    requestHeaders.add(X_IAM_ACCESS_KEY, accessKey);
                    // 3. 현재시각 + 요청 URI + 요청 메서드 정보로 만드는 Signature
                    // Signature를 만드는데 필요한 정보는 `request`에 있다.
                    requestHeaders.add(X_APIGW_SIGNATURE, makeSignature(
                            request.getMethod(),
                            request.getURI().getPath() + "?" + request.getURI().getQuery(),
                            timestamp
                    ));
                })
                .build();
    }

    @Bean
    public NcpGeolocationService geolocationService() {
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(ncpGeolocationClient()))
                .build()
                .createClient(NcpGeolocationService.class);
    }

    // Signature를 만드는 메서드
    private String makeSignature(HttpMethod method, String url, long timestamp) {
        String space = " ";
        String newLine = "\n";

        String message = new StringBuilder()
                .append(method.name())
                .append(space)
                .append(url)
                .append(newLine)
                .append(timestamp)
                .append(newLine)
                .append(accessKey)
                .toString();

        try {
            SecretKeySpec signingKey = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);

            byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));

            return Base64.encodeBase64String(rawHmac);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

















