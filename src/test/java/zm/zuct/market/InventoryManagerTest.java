package zm.zuct.market;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.LocalDate;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class InventoryManagerTest {
    @TempDir Path directory;
    @Test void stockProfitAndRestart() throws Exception {
        Path file = directory.resolve("inventory.json"); String id;
        try (var manager = new InventoryManager(file)) {
            var p = manager.saveProduct(null,"Tomatoes",500,800); id = p.getId();
            manager.record(id,10,true); manager.record(id,3,false);
            assertEquals(7,manager.find(id).getQuantity());
            manager.saveProduct(id,"Tomatoes",900,1200);
            var report = SalesReport.generate(manager.snapshot(),LocalDate.MIN,LocalDate.MAX);
            assertEquals(2400,report.getRevenue()); assertEquals(1500,report.getCostOfGoodsSold()); assertEquals(900,report.getGrossProfit());
            assertThrows(BusinessException.class, () -> manager.record(id,8,false));
            assertEquals(7,manager.find(id).getQuantity()); assertEquals(1,manager.snapshot().getSales().size());
        }
        try (var manager = new InventoryManager(file)) { assertEquals(7,manager.find(id).getQuantity()); assertEquals(1,manager.snapshot().getSales().size()); }
    }
    @Test void rejectsBadInputAndProtectsCollections() throws Exception {
        try (var manager = new InventoryManager(directory.resolve("inventory.json"))) {
            var p = manager.saveProduct(null,"Beans",100,200);
            assertThrows(BusinessException.class, () -> manager.saveProduct(null,"beans",100,200));
            assertThrows(BusinessException.class, () -> manager.record(p.getId(),0,true));
            assertThrows(BusinessException.class, () -> manager.record("missing",1,true));
            assertThrows(BusinessException.class, () -> manager.saveProduct(null,"Rice",-1,200));
            assertThrows(BusinessException.class, () -> Product.money(new BigDecimal("1.001")));
            assertThrows(UnsupportedOperationException.class, () -> manager.snapshot().getProducts().clear());
            assertThrows(BusinessException.class, () -> SalesReport.generate(manager.snapshot(),LocalDate.of(2026,2,1),LocalDate.of(2026,1,1)));
        }
    }
    @Test void simultaneousSalesCannotOversell() throws Exception {
        try (var manager = new InventoryManager(directory.resolve("inventory.json"))) {
            var p = manager.saveProduct(null,"Rice",100,200); manager.record(p.getId(),1,true);
            var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
            try {
                java.util.concurrent.Callable<Boolean> sale = () -> { try { manager.record(p.getId(),1,false); return true; } catch(BusinessException e) { return false; } };
                var results = pool.invokeAll(java.util.List.of(sale,sale));
                assertNotEquals(results.get(0).get(),results.get(1).get()); assertEquals(0,manager.find(p.getId()).getQuantity());
            } finally { pool.shutdownNow(); }
        }
    }
    @Test void corruptStorageIsNotSilentlyReset() throws Exception {
        var file = directory.resolve("inventory.json"); java.nio.file.Files.writeString(file,"broken json");
        assertThrows(java.io.IOException.class, () -> new InventoryManager(file));
        assertEquals("broken json", java.nio.file.Files.readString(file));
    }
    @Test void failedSaveDoesNotPublishNewState() throws Exception {
        Path file = directory.resolve("inventory.json");
        try (var manager = new InventoryManager(file)) {
            var p = manager.saveProduct(null,"Beans",100,200);
            java.nio.file.Files.delete(file);
            java.nio.file.Files.createDirectory(file);
            java.nio.file.Files.writeString(file.resolve("blocker"),"prevent replacement");
            assertThrows(java.io.IOException.class, () -> manager.record(p.getId(),3,true));
            assertEquals(0,manager.find(p.getId()).getQuantity()); assertTrue(manager.snapshot().getPurchases().isEmpty());
        }
    }
    @Test void interfaceDispatchHasOppositeStockEffects() {
        StockMovement purchase = new StockPurchase("p","id","Beans",4,100,"2026-11-02");
        StockMovement sale = new Sale("s","id","Beans",4,200,100,"2026-11-02");
        assertEquals(4,purchase.stockChange()); assertEquals(-4,sale.stockChange());
    }
}
