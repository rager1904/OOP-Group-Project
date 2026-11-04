package zm.zuct.market;

/** Both transaction types use this contract; their stock effects differ. */
public interface StockMovement {
    int stockChange();
}

//By mwalula A chisanga  2420947