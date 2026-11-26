package org.delcom.ifs25009springbootcashflow.adapter.ai;

import org.delcom.ifs25009springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25009springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25009springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25009springbootcashflow.domain.repository.IFinanceAiService;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Adapter HTTP ke API OpenAI-compatible (/chat/completions). */
public class OpenAiFinanceService implements IFinanceAiService {

    /** Pengirim HTTP yang bisa diganti saat unit test. */
    @FunctionalInterface
    public interface HttpSender {
        String post(String url, String apiKey, String jsonBody) throws Exception;
    }

    static final String SYSTEM_PROMPT =
            "Kamu adalah asisten pencatat keuangan pribadi. Ubah teks pengguna menjadi daftar transaksi. "
            + "Balas HANYA dengan array JSON tanpa penjelasan, tiap elemen berbentuk "
            + "{\"description\": string, \"amount\": bilangan bulat rupiah > 0, \"type\": \"INCOME\" atau \"EXPENSE\"}.";

    private final String endpoint;
    private final String apiKey;
    private final String model;
    private final HttpSender sender;

    public OpenAiFinanceService(String endpoint, String apiKey, String model) {
        this(endpoint, apiKey, model, defaultSender());
    }

    public OpenAiFinanceService(String endpoint, String apiKey, String model, HttpSender sender) {
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.model = model;
        this.sender = sender;
    }

    public static HttpSender defaultSender() {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
        return (url, key, body) -> {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(90))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + key)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IOException("HTTP " + response.statusCode());
            }
            return response.body();
        };
    }

    String buildBody(String prompt) {
        return "{\"model\":" + MiniJson.quote(model) + ",\"temperature\":0.2,\"messages\":["
                + "{\"role\":\"system\",\"content\":" + MiniJson.quote(SYSTEM_PROMPT) + "},"
                + "{\"role\":\"user\",\"content\":" + MiniJson.quote(prompt) + "}]}";
    }

    @Override
    public List<AiGeneratedTransaction> generateTransactions(String prompt) throws DomainException {
        String url = (endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint)
                + "/chat/completions";
        String raw;
        try {
            raw = sender.post(url, apiKey, buildBody(prompt));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DomainException("Permintaan AI dibatalkan");
        } catch (Exception e) {
            throw new DomainException("Gagal menghubungi layanan AI: " + e.getMessage());
        }
        try {
            return parse(raw);
        } catch (RuntimeException e) {
            throw new DomainException("Respons AI tidak dapat diproses: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    List<AiGeneratedTransaction> parse(String raw) {
        Map<String, Object> root = (Map<String, Object>) MiniJson.parse(raw);
        List<Object> choices = (List<Object>) root.get("choices");
        Map<String, Object> message = (Map<String, Object>) ((Map<String, Object>) choices.get(0)).get("message");
        String content = (String) message.get("content");
        int start = content.indexOf('[');
        int end = content.lastIndexOf(']');
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("array JSON tidak ditemukan");
        }
        List<Object> items = (List<Object>) MiniJson.parse(content.substring(start, end + 1));
        List<AiGeneratedTransaction> result = new ArrayList<>();
        for (Object o : items) {
            Map<String, Object> item = (Map<String, Object>) o;
            String description = (String) item.get("description");
            long amount = ((Number) item.get("amount")).longValue();
            TransactionType type = TransactionType.valueOf(((String) item.get("type")).trim().toUpperCase(Locale.ROOT));
            result.add(new AiGeneratedTransaction(description, amount, type));
        }
        return result;
    }
}
