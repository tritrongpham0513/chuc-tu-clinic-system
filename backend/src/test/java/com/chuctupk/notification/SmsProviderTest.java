package com.chuctupk.notification;

import com.chuctupk.service.SmsService;
import com.chuctupk.service.impl.SmsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.Authenticator;
import java.net.ConnectException;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

import static org.assertj.core.api.Assertions.assertThat;

class SmsProviderTest {

    static class FakeSmsService implements SmsService {
        OtpSendResult resultToReturn = OtpSendResult.SENT;
        String errorCodeToReturn = null;
        String capturedPhone;
        String capturedMessage;

        @Override
        public OtpSendResult sendOtp(String phone, String otpCode) {
            return resultToReturn;
        }

        @Override
        public OtpSendResult sendMessage(String phone, String message) {
            this.capturedPhone = phone;
            this.capturedMessage = message;
            return resultToReturn;
        }

        @Override
        public String getProviderName() {
            return "esms";
        }

        @Override
        public String getLastErrorCode() {
            return errorCodeToReturn;
        }
    }

    static class FakeHttpResponse implements HttpResponse<String> {
        private final int status;
        private final String body;
        private final HttpRequest request;

        FakeHttpResponse(int status, String body, HttpRequest request) {
            this.status = status;
            this.body = body;
            this.request = request;
        }

        @Override public int statusCode() { return status; }
        @Override public HttpRequest request() { return request; }
        @Override public Optional<HttpResponse<String>> previousResponse() { return Optional.empty(); }
        @Override public HttpHeaders headers() { return HttpHeaders.of(Map.of(), (k, v) -> true); }
        @Override public String body() { return body; }
        @Override public Optional<SSLSession> sslSession() { return Optional.empty(); }
        @Override public URI uri() { return request.uri(); }
        @Override public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
    }

    @FunctionalInterface
    interface HttpCallHandler {
        HttpResponse<String> handle(HttpRequest request, int callNumber) throws IOException;
    }

    static class FakeHttpClient extends HttpClient {
        final List<HttpRequest> requests = new ArrayList<>();
        final List<String> requestBodies = new ArrayList<>();
        private final HttpCallHandler handler;

