package org.delcom.ifs25009springbootcashflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.delcom.ifs25009springbootcashflow.adapter.ai.InMemoryFinanceAiService;
import org.delcom.ifs25009springbootcashflow.adapter.ai.MiniJson;
import org.delcom.ifs25009springbootcashflow.adapter.ai.OpenAiFinanceService;
import org.delcom.ifs25009springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25009springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25009springbootcashflow.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

class AiAdapterTest {
    private static String response(String content) {
        return "{\"choices\":[{\"message\":{\"content\":" + MiniJson.quote(content) + "}}]}";
    }

    @Test
    void inMemoryStub() {
        InMemoryFinanceAiService stub = new InMemoryFinanceAiService();
        assertEquals(1, stub.generateTransactions("beli kopi").size());
        List<AiGeneratedTransaction> withSalary = stub.generateTransactions("Gaji bulan ini");
        assertEquals(2, withSalary.size());
        assertEquals(TransactionType.INCOME, withSalary.get(0).type());
    }

    @Test
    void miniJsonParsesAllValueTypes() {
        Object v = MiniJson.parse(" {\"a\": [1, -2.5e1, true, false, null, \"x\\n\\r\\t\\b\\f\\\"\\\\\\/\\u0041\"], \"b\": {}, \"c\": []} ");
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) v;
        @SuppressWarnings("unchecked")
        List<Object> a = (List<Object>) map.get("a");
        assertEquals(1.0, a.get(0));
        assertEquals(-25.0, a.get(1));
        assertEquals(Boolean.TRUE, a.get(2));
        assertEquals(Boolean.FALSE, a.get(3));
        assertNull(a.get(4));
        assertEquals("x\n\r\t\b\f\"\\/A", a.get(5));
        assertTrue(((Map<?, ?>) map.get("b")).isEmpty());
        assertTrue(((List<?>) map.get("c")).isEmpty());
    }

    @Test
    void miniJsonRejectsInvalidInput() {
        for (String bad : new String[]{"", "   ", "{", "[", "[1", "[1,", "[1 2]", "{\"a\" 1}", "{1:2}",
                "{\"a\":1", "{\"a\":1 \"b\":2}", "\"abc", "\"abc\\", "\"\\u12\"", "@", "[1] x", "{\"a\":1,"}) {
            assertThrows(IllegalArgumentException.class, () -> MiniJson.parse(bad), bad);
        }
        assertEquals(42.0, MiniJson.parse("42"));
    }

    @Test
    void miniJsonQuote() {
        assertEquals("\"a\\\"b\\\\c\\n\\r\\t\\u0001d\"", MiniJson.quote("a\"b\\c\n\r\t\u0001d"));
        assertEquals("\"\"", MiniJson.quote(""));
    }

    @Test
    void openAiParsesResponseWithCodeFence() throws Exception {
        AtomicReference<String> url = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        String content = "Berikut hasilnya:\n```json\n[{\"description\":\"Gaji\",\"amount\":5000000,\"type\":\"income\"},"
                + "{\"description\":\"Makan\",\"amount\":25000.0,\"type\":\"EXPENSE\"}]\n```";
        OpenAiFinanceService service = new OpenAiFinanceService("http://host/v1/", "KEY", "model-x", (u, k, b) -> {
            url.set(u);
            body.set(k + "|" + b);
            return response(content);
        });
        List<AiGeneratedTransaction> result = service.generateTransactions("gaji 5 juta, makan 25 ribu");
        assertEquals("http://host/v1/chat/completions", url.get());
        assertTrue(body.get().startsWith("KEY|{\"model\":\"model-x\""));
        assertTrue(body.get().contains("gaji 5 juta, makan 25 ribu"));
        assertEquals(2, result.size());
        assertEquals(new AiGeneratedTransaction("Gaji", 5_000_000, TransactionType.INCOME), result.get(0));
        assertEquals(new AiGeneratedTransaction("Makan", 25_000, TransactionType.EXPENSE), result.get(1));
        // endpoint tanpa garis miring di akhir
        new OpenAiFinanceService("http://host/v1", "K", "m", (u, k, b) -> {
            url.set(u);
            return response("[]");
        }).generateTransactions("x");
        assertEquals("http://host/v1/chat/completions", url.get());
    }

    @Test
    void openAiWrapsFailures() {
        OpenAiFinanceService network = new OpenAiFinanceService("http://h", "k", "m", (u, k, b) -> {
            throw new IOException("koneksi putus");
        });
        DomainException e = assertThrows(DomainException.class, () -> network.generateTransactions("x"));
        assertTrue(e.getMessage().contains("koneksi putus"));

        OpenAiFinanceService interrupted = new OpenAiFinanceService("http://h", "k", "m", (u, k, b) -> {
            throw new InterruptedException();
        });
        assertThrows(DomainException.class, () -> interrupted.generateTransactions("x"));
        assertTrue(Thread.interrupted()); // bersihkan flag interrupt

        for (String raw : new String[]{"{}", "tidak json", response("tanpa array"), response("["),
                response("[{\"description\":\"x\"}]"),
                response("[{\"description\":\"x\",\"amount\":1,\"type\":\"LAIN\"}]")}) {
            OpenAiFinanceService bad = new OpenAiFinanceService("http://h", "k", "m", (u, k, b) -> raw);
            assertThrows(DomainException.class, () -> bad.generateTransactions("x"), raw);
        }
    }

    @Test
    void defaultSenderTalksHttp() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> auth = new AtomicReference<>();
        server.createContext("/api/chat/completions", ex -> {
            auth.set(ex.getRequestHeaders().getFirst("Authorization"));
            byte[] out = response("[{\"description\":\"Kopi\",\"amount\":15000,\"type\":\"EXPENSE\"}]")
                    .getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, out.length);
            ex.getResponseBody().write(out);
            ex.close();
        });
        server.createContext("/err/chat/completions", ex -> {
            ex.sendResponseHeaders(500, -1);
            ex.close();
        });
        server.start();
        try {
            String base = "http://127.0.0.1:" + server.getAddress().getPort();
            List<AiGeneratedTransaction> result =
                    new OpenAiFinanceService(base + "/api", "secret", "m").generateTransactions("kopi");
            assertEquals("Bearer secret", auth.get());
            assertEquals("Kopi", result.get(0).description());
            DomainException e = assertThrows(DomainException.class,
                    () -> new OpenAiFinanceService(base + "/err", "secret", "m").generateTransactions("x"));
            assertTrue(e.getMessage().contains("HTTP 500"));
        } finally {
            server.stop(0);
        }
    }
}
