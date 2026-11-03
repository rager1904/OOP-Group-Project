package zm.zuct.market;

/** A predictable input or stock conflict, safe to show to the trader. */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) { super(message); }
}
