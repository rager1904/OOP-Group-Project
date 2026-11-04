package zm.zuct.market;

import java.math.BigDecimal;

/** Immutable snapshots prevent callers from bypassing stock controls. Money is in ngwee. */
public final class Product {
    private final String id;
    private final String name;
    private final long buyingPrice;
    private final long sellingPrice;
    private final int quantity;

    @com.fasterxml.jackson.annotation.JsonCreator
    public Product(@com.fasterxml.jackson.annotation.JsonProperty("id") String id,
                   @com.fasterxml.jackson.annotation.JsonProperty("name") String name,
                   @com.fasterxml.jackson.annotation.JsonProperty("buyingPrice") long buyingPrice,
                   @com.fasterxml.jackson.annotation.JsonProperty("sellingPrice") long sellingPrice,
                   @com.fasterxml.jackson.annotation.JsonProperty("quantity") int quantity) {
        if (id == null || id.isBlank() || name == null || name.isBlank() || name.trim().length() > 80)
            throw new BusinessException("Product name must contain 1 to 80 characters.");
        name = name.trim();
        if (buyingPrice < 0 || sellingPrice < 0 || buyingPrice > 100_000_000 || sellingPrice > 100_000_000)
            throw new BusinessException("Prices must be between 0 and 1,000,000 kwacha.");
        if (quantity < 0 || quantity > 1_000_000) throw new BusinessException("Stock must be between 0 and 1,000,000.");
        this.id = id;
        this.name = name;
        this.buyingPrice = buyingPrice;
        this.sellingPrice = sellingPrice;
        this.quantity = quantity;
    }
    public String getId() { return id; }
    public String getName() { return name; }
    public long getBuyingPrice() { return buyingPrice; }
    public long getSellingPrice() { return sellingPrice; }
    public int getQuantity() { return quantity; }
    public Product adjustStock(int change) {
        long next = (long) quantity + change;
        if (next < 0) throw new BusinessException("Insufficient stock.");
        if (next > 1_000_000) throw new BusinessException("Stock limit exceeded.");
        return new Product(id, name, buyingPrice, sellingPrice, (int) next);
    }
    public static long money(BigDecimal value) {
        if (value == null) throw new BusinessException("Price is required.");
        try { return value.movePointRight(2).longValueExact(); }
        catch (ArithmeticException e) { throw new BusinessException("Use a valid price with at most two decimal places."); }
    }
}

//BY FAITH BWALYA
