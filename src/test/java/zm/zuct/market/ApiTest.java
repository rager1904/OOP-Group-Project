package zm.zuct.market;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.net.URI;
import java.net.http.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;

class ApiTest {
    @TempDir Path directory;
    private final HttpClient client = HttpClient.newHttpClient();
    private HttpResponse<String> request(int port, String path, String method, String body, String origin) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
        if (origin != null) builder.header("Origin",origin);
        if (body != null) builder.header("Content-Type","application/json");
        return client.send(builder.method(method,body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
    @Test void completeHttpFlowAndSecurityChecks() throws Exception {
        try (var inventory = new InventoryManager(directory.resolve("inventory.json"))) {
            var app = App.create(inventory).start(0);
            try {
                int port = app.port();
                var home = request(port,"/","GET",null,null); assertEquals(200,home.statusCode()); assertTrue(home.body().contains("Market Stock"));
                assertEquals(200,request(port,"/app.js","GET",null,null).statusCode());
                var created = request(port,"/api/products","POST","{\"name\":\"Beans\",\"buyingPrice\":5,\"sellingPrice\":8}",null);
                assertEquals(201,created.statusCode(),created.body());
                String id = new ObjectMapper().readTree(created.body()).get("id").asText();
                String transaction = "{\"productId\":\"" + id + "\",\"quantity\":10}";
                assertEquals(201,request(port,"/api/purchases","POST",transaction,null).statusCode());
                assertEquals(201,request(port,"/api/sales","POST",transaction.replace(":10",":3"),null).statusCode());
                assertEquals(400,request(port,"/api/sales","POST",transaction,null).statusCode());
                var report = request(port,"/api/reports","GET",null,null); assertEquals(200,report.statusCode());
                assertEquals(900,new ObjectMapper().readTree(report.body()).get("grossProfit").asLong());
                assertEquals(200,request(port,"/api/products/"+id,"PUT","{\"name\":\"Dry beans\",\"buyingPrice\":6,\"sellingPrice\":9}",null).statusCode());
                assertEquals(200,request(port,"/api/products/"+id,"GET",null,null).statusCode());
                assertEquals(200,request(port,"/api/products","GET",null,null).statusCode());
                assertEquals(200,request(port,"/api/purchases","GET",null,null).statusCode());
                assertEquals(200,request(port,"/api/sales","GET",null,null).statusCode());
                assertEquals(403,request(port,"/api/sales","POST",transaction,"https://untrusted.example").statusCode());
                assertEquals(415,request(port,"/api/sales","POST",null,null).statusCode());
                assertEquals(400,request(port,"/api/sales","POST",transaction.replace(":10",":1.5"),null).statusCode());
                assertEquals(400,request(port,"/api/sales","POST","{}",null).statusCode());
                assertEquals(400,request(port,"/api/products","POST","null",null).statusCode());
                assertEquals(400,request(port,"/api/products","POST","broken",null).statusCode());
                assertEquals(400,request(port,"/api/reports?from=bad","GET",null,null).statusCode());
                assertTrue(home.headers().firstValue("Content-Security-Policy").orElse("").contains("frame-ancestors 'none'"));
            } finally { app.stop(); }
        }
    }
}
