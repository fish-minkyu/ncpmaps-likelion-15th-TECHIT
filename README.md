# NCP Maps (Navaer Cloud Maps)
- 2024.02.21 ~ 02.22 `15주차`
- 02.21 - API 사용 (NaviController에 비즈니스 로직이 몰려 있는 형태로 코드 구현)
- 02.22 - NaviController와 NaviService의 코드 분리

본 프로젝트는 ncpmaps 스켈레톤 코드를 제공 받아 만든 프로젝트다.  
사용 API는 다음과 같다.

- AI NAVER API
  - Web Dynamic Map
  - Directions 5
  - Geocoding
  - Reverse Geocoding

- GeoLocation

Http Interface 방법으로 Http Client를 구현했다.  
Http Interface 특징 상, 사용할 외부 API에게 어떻게 요청을 보낼지 정의한 인터페이스와  
요청을 보낼 객체(RestTemplage, WebClient, RestClient)가 필요하다.

해당 프로젝트는 `Restclient`와 `Http Interface`를 연결하였다.


## 스팩

- Spring Boot 3.2.2
- Spring Web
- Lombok

## Key Point

- `Http Interface`

[NcpClientConfig](/src/main/java/com/example/ncpmaps/config/NcpClientConfig.java)  
=> Http Interface를 사용하면서 실제로 요청을 보낼 RestClient 객체를 2개 만들어줬다.  
( AI NAVER API와 GeoLocation이 인증을 요구하는 방식이 달랐으므로 2개를 만들어줬다.)

1. RestClient(AI NAVER API) 정의
```java
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
```

2. Http Proxy(AI NAVER API) 정의
```java
  // Http Interface 구현체를 Bean으로 등록
  @Bean
  public NcpMapApiService mapApiService() {
    return HttpServiceProxyFactory
          .builderFor(RestClientAdapter.create(ncpMapClient()))
          .build()
          .createClient(NcpMapApiService.class);
  }
```

3. RestClient(GeoLocation) 정의
```java
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
```

4. Http Proxy(GeoLocation) 정의
```java
  @Bean
  // ncpGeolocationClient + NcpGeolocationService == geolocationService
  public NcpGeolocationService geolocationService() {
    return HttpServiceProxyFactory
          .builderFor(RestClientAdapter.create(ncpGeolocationClient()))
          .build()
          .createClient(NcpGeolocationService.class);
  }
```

5. Signature Method: GeoLocation API에서 요구하는 전자 서명을 만든다.
```java
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
```

[NcpMapApiService](/src/main/java/com/example/ncpmaps/service/NcpMapApiService.java)  
=> AI NAVER API에게 어떻게 요청을 보낼지 정의한 인터페이스다.
```java
// HTTP 요청을 보내는 방법
public interface NcpMapApiService {
  // directions5
  @GetExchange("/map-direction/v1/driving")
  DirectionNcpResponse directions5(
          @RequestParam
          Map<String, Object> params
  );

  // geocode
  @GetExchange("/map-geocode/v2/geocode")
  GeoNcpResponse geocode(
          @RequestParam
          Map<String, Object> params
  );

  // reverse geocode
  @GetExchange("/map-reversegeocode/v2/gc")
  RGeoNcpResponse reverseGeocode(
          @RequestParam
          Map<String, Object> params
  );
}
```

[NcpGeolocationService](/src/main/java/com/example/ncpmaps/service/NcpGeolocationService.java)  
=> GeoLocation API에게 어떻게 요청을 보낼지 정의한 인터페이스다.
```java
public interface NcpGeolocationService {
  @GetExchange
  GeoLocationNcpResponse geoLocation(
    @RequestParam
    Map<String, Object> params
  );
}
```

[TestController](/src/main/java/com/example/ncpmaps/TestController.java)  
=> GeoLocation API를 테스트 해보는 컨트롤러다.
```java
@RestController
@RequestMapping("test")
@RequiredArgsConstructor
public class TestController {
  private final NcpGeolocationService geolocationService;

  @GetMapping("geolocation")
  public GeoLocationNcpResponse geoLocation(
      @RequestParam("ip")
      String ip
  ) {
      return geolocationService.geoLocation(Map.of(
        "ip", ip,
        // Geolocation은 응답 기본형이 xml이므로 json을 넣어준다.
        "responseFormatType", "json",
        "ext", "t"
      ));
  }
}
```

## GitHUb

- 강사님 GitHub  
[likelion-backend-8-ncpmaps](https://github.com/edujeeho0/likelion-backend-8-ncpmaps)