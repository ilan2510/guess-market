package guessmarket.engine;

import java.util.ArrayList;
import java.util.List;

public class Event
{

    private final int id;
    private final String name;
    private final String description;
    private final int commissionPercent;
    private final CommissionType commissionType;
    private final String[] optionNames;
    private final int liquidityB;

    private final int[] quantities;
    private final List<Trade> tradeHistory;
    private double accountBalance;
    private double totalFeesCollected;
    private boolean closed;
    private int winningOptionIndex;

    public Event(int id, String name, String description, int commissionPercent,
                 CommissionType commissionType, String optionAName, String optionBName, int liquidityB)
    {
        this.id = id;
        this.name = name;
        this.description = description;
        this.commissionPercent = commissionPercent;
        this.commissionType = commissionType;
        this.optionNames = new String[] { optionAName, optionBName };
        this.liquidityB = liquidityB;

        this.quantities = new int[] { 0, 0 };
        this.tradeHistory = new ArrayList<>();
        // The MM funds the LMSR subsidy up front: C(0,0) = b * ln(2)
        this.accountBalance = Lmsr.cost(0, 0, liquidityB);
        this.totalFeesCollected = 0;
        this.closed = false;
        this.winningOptionIndex = -1;
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

    public int getLiquidityB()
    {
        return liquidityB;
    }

    public int getQuantity(int optionIndex)
    {
        return quantities[optionIndex];
    }

    public double getCurrentPrice(int optionIndex)
    {
        // there are only 2 options (index 0 or 1), so "1 - optionIndex" always gives the other one
        int otherOptionIndex = 1 - optionIndex;
        return Lmsr.price(quantities[optionIndex], quantities[otherOptionIndex], liquidityB);
    }

    public List<Trade> getTradeHistory()
    {
        return tradeHistory;
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

    void recordPurchase(int optionIndex, int quantity, double pricePaid)
    {
        quantities[optionIndex] += quantity;
        tradeHistory.add(new Trade(optionNames[optionIndex], quantity, pricePaid));
    }

    void addToAccountBalance(double amount)
    {
        accountBalance += amount;
    }

    void addFeesCollected(double amount)
    {
        totalFeesCollected += amount;
    }

    void close(int winningOptionIndex)
    {
        this.closed = true;
        this.winningOptionIndex = winningOptionIndex;
    }

    // Builds a read-only snapshot of this event's current data, safe to hand out to the UI.
    public EventInfo toEventInfo()
    {
        String[] optionNamesCopy = { optionNames[0], optionNames[1] };
        double[] optionPrices = { getCurrentPrice(0), getCurrentPrice(1) };
        int[] optionQuantitiesCopy = { quantities[0], quantities[1] };

        List<TradeInfo> tradeInfoHistory = new ArrayList<>();
        for (Trade trade : tradeHistory)
        {
            tradeInfoHistory.add(trade.toTradeInfo());
        }

        return new EventInfo(id, name, description, commissionPercent, commissionType,
                optionNamesCopy, optionPrices, optionQuantitiesCopy,
                accountBalance, totalFeesCollected, closed, winningOptionIndex, tradeInfoHistory);
    }
}
