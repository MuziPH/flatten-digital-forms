package za.co.ocr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;
import za.co.ocr.dto.CreatePaymentTransactionResponse;
import za.co.ocr.model.EmailInfo;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.ssl.SSLContextBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class LinkDocCarrierService {
    private static final Logger log = LoggerFactory.getLogger(LinkDocCarrierService.class);

    private final RestClient restClient;
    private final String basicAuthHeader;
    private final String bpmUrl;

    public LinkDocCarrierService(
            @Value("${bpm.url}") String bpmUrl,
            @Value("${bpm.auth}") String bpmAuth) {
        this.bpmUrl = bpmUrl;
        this.basicAuthHeader = bpmAuth;
        log.debug("BPM auth header loaded, length={}", bpmAuth.length());
        try {
            var sslContext = SSLContextBuilder.create().loadTrustMaterial((chain, authType) -> true).build();
            var httpClient = HttpClients.custom()
                    .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                            .setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create()
                                    .setSslContext(sslContext)
                                    .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                                    .build())
                            .build())
                    .build();
            this.restClient = RestClient.builder()
                    .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create SSL-trusting RestClient", e);
        }
    }

    public CreatePaymentTransactionResponse createPaymentTransaction(String traceId, EmailInfo emailInfo) {
        Map<String, Object> ocrPayloadRequest = new HashMap<>();
        ocrPayloadRequest.put("ocrMessages", List.of("LinkDocCarrier"));
        ocrPayloadRequest.put("attachments", null);
        ocrPayloadRequest.put("emailInfo", Map.of(
                "emailReceiver", emailInfo.getEmailReceiver(),
                "emailSender", emailInfo.getEmailSender(),
                "emailSubject", emailInfo.getEmailSubject(),
                "emailReceivedOn", emailInfo.getEmailReceivedOn()
        ));

        Map<String, Object> requestBody = Map.of(
                "traceID", traceId,
                "channelId", UUID.randomUUID().toString(),
                "ocrPayloadRequest", ocrPayloadRequest,
                "autoDocJson", ""
        );

        log.debug("Calling BPM createPaymentTransaction for traceId={} to URL: {}", traceId, bpmUrl);

        try {
            CreatePaymentTransactionResponse response = restClient.post()
                    .uri(bpmUrl)
                    .header("Authorization", basicAuthHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(CreatePaymentTransactionResponse.class);

            log.debug("BPM response status={}, workflowRef={}",
                    response != null ? response.getStatus() : null,
                    response != null && response.getData() != null && response.getData().getData() != null
                            ? response.getData().getData().getWorkflowReferenceNumber() : null);

            return response;
        } catch (HttpClientErrorException e) {
            log.error("BPM service returned HTTP {} error for traceId={}: {}",
                    e.getStatusCode(), traceId, e.getResponseBodyAsString(), e);

            if (e.getStatusCode().value() == 401) {
                log.error("Authentication failed. Verify BPM_AUTH environment variable is correctly set.");
            }
            throw e;
        } catch (Exception e) {
            log.error("Failed to call BPM service for traceId={}: {}", traceId, e.getMessage(), e);
            throw e;
        }
    }
}
