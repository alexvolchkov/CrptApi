package ru.job4j;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.concurrent.TimedSemaphore;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Aleksandr Volchkov
 */
public class CrptApi {

    private final static String URL_ADDRESS = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final static String CLIENT_TOKEN = "token";

    private final RequestPool requestPool;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        if (requestLimit <= 0) {
            throw new IllegalArgumentException("Request limit must be > 0");
        }
        this.requestPool = new RequestPool(timeUnit, requestLimit);
    }

    public void createDoc(Document document, String signature) throws JsonProcessingException, InterruptedException {
        requestPool.tryRequest();
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        String jsonDoc = objectMapper.writeValueAsString(document);
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpPost httpPost = new HttpPost(URL_ADDRESS);
            httpPost.addHeader("content-type", "application/json");
            httpPost.addHeader("Authorization", CLIENT_TOKEN);
            httpPost.addHeader("signature", signature);
            httpPost.setEntity(new StringEntity(jsonDoc));
            CloseableHttpResponse response = client.execute(httpPost);
            HttpEntity httpEntity = response.getEntity();
            /*
            Обработка httpEntity
            */
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @AllArgsConstructor
    @Getter
    public static class Document {

        private final Description description;

        @JsonProperty("doc_id")
        private final String docId;

        @JsonProperty("doc_status")
        private final String docStatus;

        @JsonProperty("doc_type")
        private final DocType docType;

        private final boolean importRequest;

        @JsonProperty("owner_inn")
        private final String ownerInn;

        @JsonProperty("participant_inn")
        private final String participantInn;

        @JsonProperty("producer_inn")
        private final String producerInn;

        @JsonProperty("production_date")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private final LocalDate productionDate;

        @JsonProperty("production_type")
        private final String productionType;

        private final List<Product> products;

        @JsonProperty("reg_date")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private final LocalDate regDate;

        @JsonProperty("reg_number")
        private final String regNumber;
    }

    @AllArgsConstructor
    @Getter
    public static class Description {

        private final String participantInn;

    }

    @AllArgsConstructor
    @Getter
    public static class Product {
        @JsonProperty("certificate_document")
        private final String certificateDocument;

        @JsonProperty("certificate_document_date")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private final LocalDate certificateDocumentDate;

        @JsonProperty("certificate_document_number")
        private final String certificateDocumentNumber;

        @JsonProperty("owner_inn")
        private final String ownerInn;

        @JsonProperty("producer_inn")
        private final String producerInn;

        @JsonProperty("production_date")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private final LocalDate productionDate;

        @JsonProperty("tnved_code")
        private final String tnvedCode;

        @JsonProperty("uit_code")
        private final String uitCode;

        @JsonProperty("uitu_code")
        private final String uituCode;

    }

    public static class RequestPool {
        private static final int TIME_PERIOD = 1;
        private final TimedSemaphore timedSemaphore;

        public RequestPool(TimeUnit timeUnit, int requestLimit) {
            this.timedSemaphore = new TimedSemaphore(TIME_PERIOD, timeUnit, requestLimit);
        }

        private void tryRequest() throws InterruptedException {
            timedSemaphore.acquire();
        }
    }
}

