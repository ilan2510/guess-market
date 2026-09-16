package guessmarket.engine;

import java.util.ArrayList;
import java.util.List;

public class EventInfo
{

    private final int id;
    private final String name;
    private final String description;
    private final int commissionPercent;
    private final CommissionType commissionType;
    private final String[] optionNames;
    private final String mmUserName;
    private final TradingMethod method;
    private final EventStatus status;
    private final double accountBalance;
    private final double totalFeesCollected;
    private final int winningOptionIndex;

    // LMSR fields (meaningful only when method == LMSR)
    private final int liquidityB;
    private final double[] optionPrices;
    private final int[] optionQuantities;
    private final List<TradeInfo> tradeHistory;

    // Order Book fields (meaningful only when method == ORDER_BOOK)
    private final int baseValueD;
    private final boolean allowMint;
    private final List<OrderInfo> orderBookOption0;
    private final List<OrderInfo> orderBookOption1;
    private final PriceStatsInfo[] priceStats;
    private final List<ParticipantInfo> participants;

    private EventInfo(int id, String name, String description, int commissionPercent, CommissionType commissionType,
                       String[] optionNames, String mmUserName, TradingMethod method, EventStatus status,
                       double accountBalance, double totalFeesCollected, int winningOptionIndex,
                       int liquidityB, double[] optionPrices, int[] optionQuantities, List<TradeInfo> tradeHistory,
                       int baseValueD, boolean allowMint, List<OrderInfo> orderBookOption0,
                       List<OrderInfo> orderBookOption1, PriceStatsInfo[] priceStats, List<ParticipantInfo> participants)
    {
        this.id = id;
        this.name = name;
        this.description = description;
        this.commissionPercent = commissionPercent;
        this.commissionType = commissionType;
        this.optionNames = optionNames;
        this.mmUserName = mmUserName;
        this.method = method;
        this.status = status;
        this.accountBalance = accountBalance;
        this.totalFeesCollected = totalFeesCollected;
        this.winningOptionIndex = winningOptionIndex;

        this.liquidityB = liquidityB;
        this.optionPrices = optionPrices;
        this.optionQuantities = optionQuantities;
        this.tradeHistory = tradeHistory;

        this.baseValueD = baseValueD;
        this.allowMint = allowMint;
        this.orderBookOption0 = orderBookOption0;
        this.orderBookOption1 = orderBookOption1;
        this.priceStats = priceStats;
        this.participants = participants;
    }

    public static EventInfo forLmsr(int id, String name, String description, int commissionPercent,
            CommissionType commissionType, String[] optionNames, String mmUserName, EventStatus status,
            double accountBalance, double totalFeesCollected, int winningOptionIndex,
            int liquidityB, double[] optionPrices, int[] optionQuantities, List<TradeInfo> tradeHistory)
    {
        return new EventInfo(id, name, description, commissionPercent, commissionType, optionNames, mmUserName,
                TradingMethod.LMSR, status, accountBalance, totalFeesCollected, winningOptionIndex,
                liquidityB, optionPrices, optionQuantities, tradeHistory,
                0, false, new ArrayList<>(), new ArrayList<>(), new PriceStatsInfo[2], new ArrayList<>());
    }

    public static EventInfo forOrderBook(int id, String name, String description, int commissionPercent,
            CommissionType commissionType, String[] optionNames, String mmUserName, EventStatus status,
            double accountBalance, double totalFeesCollected, int winningOptionIndex,
            int baseValueD, boolean allowMint, List<OrderInfo> orderBookOption0, List<OrderInfo> orderBookOption1,
            PriceStatsInfo[] priceStats, List<ParticipantInfo> participants)
    {
        return new EventInfo(id, name, description, commissionPercent, commissionType, optionNames, mmUserName,
                TradingMethod.ORDER_BOOK, status, accountBalance, totalFeesCollected, winningOptionIndex,
                0, new double[2], new int[2], new ArrayList<>(),
                baseValueD, allowMint, orderBookOption0, orderBookOption1, priceStats, participants);
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

    public String getMmUserName()
    {
        return mmUserName;
    }

    public TradingMethod getMethod()
    {
        return method;
    }

    public EventStatus getStatus()
    {
        return status;
    }

    public double getAccountBalance()
    {
        return accountBalance;
    }

    public double getTotalFeesCollected()
    {
        return totalFeesCollected;
    }

    public int getWinningOptionIndex()
    {
        return winningOptionIndex;
    }

    public boolean isClosed()
    {
        return status == EventStatus.CLOSED;
    }

    public int getLiquidityB()
    {
        return liquidityB;
    }

    public double getCurrentPrice(int optionIndex)
    {
        return optionPrices[optionIndex];
    }

    public int getQuantity(int optionIndex)
    {
        return optionQuantities[optionIndex];
    }

    public List<TradeInfo> getTradeHistory()
    {
        return tradeHistory;
    }

    public int getBaseValueD()
    {
        return baseValueD;
    }

    public boolean isAllowMint()
    {
        return allowMint;
    }

    public List<OrderInfo> getOrderBook(int optionIndex)
    {
        if (optionIndex == 0)
        {
            return orderBookOption0;
        }
        return orderBookOption1;
    }

    public PriceStatsInfo getPriceStats(int optionIndex)
    {
        return priceStats[optionIndex];
    }

    public List<ParticipantInfo> getParticipants()
    {
        return participants;
    }
}