        FakeHttpClient(HttpCallHandler handler) {
            this.handler = handler;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
                throws IOException {
            requests.add(request);
            requestBodies.add(extractBody(request));
            return (HttpResponse<T>) handler.handle(request, requests.size());
        }

        private String extractBody(HttpRequest request) {
            return request.bodyPublisher().map(pub -> {
                List<ByteBuffer> chunks = new ArrayList<>();
                pub.subscribe(new Flow.Subscriber<>() {
                    @Override public void onSubscribe(Flow.Subscription subscription) { subscription.request(Long.MAX_VALUE); }
                    @Override public void onNext(ByteBuffer item) { chunks.add(item); }
                    @Override public void onError(Throwable throwable) {}
                    @Override public void onComplete() {}
                });
                int total = chunks.stream().mapToInt(ByteBuffer::remaining).sum();
                byte[] bytes = new byte[total];
                int offset = 0;
                for (ByteBuffer buf : chunks) {
                    int len = buf.remaining();
                    buf.get(bytes, offset, len);
                    offset += len;
                }
                return new String(bytes, StandardCharsets.UTF_8);
            }).orElse("");
        }

        @Override public Optional<CookieHandler> cookieHandler() { return Optional.empty(); }
        @Override public Optional<Duration> connectTimeout() { return Optional.empty(); }
        @Override public Redirect followRedirects() { return Redirect.NORMAL; }
        @Override public Optional<ProxySelector> proxy() { return Optional.empty(); }
        @Override public SSLContext sslContext() { return null; }
        @Override public SSLParameters sslParameters() { return null; }
        @Override public Optional<Authenticator> authenticator() { return Optional.empty(); }
        @Override public Version version() { return Version.HTTP_1_1; }
        @Override public Optional<Executor> executor() { return Optional.empty(); }
        @Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest r, HttpResponse.BodyHandler<T> h) { throw new UnsupportedOperationException(); }
        @Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest r, HttpResponse.BodyHandler<T> h, HttpResponse.PushPromiseHandler<T> p) { throw new UnsupportedOperationException(); }
    }

    static class RecordingSmsServiceImpl extends SmsServiceImpl {
        final List<Long> recordedSleeps = new ArrayList<>();

        @Override
        protected void sleep(long ms) {
            recordedSleeps.add(ms);
        }
    }

    private SmsServiceImpl configureService(RecordingSmsServiceImpl service, FakeHttpClient client, int smsType, String brandname) {
        ReflectionTestUtils.setField(service, "smsEnabled", true);
        ReflectionTestUtils.setField(service, "provider", "esms");
        ReflectionTestUtils.setField(service, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(service, "secretKey", "test-secret-key");
        ReflectionTestUtils.setField(service, "smsType", smsType);
        ReflectionTestUtils.setField(service, "brandname", brandname);
        ReflectionTestUtils.setField(service, "httpClient", client);
        return service;
    }

    @Test
    @DisplayName("Ticket 1 & 6: Map OtpSendResult sang DeliveryResult đúng 3 trường hợp (SENT, SKIPPED, FAILED)")
    void testMapOtpSendResultToDeliveryResult() {
        FakeSmsService fakeService = new FakeSmsService();
        SmsProvider provider = new SmsProvider(fakeService);

        NotificationMessage message = new NotificationMessage(
                1L, "0912345678", "BOOKING_SUCCESS", Map.of("message", "Thong bao test")
        );

        // 1. Trường hợp SENT
        fakeService.resultToReturn = SmsService.OtpSendResult.SENT;
        fakeService.errorCodeToReturn = null;
        DeliveryResult resSent = provider.send(message);
        assertThat(resSent.status()).isEqualTo(DeliveryResult.Status.SENT);
        assertThat(resSent.errorCode()).isNull();
        assertThat(resSent.providerRef()).isEqualTo("esms");
        assertThat(fakeService.capturedPhone).isEqualTo("0912345678");
        assertThat(fakeService.capturedMessage).isEqualTo("Thong bao test");

        // 2. Trường hợp SKIPPED
        fakeService.resultToReturn = SmsService.OtpSendResult.SKIPPED;
        fakeService.errorCodeToReturn = null;
        DeliveryResult resSkipped = provider.send(message);
        assertThat(resSkipped.status()).isEqualTo(DeliveryResult.Status.SKIPPED);
        assertThat(resSkipped.errorCode()).isNull();
        assertThat(resSkipped.providerRef()).isEqualTo("esms");

        // 3. Trường hợp FAILED
        fakeService.resultToReturn = SmsService.OtpSendResult.FAILED;
        fakeService.errorCodeToReturn = "101";
        DeliveryResult resFailed = provider.send(message);
        assertThat(resFailed.status()).isEqualTo(DeliveryResult.Status.FAILED);
        assertThat(resFailed.errorCode()).isEqualTo("101");
        assertThat(resFailed.providerRef()).isEqualTo("esms");
    }

    @Test
    @DisplayName("Ticket 6: SmsType=2 mà brandname trống -> tự hạ về 8 trong JSON payload gửi đi")
    void testSmsTypeFallbackTo8WhenBrandnameBlank() {
        FakeHttpClient fakeClient = new FakeHttpClient((req, attempt) ->
                new FakeHttpResponse(200, "{\"CodeResult\":\"100\",\"SMSID\":\"12345\"}", req));

        RecordingSmsServiceImpl service = new RecordingSmsServiceImpl();
        configureService(service, fakeClient, 2, "");

        DeliveryResult res = service.sendNotification("0912345678", "Noi dung test");
        assertThat(res.status()).isEqualTo(DeliveryResult.Status.SENT);
        assertThat(fakeClient.requests).hasSize(1);
        assertThat(fakeClient.requestBodies.get(0)).contains("\"SmsType\":\"8\"");
        assertThat(fakeClient.requestBodies.get(0)).doesNotContain("\"Brandname\"");

        // Khi có brandname -> giữ nguyên SmsType=2 và gửi kèm Brandname
        FakeHttpClient clientWithBrand = new FakeHttpClient((req, attempt) ->
                new FakeHttpResponse(200, "{\"CodeResult\":\"100\",\"SMSID\":\"12346\"}", req));
        RecordingSmsServiceImpl serviceWithBrand = new RecordingSmsServiceImpl();
        configureService(serviceWithBrand, clientWithBrand, 2, "ChucTu");

        DeliveryResult resBrand = serviceWithBrand.sendNotification("0912345678", "Noi dung test");
        assertThat(resBrand.status()).isEqualTo(DeliveryResult.Status.SENT);
        assertThat(clientWithBrand.requestBodies.get(0)).contains("\"SmsType\":\"2\"");
        assertThat(clientWithBrand.requestBodies.get(0)).contains("\"Brandname\":\"ChucTu\"");
    }

    @Test
    @DisplayName("Ticket 3 & 6: Retry tối đa 2 lần backoff 2s->5s cho lỗi mạng/timeout; không retry lỗi 101/103")
    void testRetryBehaviorRealExecution() {
        // Kịch bản A: 2 lần đầu HttpTimeoutException, lần 3 thành công -> retry đúng 2 lần với backoff 2000ms -> 5000ms
        FakeHttpClient timeoutThenSuccessClient = new FakeHttpClient((req, attempt) -> {
            if (attempt <= 2) {
                throw new HttpTimeoutException("request timed out");
            }
            return new FakeHttpResponse(200, "{\"CodeResult\":\"100\",\"SMSID\":\"777\"}", req);
        });
        RecordingSmsServiceImpl svcA = new RecordingSmsServiceImpl();
        configureService(svcA, timeoutThenSuccessClient, 8, "");

        DeliveryResult resA = svcA.sendNotification("0912345678", "Nhac lich");
        assertThat(resA.status()).isEqualTo(DeliveryResult.Status.SENT);
        assertThat(resA.errorCode()).isNull();
        assertThat(timeoutThenSuccessClient.requests).hasSize(3);
        assertThat(svcA.recordedSleeps).containsExactly(2000L, 5000L);

        // Kịch bản B: ConnectException cả 3 lần -> dừng sau 2 lần retry, trả FAILED với CONNECT_ERROR
        FakeHttpClient connectFailClient = new FakeHttpClient((req, attempt) -> {
            throw new ConnectException("Connection refused");
        });
        RecordingSmsServiceImpl svcB = new RecordingSmsServiceImpl();
        configureService(svcB, connectFailClient, 8, "");

        DeliveryResult resB = svcB.sendNotification("0912345678", "Nhac lich");
        assertThat(resB.status()).isEqualTo(DeliveryResult.Status.FAILED);
        assertThat(resB.errorCode()).isEqualTo("CONNECT_ERROR");
        assertThat(connectFailClient.requests).hasSize(3);
        assertThat(svcB.recordedSleeps).containsExactly(2000L, 5000L);

        // Kịch bản C: eSMS trả lỗi 101 (sai key) -> chỉ gọi 1 lần, KHÔNG retry
        FakeHttpClient authFailClient = new FakeHttpClient((req, attempt) ->
                new FakeHttpResponse(200, "{\"CodeResult\":\"101\",\"ErrorMessage\":\"Invalid key\"}", req));
        RecordingSmsServiceImpl svcC = new RecordingSmsServiceImpl();
        configureService(svcC, authFailClient, 8, "");

        DeliveryResult resC = svcC.sendNotification("0912345678", "Nhac lich");
        assertThat(resC.status()).isEqualTo(DeliveryResult.Status.FAILED);
        assertThat(resC.errorCode()).isEqualTo("101");
        assertThat(authFailClient.requests).hasSize(1);
        assertThat(svcC.recordedSleeps).isEmpty();

        // Kịch bản D: eSMS trả lỗi 103 (hết số dư) -> chỉ gọi 1 lần, KHÔNG retry
        FakeHttpClient balanceFailClient = new FakeHttpClient((req, attempt) ->
                new FakeHttpResponse(200, "{\"CodeResult\":\"103\",\"ErrorMessage\":\"Not enough balance\"}", req));
        RecordingSmsServiceImpl svcD = new RecordingSmsServiceImpl();
        configureService(svcD, balanceFailClient, 8, "");

        DeliveryResult resD = svcD.sendNotification("0912345678", "Nhac lich");
        assertThat(resD.status()).isEqualTo(DeliveryResult.Status.FAILED);
        assertThat(resD.errorCode()).isEqualTo("103");
        assertThat(balanceFailClient.requests).hasSize(1);
        assertThat(svcD.recordedSleeps).isEmpty();
    }

    @Test
    @DisplayName("Ticket 5: SĐT sai định dạng hoặc cổng tắt -> SKIPPED, không gọi API")
    void testInvalidPhoneAndDisabledGatewaySkipWithoutCallingApi() {
        FakeHttpClient client = new FakeHttpClient((req, attempt) ->
                new FakeHttpResponse(200, "{\"CodeResult\":\"100\"}", req));
        RecordingSmsServiceImpl service = new RecordingSmsServiceImpl();
        configureService(service, client, 8, "");

        // SĐT sai định dạng (đầu số 01 hoặc quá ngắn) dù cổng đang bật -> SKIPPED + INVALID_PHONE, không gọi HTTP
        DeliveryResult invalidRes = service.sendNotification("0123456789", "Test");
        assertThat(invalidRes.status()).isEqualTo(DeliveryResult.Status.SKIPPED);
        assertThat(invalidRes.errorCode()).isEqualTo("INVALID_PHONE");
        assertThat(client.requests).isEmpty();

        DeliveryResult shortRes = service.sendNotification("12345", "Test");
        assertThat(shortRes.status()).isEqualTo(DeliveryResult.Status.SKIPPED);
        assertThat(shortRes.errorCode()).isEqualTo("INVALID_PHONE");
        assertThat(client.requests).isEmpty();

        // Cổng tắt -> SKIPPED, không gọi HTTP
        ReflectionTestUtils.setField(service, "smsEnabled", false);
        DeliveryResult disabledRes = service.sendNotification("0912345678", "Test");
        assertThat(disabledRes.status()).isEqualTo(DeliveryResult.Status.SKIPPED);
        assertThat(disabledRes.errorCode()).isNull();
        assertThat(client.requests).isEmpty();
    }
}
