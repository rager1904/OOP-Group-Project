package zm.zuct.market;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Sale implements StockMovement {
    private final String id;
    private final String productId;
    private final String productName;
    private final int quantity;
    private final long unitPrice;
    private final long unitCost;
    private final String date;

    @JsonCreator
    public Sale(@JsonProperty("id") String id,
            @JsonProperty("productId") String productId,
            @JsonProperty("productName") String productName,
            @JsonProperty("quantity") int quantity,
            @JsonProperty("unitPrice") long unitPrice,
            @JsonProperty("unitCost") long unitCost,
            @JsonProperty("date") String date) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.unitCost = unitCost;
        this.date = date;
    }

    public String getId() { return id; }
    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public long getUnitPrice() { return unitPrice; }
    public long getUnitCost() { return unitCost; }
    public String getDate() { return date; }
    @Override public int stockChange() { return -quantity; }
    public long revenue() { return unitPrice * quantity; }
    public long cost() { return unitCost * quantity; }
}
