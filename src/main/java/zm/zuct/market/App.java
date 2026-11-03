package zm.zuct.market;

import io.javalin.Javalin;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

public final class App {
    public static class ProductInput {
        private String name;
        private BigDecimal buyingPrice;
        private BigDecimal sellingPrice;
        public ProductInput() {}
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public BigDecimal getBuyingPrice() { return buyingPrice; }
        public void setBuyingPrice(BigDecimal price) { this.buyingPrice = price; }
        public BigDecimal getSellingPrice() { return sellingPrice; }
        public void setSellingPrice(BigDecimal price) { this.sellingPrice = price; }
    }
    public static class MovementInput {
        private String productId;
        private int quantity;
        public MovementInput() {}
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }
    public static Javalin create(InventoryManager inventory) {
        return Javalin.create(config -> {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.disable(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_FLOAT_AS_INT);
            config.jsonMapper(new io.javalin.json.JavalinJackson(mapper, false));
            config.jetty.host = "127.0.0.1";
            config.http.maxRequestSize = 16_384L;
            config.staticFiles.add("/public");
            config.routes.before(ctx -> {
                if (!ctx.host().matches("(127\\.0\\.0\\.1|localhost)(:[0-9]+)?"))
                    throw new io.javalin.http.ForbiddenResponse("Use the local tracker address.");
                ctx.header("X-Content-Type-Options", "nosniff");
                ctx.header("Content-Security-Policy", "default-src 'self'; frame-ancestors 'none'; object-src 'none'; base-uri 'none'");
                if (ctx.path().startsWith("/api")) {
                    ctx.header("Cache-Control", "no-store");
                    if (ctx.method() != io.javalin.http.HandlerType.GET) {
                        String origin = ctx.header("Origin");
                        if (origin != null && !origin.equals(ctx.scheme() + "://" + ctx.host()))
                            throw new io.javalin.http.ForbiddenResponse("Cross-origin write blocked.");
                        if (ctx.contentType() == null || !ctx.contentType().split(";")[0].equalsIgnoreCase("application/json"))
                            throw new io.javalin.http.UnsupportedMediaTypeResponse("Send application/json.");
                        if (ctx.body().trim().equals("null")) throw new BusinessException("Send a JSON object with the required fields.");
                    }
                }
            });
            config.routes.get("/api/products", ctx -> ctx.json(inventory.snapshot().getProducts().values()));
            config.routes.get("/api/products/{id}", ctx -> ctx.json(inventory.find(ctx.pathParam("id"))));
            config.routes.post("/api/products", ctx -> {
                ProductInput input = ctx.bodyAsClass(ProductInput.class);
                if (input == null) throw new BusinessException("Product details are required.");
                ctx.status(201).json(inventory.saveProduct(null, input.getName(), Product.money(input.getBuyingPrice()), Product.money(input.getSellingPrice())));
            });
            config.routes.put("/api/products/{id}", ctx -> {
                ProductInput input = ctx.bodyAsClass(ProductInput.class);
                if (input == null) throw new BusinessException("Product details are required.");
                ctx.json(inventory.saveProduct(ctx.pathParam("id"), input.getName(), Product.money(input.getBuyingPrice()), Product.money(input.getSellingPrice())));
            });
            config.routes.post("/api/purchases", ctx -> { MovementInput input = ctx.bodyAsClass(MovementInput.class); if (input == null) throw new BusinessException("Transaction details are required."); ctx.status(201).json(inventory.record(input.getProductId(), input.getQuantity(), true)); });
            config.routes.post("/api/sales", ctx -> { MovementInput input = ctx.bodyAsClass(MovementInput.class); if (input == null) throw new BusinessException("Transaction details are required."); ctx.status(201).json(inventory.record(input.getProductId(), input.getQuantity(), false)); });
            config.routes.get("/api/purchases", ctx -> ctx.json(inventory.snapshot().getPurchases()));
            config.routes.get("/api/sales", ctx -> ctx.json(inventory.snapshot().getSales()));
            config.routes.get("/api/reports", ctx -> {
                String from = ctx.queryParam("from"), to = ctx.queryParam("to");
                ctx.json(SalesReport.generate(inventory.snapshot(), from == null || from.isBlank() ? LocalDate.MIN : LocalDate.parse(from), to == null || to.isBlank() ? LocalDate.MAX : LocalDate.parse(to)));
            });
            config.routes.exception(BusinessException.class, (e, ctx) -> ctx.status(400).json(Map.of("message", e.getMessage())));
            config.routes.exception(com.fasterxml.jackson.core.JsonProcessingException.class, (e, ctx) -> ctx.status(400).json(Map.of("message", "Invalid JSON fields or values. Check prices and whole-number quantities.")));
            config.routes.exception(DateTimeParseException.class, (e, ctx) -> ctx.status(400).json(Map.of("message", "Use dates in YYYY-MM-DD format.")));
            config.routes.exception(java.io.IOException.class, (e, ctx) -> { e.printStackTrace(); ctx.status(500).json(Map.of("message", "Could not save data. No change was applied.")); });
        });
    }
    public static void main(String[] args) throws Exception {
        InventoryManager inventory = new InventoryManager(Path.of("data/inventory.json"));
        Javalin app = create(inventory).start(7070);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> { app.stop(); try { inventory.close(); } catch (Exception e) { e.printStackTrace(); } }));
        System.out.println("Open http://127.0.0.1:7070");
    }
}
