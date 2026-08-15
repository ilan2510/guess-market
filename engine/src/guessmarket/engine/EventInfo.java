package guessmarket.engine;

import java.util.List;

public class EventInfo
{

    private final int id;
    private final String name;
    private final String description;
    private final int commissionPercent;
    private final CommissionType commissionType;
    private final String[] optionNames;
    private final double[] optionPrices;
    private final int[] optionQuantities;
    private final double accountBalance;
    private final double totalFeesCollected;
    private final boolean closed;
    private final int winningOptionIndex;
    private final List<TradeInfo> tradeHistory;

    public EventInfo(int id, String name, String description, int commissionPercent, CommissionType commissionType,
                      String[] optionNames, double[] optionPrices, int[] optionQuantities,
                      double accountBalance, double totalFeesCollected,
                      boolean closed, int winningOptionIndex, List<TradeInfo> tradeHistory)
    {
        this.id = id;
        this.name = name;
        this.description = description;
        this.commissionPercent = commissionPercent;
        this.commissionType = commissionType;
        this.optionNames = optionNames;
        this.optionPrices = optionPrices;
        this.optionQuantities = optionQuantities;
        this.accountBalance = accountBalance;
        this.totalFeesCollected = totalFeesCollected;
        this.closed = closed;
        this.winningOptionIndex = winningOptionIndex;
        this.tradeHistory = tradeHistory;
    }

    public int getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public int getCommissionPercent()
    {
        return commissionPercent;
    }

    public CommissionType getCommissionType()
    {
        return commissionType;
    }

    public String getOptionName(int optionIndex)
    {
        return optionNames[optionIndex];
    }

    public double getCurrentPrice(int optionIndex)
    {
        return optionPrices[optionIndex];
    }

    public int getQuantity(int optionIndex)
    {
        return optionQuantities[optionIndex];
    }

    public double getAccountBalance()
    {
        return accountBalance;
    }

    public double getTotalFeesCollected()
    {
        return totalFeesCollected;
    }

    public boolean isClosed()
    {
        return closed;
    }

    public int getWinningOptionIndex()
    {
        return winningOptionIndex;
    }

    public List<TradeInfo> getTradeHistory()
    {
        return tradeHistory;
    }
}
