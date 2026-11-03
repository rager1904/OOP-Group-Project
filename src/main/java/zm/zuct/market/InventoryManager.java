package zm.zuct.market;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/** One process owns the file. Synchronized operations publish state only after durable saving. */
public final class InventoryManager implements AutoCloseable {
    /** All saved records grouped into one file. Getters return read-only collections. */
    public static class State {
        private final Map<String, Product> products;
        private final List<StockPurchase> purchases;
        private final List<Sale> sales;

        @com.fasterxml.jackson.annotation.JsonCreator
        public State(@com.fasterxml.jackson.annotation.JsonProperty("products") Map<String, Product> products,
                     @com.fasterxml.jackson.annotation.JsonProperty("purchases") List<StockPurchase> purchases,
                     @com.fasterxml.jackson.annotation.JsonProperty("sales") List<Sale> sales) {
            this.products = Collections.unmodifiableMap(new HashMap<>(products));
            this.purchases = Collections.unmodifiableList(new ArrayList<>(purchases));
            this.sales = Collections.unmodifiableList(new ArrayList<>(sales));
        }
        public Map<String, Product> getProducts() { return products; }
        public List<StockPurchase> getPurchases() { return purchases; }
        public List<Sale> getSales() { return sales; }
    }
    private final Path file;
    private final ObjectMapper mapper = new ObjectMapper();
    private final FileChannel lockChannel;
    private final FileLock lock;
    private State state;
    public InventoryManager(Path file) throws IOException {
        this.file = file.toAbsolutePath();
        Files.createDirectories(this.file.getParent());
        lockChannel = FileChannel.open(this.file.resolveSibling("inventory.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        lock = lockChannel.tryLock();
        if (lock == null) { lockChannel.close(); throw new IOException("Another tracker is using this data folder."); }
        try {
            state = Files.exists(this.file) ? mapper.readValue(this.file.toFile(), State.class) : new State(Map.of(), List.of(), List.of());
        } catch (IOException | RuntimeException e) { close(); throw e; }
    }
    public synchronized State snapshot() { return state; }
    public synchronized Product find(String id) {
        if (id == null || id.isBlank()) throw new BusinessException("Product ID is required.");
        Product p = state.getProducts().get(id);
        if (p == null) throw new BusinessException("Product not found.");
        return p;
    }
    public synchronized Product saveProduct(String id, String name, long buy, long sell) throws IOException {
        Product old = id == null ? null : find(id);
        Product p = new Product(old == null ? UUID.randomUUID().toString() : id, name, buy, sell, old == null ? 0 : old.getQuantity());
        for (Product existing : state.getProducts().values()) {
            if (!existing.getId().equals(p.getId()) && existing.getName().equalsIgnoreCase(p.getName())) {
                throw new BusinessException("A product with this name already exists.");
            }
        }
        Map<String, Product> products = new HashMap<>(state.getProducts()); products.put(p.getId(), p);
        commit(new State(products, state.getPurchases(), state.getSales())); return p;
    }
    public synchronized Object record(String id, int quantity, boolean purchase) throws IOException {
        if (quantity < 1 || quantity > 1_000_000) throw new BusinessException("Quantity must be between 1 and 1,000,000.");
        Product p = find(id);
        String date = LocalDate.now(ZoneId.of("Africa/Lusaka")).toString();
        StockMovement movement = purchase
            ? new StockPurchase(UUID.randomUUID().toString(), id, p.getName(), quantity, p.getBuyingPrice(), date)
            : new Sale(UUID.randomUUID().toString(), id, p.getName(), quantity, p.getSellingPrice(), p.getBuyingPrice(), date);
        Map<String, Product> products = new HashMap<>(state.getProducts());
        // Runtime dispatch: purchase adds stock; sale subtracts it through the same call.
        products.put(id, p.adjustStock(movement.stockChange()));
        List<StockPurchase> purchases = new ArrayList<>(state.getPurchases());
        List<Sale> sales = new ArrayList<>(state.getSales());
        if (purchase) {
            purchases.add((StockPurchase) movement);
        } else {
            sales.add((Sale) movement);
        }
        commit(new State(products, purchases, sales)); return movement;
    }
    private void commit(State next) throws IOException {
        Path temporary = Files.createTempFile(file.getParent(), "inventory-", ".tmp");
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), next);
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) { channel.force(true); }
            // Fail safely on filesystems without atomic replacement rather than risking a partial file.
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            state = next;
        } finally { Files.deleteIfExists(temporary); }
    }
    @Override public void close() throws IOException { if (lock != null && lock.isValid()) lock.release(); lockChannel.close(); }
}
