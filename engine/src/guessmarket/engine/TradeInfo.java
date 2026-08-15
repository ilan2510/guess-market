package guessmarket.engine;

// DTO: a plain, read-only copy of a trade, safe to hand out to the UI.
public class TradeInfo
{

    private final String optionName;
    private final int quantity;
    private final double pricePaid;

    public TradeInfo(String optionName, int quantity, double pricePaid)
    {
        this.optionName = optionName;
        this.quantity = quantity;
        this.pricePaid = pricePaid;
    }

    public String getOptionName()
    {
        return optionName;
    }

    public int getQuantity()
    {
        return quantity;
    }

    public double getPricePaid()
    {
        return pricePaid;
    }
}
