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
// ncp에 요청을 보내기 위한 RestClient
public class NcpClientConfig {
  // NCP Map API Rest Client
  private static final String NCP_APIGW_KEY_ID = "X-NCP-APIGW-API-KEY-ID";
  private static final String NCP_APIGW_KEY = "X-NCP-APIGW-API-KEY";
  @Value("${ncp.api.client-id}")
  private String ncpMapClientId;
  @Value("${ncp.api.client-secret}")
  private String ncpMapClientSecret;

  // Bean으로 등록하면 하나의 객체를 공유해서 사용할 수 있다.
  @Bean
  // RestClient
  public RestClient ncpMapClient() {
    return RestClient.builder()
          // 해당 RestClient에서 시작되는 요청은 해당 경로에서부터 시작이 된다.
          .baseUrl("https://naveropenapi.apigw.ntruss.com")
          // defaultHeader(): 해당 RestClient의 모든 요청에 대해서 해당 헤더를 적용해서 사용할 수 있다.
          .defaultHeader(NCP_APIGW_KEY_ID, ncpMapClientId)
          .defaultHeader(NCP_APIGW_KEY, ncpMapClientSecret)
          .build();
  }

  // Http Interface 구현체를 Bean으로 등록
  @Bean
  public NcpMapApiService mapApiService() {
    return HttpServiceProxyFactory
          .builderFor(RestClientAdapter.create(ncpMapClient()))
          .build()
          .createClient(NcpMapApiService.class);
  }


  // Geolocation API Rest Client
  // : AI NAVER API와 GeoLocation의 키가 달라서 RestClient를 하나 더 만들어준다.
  private static final String X_TIMESTAMP_HEADER = "x-ncp-apigw-timestamp";
  private static final String X_IAM_ACCESS_KEY = "x-ncp-iam-access-key";
  private static final String X_APIGW_SIGNATURE = "x-ncp-apigw-signature-v2";
  @Value("${ncp.api.api-access}")
  private String accessKey;
  @Value("${ncp.api.api-secret}")
  private String secretKey;

  @Bean
  // 메서드 이름이 다르면 사용하게 되는 Bean 객체도 이름이 달리 되기 때문에
  // 똑같은 타입의 Bean 객체를 여러개 만들 수 있다.
  public RestClient ncpGeolocationClient() {
    return RestClient.builder()
          .baseUrl("https://geolocation.apigw.ntruss.com/geolocation/v2/geoLocation")
          // requestInitializer
          // : 해당 RestClient의 모든 요청을 보내기 전에, 헤더를 추가할 수 있다.
          .requestInitializer(request -> {
            // 여러 개를 설정해줄 것이므로 지역 변수 생성
            // requestHeaders에는 method와 헤더 정보가 담겨져 있다.
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
  // ncpGeolocationClient + NcpGeolocationService == geolocationService
  public NcpGeolocationService geolocationService() {
    return HttpServiceProxyFactory
          .builderFor(RestClientAdapter.create(ncpGeolocationClient()))
          .build()
          .createClient(NcpGeolocationService.class);
  }

  // Signature를 만드는 메서드
  private String makeSignature(HttpMethod method, String url, long timestamp) {
    String space = " ";
    String newLine = "\n";
    // 요청에 따라 변하는 부분 -> 매개 변수로 넣어준다.
/*    String method = "GET";
    String url = "/phots/puppy.jpg?query=1&query2";
    String timestamp = "{timestamp}";*/

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

















