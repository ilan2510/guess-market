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
    private String mmUserName;
    private final TradingMethod method;

    // LMSR-only fields (meaningful only when method == LMSR)
    private final int liquidityB;
    private final int[] quantities;
    private final List<Trade> tradeHistory;

    // Order Book-only fields (meaningful only when method == ORDER_BOOK)
    private final int baseValueD;
    private final boolean allowMint;
    private final int initialInvestment;
    private final List<Order> orderBook;
    private final List<Holding> holdings;
    private final Double[] lastTradePrice;

    private EventStatus status;
    private double accountBalance;
    private double totalFeesCollected;
    private int winningOptionIndex;

    private Event(int id, String name, String description, int commissionPercent, CommissionType commissionType,
                  String optionAName, String optionBName, TradingMethod method,
                  int liquidityB, int baseValueD, boolean allowMint, int initialInvestment)
    {
        this.id = id;
        this.name = name;
        this.description = description;
        this.commissionPercent = commissionPercent;
        this.commissionType = commissionType;
        this.optionNames = new String[] { optionAName, optionBName };
        this.mmUserName = null;
        this.method = method;

        this.liquidityB = liquidityB;
        this.quantities = new int[] { 0, 0 };
        this.tradeHistory = new ArrayList<>();

        this.baseValueD = baseValueD;
        this.allowMint = allowMint;
        this.initialInvestment = initialInvestment;
        this.orderBook = new ArrayList<>();
        this.holdings = new ArrayList<>();
        this.lastTradePrice = new Double[2];

        this.status = EventStatus.NOT_STARTED;
        this.accountBalance = 0;
        this.totalFeesCollected = 0;
        this.winningOptionIndex = -1;
    }

    public static Event createLmsrEvent(int id, String name, String description, int commissionPercent,
            CommissionType commissionType, String optionAName, String optionBName, int liquidityB)
    {
        return new Event(id, name, description, commissionPercent, commissionType, optionAName, optionBName,
                TradingMethod.LMSR, liquidityB, 0, false, 0);
    }

    public static Event createOrderBookEvent(int id, String name, String description, int commissionPercent,
            CommissionType commissionType, String optionAName, String optionBName,
            int baseValueD, boolean allowMint, int initialInvestment)
    {
        return new Event(id, name, description, commissionPercent, commissionType, optionAName, optionBName,
                TradingMethod.ORDER_BOOK, 0, baseValueD, allowMint, initialInvestment);
    }

    void assignMarketMaker(String userName)
    {
        this.mmUserName = userName;
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

    public int getBaseValueD()
    {
        return baseValueD;
    }

    public boolean isAllowMint()
    {
        return allowMint;
    }

    public int getInitialInvestment()
    {
        return initialInvestment;
    }

    public List<Order> getOrderBook()
    {
        return orderBook;
    }

    public List<Holding> getHoldings()
    {
        return holdings;
    }

    void setLastTradePrice(int optionIndex, double price)
    {
        lastTradePrice[optionIndex] = price;
    }

    public Holding findHolding(String userName, int optionIndex)
    {
        for (Holding holding : holdings)
        {
            if (holding.getUserName().equals(userName) && holding.getOptionIndex() == optionIndex)
            {
                return holding;
            }
        }
        return null;
    }

    Holding findOrCreateHolding(String userName, int optionIndex)
    {
        Holding existing = findHolding(userName, optionIndex);
        if (existing != null)
        {
            return existing;
        }
        Holding created = new Holding(userName, optionIndex);
        holdings.add(created);
        return created;
    }

    void recordLmsrPurchase(String userName, int optionIndex, int quantity, double pricePaid, double fee)
    {
        quantities[optionIndex] += quantity;
        tradeHistory.add(new Trade(userName, optionNames[optionIndex], quantity, pricePaid, fee));
    }

    void addToAccountBalance(double amount)
    {
        accountBalance += amount;
    }

    void addFeesCollected(double amount)
    {
        totalFeesCollected += amount;
    }

    void activate()
    {
        status = EventStatus.ACTIVE;
    }

    void close(int winningOptionIndex)
    {
        this.status = EventStatus.CLOSED;
        this.winningOptionIndex = winningOptionIndex;
    }

    public boolean isClosed()
    {
        return status == EventStatus.CLOSED;
    }

    public EventInfo toEventInfo()
    {
        if (method == TradingMethod.LMSR)
        {
            return buildLmsrEventInfo();
        }
        return buildOrderBookEventInfo();
    }

    private EventInfo buildLmsrEventInfo()
    {
        double[] optionPrices = { getCurrentPrice(0), getCurrentPrice(1) };
        int[] optionQuantitiesCopy = { quantities[0], quantities[1] };

        List<TradeInfo> tradeInfoHistory = new ArrayList<>();
        for (Trade trade : tradeHistory)
        {
            tradeInfoHistory.add(trade.toTradeInfo());
        }

        return EventInfo.forLmsr(id, name, description, commissionPercent, commissionType,
                copyOptionNames(), mmUserName, status, accountBalance, totalFeesCollected, winningOptionIndex,
                liquidityB, optionPrices, optionQuantitiesCopy, tradeInfoHistory);
    }

    private EventInfo buildOrderBookEventInfo()
    {
        List<OrderInfo> orderBookOption0 = new ArrayList<>();
        List<OrderInfo> orderBookOption1 = new ArrayList<>();
        for (Order order : orderBook)
        {
            if (order.getOptionIndex() == 0)
            {
                orderBookOption0.add(order.toOrderInfo());
            }
            else
            {
                orderBookOption1.add(order.toOrderInfo());
            }
        }

        PriceStatsInfo[] priceStats = { buildPriceStats(0), buildPriceStats(1) };
        List<ParticipantInfo> participants = buildParticipants(priceStats);

        return EventInfo.forOrderBook(id, name, description, commissionPercent, commissionType,
                copyOptionNames(), mmUserName, status, accountBalance, totalFeesCollected, winningOptionIndex,
                baseValueD, allowMint, orderBookOption0, orderBookOption1, priceStats, participants);
    }

    private String[] copyOptionNames()
    {
        return new String[] { optionNames[0], optionNames[1] };
    }

    private PriceStatsInfo buildPriceStats(int optionIndex)
    {
        Double last = lastTradePrice[optionIndex];
        Double bestBid = findBestOrderPrice(optionIndex, OrderSide.BUY, true);
        Double bestAsk = findBestOrderPrice(optionIndex, OrderSide.SELL, false);

        Double mid = null;
        Double spread = null;
        if (bestBid != null && bestAsk != null)
        {
            mid = (bestBid + bestAsk) / 2.0;
            spread = bestAsk - bestBid;
        }

        return new PriceStatsInfo(last, bestBid, bestAsk, mid, spread);
    }

    private Double findBestOrderPrice(int optionIndex, OrderSide side, boolean wantHighest)
    {
        Double best = null;
        for (Order order : orderBook)
        {
            if (order.getOptionIndex() != optionIndex || order.getSide() != side)
            {
                continue;
            }
            if (best == null)
            {
                best = order.getPrice();
            }
            else if (wantHighest && order.getPrice() > best)
            {
                best = order.getPrice();
            }
            else if (!wantHighest && order.getPrice() < best)
            {
                best = order.getPrice();
            }
        }
        return best;
    }

    private List<ParticipantInfo> buildParticipants(PriceStatsInfo[] priceStats)
    {
        double option0EstimatedPrice = estimatePrice(priceStats[0]);
        double option1EstimatedPrice = estimatePrice(priceStats[1]);

        List<String> participantNames = new ArrayList<>();
        for (Holding holding : holdings)
        {
            if (!participantNames.contains(holding.getUserName()) && holding.getQuantity() > 0)
            {
                participantNames.add(holding.getUserName());
            }
        }
        for (Order order : orderBook)
        {
            if (!participantNames.contains(order.getUserName()))
            {
                participantNames.add(order.getUserName());
            }
        }

        List<ParticipantInfo> participants = new ArrayList<>();
        for (String participantName : participantNames)
        {
            int quantity0 = getHoldingQuantity(participantName, 0);
            int quantity1 = getHoldingQuantity(participantName, 1);
            int[] quantities = { quantity0, quantity1 };
            double[] values = { quantity0 * option0EstimatedPrice, quantity1 * option1EstimatedPrice };
            double[] amountsPaid = { getHoldingAmountPaid(participantName, 0), getHoldingAmountPaid(participantName, 1) };
            double feePaid = getHoldingFeePaid(participantName, 0) + getHoldingFeePaid(participantName, 1);
            participants.add(new ParticipantInfo(participantName, quantities, values, amountsPaid, feePaid));
        }
        return participants;
    }

    private int getHoldingQuantity(String userName, int optionIndex)
    {
        Holding holding = findHolding(userName, optionIndex);
        if (holding == null)
        {
            return 0;
        }
        return holding.getQuantity();
    }

    private double getHoldingAmountPaid(String userName, int optionIndex)
    {
        Holding holding = findHolding(userName, optionIndex);
        if (holding == null)
        {
            return 0;
        }
        return holding.getAmountPaid();
    }

    private double getHoldingFeePaid(String userName, int optionIndex)
    {
        Holding holding = findHolding(userName, optionIndex);
        if (holding == null)
        {
            return 0;
        }
        return holding.getFeePaid();
    }

    private double estimatePrice(PriceStatsInfo stats)
    {
        if (stats.getMid() != null)
        {
            return stats.getMid();
        }
        if (stats.getLast() != null)
        {
            return stats.getLast();
        }
        return 0;
    }
}
