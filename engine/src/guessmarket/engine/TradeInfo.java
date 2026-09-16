package guessmarket.engine;

public class TradeInfo
{

    private final String userName;
    private final String optionName;
    private final int quantity;
    private final double pricePaid;
    private final double fee;

    public TradeInfo(String userName, String optionName, int quantity, double pricePaid, double fee)
    {
        this.userName = userName;
        this.optionName = optionName;
        this.quantity = quantity;
        this.pricePaid = pricePaid;
        this.fee = fee;
    }

    public String getUserName()
    {
        return userName;
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

    public double getFee()
    {
        return fee;
    }
}
