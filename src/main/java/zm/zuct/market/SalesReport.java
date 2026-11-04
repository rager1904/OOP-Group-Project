package zm.zuct.market;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SalesReport {
    private final long revenue;
    private final long costOfGoodsSold;
    private final long grossProfit;
    private final long remainingStockCost;
    private final long remainingStockRetailValue;
    private final long saleCount;

    @JsonCreator
    public SalesReport(@JsonProperty("revenue") long revenue,
            @JsonProperty("costOfGoodsSold") long costOfGoodsSold,
            @JsonProperty("grossProfit") long grossProfit,
            @JsonProperty("remainingStockCost") long remainingStockCost,
            @JsonProperty("remainingStockRetailValue") long remainingStockRetailValue,
            @JsonProperty("saleCount") long saleCount) {
        this.revenue = revenue;
        this.costOfGoodsSold = costOfGoodsSold;
        this.grossProfit = grossProfit;
        this.remainingStockCost = remainingStockCost;
        this.remainingStockRetailValue = remainingStockRetailValue;
        this.saleCount = saleCount;
    }

    public long getRevenue() { return revenue; }
    public long getCostOfGoodsSold() { return costOfGoodsSold; }
    public long getGrossProfit() { return grossProfit; }
    public long getRemainingStockCost() { return remainingStockCost; }
    public long getRemainingStockRetailValue() { return remainingStockRetailValue; }
    public long getSaleCount() { return saleCount; }

    public static SalesReport generate(InventoryManager.State state, java.time.LocalDate from, java.time.LocalDate to) {
        if (from.isAfter(to)) throw new BusinessException("Start date must be on or before end date.");
        long revenue = 0;
        long cost = 0;
        long count = 0;
        for (Sale sale : state.getSales()) {
            java.time.LocalDate date = java.time.LocalDate.parse(sale.getDate());
            if (!date.isBefore(from) && !date.isAfter(to)) {
                revenue += sale.revenue();
                cost += sale.cost();
                count++;
            }
        }
        long stockCost = 0;
        long stockRetail = 0;
        for (Product product : state.getProducts().values()) {
            stockCost += product.getBuyingPrice() * product.getQuantity();
            stockRetail += product.getSellingPrice() * product.getQuantity();
        }
        return new SalesReport(revenue, cost, revenue - cost, stockCost, stockRetail, count);
    }

}
//BY Natasha Mutale 2421007
