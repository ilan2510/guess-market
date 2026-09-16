package guessmarket.engine;

import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.InvalidEventFileException;

import java.util.ArrayList;
import java.util.List;

public class GuessMarketEngineImpl implements GuessMarketEngine
{

    private static final double WINNING_SHARE_PAYOUT = 1.0;

    private final EventFileLoader fileLoader;
    private List<Event> events;
    private List<User> users;

    public GuessMarketEngineImpl()
    {
        this.fileLoader = new EventFileLoader();
        this.events = new ArrayList<>();
        this.users = new ArrayList<>();
    }

    @Override
    public List<EventInfo> loadFile(String filePath) throws InvalidEventFileException
    {
        LoadResult result = fileLoader.load(filePath);
        this.events = result.getEvents();
        this.users = result.getUsers();
        return toEventInfoList(events);
    }

    @Override
    public List<UserInfo> getAllUsers() throws EngineException
    {
        requireUsersLoaded();
        List<UserInfo> result = new ArrayList<>();
        for (User user : users)
        {
            result.add(user.toUserInfo());
        }
        return result;
    }

    @Override
    public UserInfo getUserInfo(String userName) throws EngineException
    {
        return findUserByName(userName).toUserInfo();
    }

    @Override
    public List<EventInfo> getEventsForUser(String userName) throws EngineException
    {
        findUserByName(userName);
        List<EventInfo> result = new ArrayList<>();
        for (Event event : events)
        {
            boolean isMm = event.getMmUserName().equals(userName);
            boolean everParticipated = eventInvolvesUser(event, userName);
            boolean isActive = event.getStatus() == EventStatus.ACTIVE;
            if (isMm || everParticipated || isActive)
            {
                result.add(event.toEventInfo());
            }
        }
        return result;
    }

    @Override
    public List<EventInfo> getAllEvents() throws EngineException
    {
        requireEventsLoaded();
        return toEventInfoList(events);
    }

    @Override
    public EventInfo getEventInfo(int eventId) throws EngineException
    {
        return findEventById(eventId).toEventInfo();
    }

    @Override
    public void startEvent(int eventId, String actingUserName) throws EngineException
    {
        Event event = findEventById(eventId);
        User user = findUserByName(actingUserName);
        requireNotBlocked(user);

        if (!event.getMmUserName().equals(actingUserName))
        {
            throw new EngineException("Only " + event.getMmUserName() + " (the market maker) can start this event.");
        }
        if (event.getStatus() != EventStatus.NOT_STARTED)
        {
            throw new EngineException("This event has already been started.");
        }

        double requiredAmount;
        if (event.getMethod() == TradingMethod.LMSR)
        {
            requiredAmount = Lmsr.cost(0, 0, event.getLiquidityB());
        }
        else
        {
            requiredAmount = event.getInitialInvestment();
        }

        if (user.getBalance() < requiredAmount)
        {
            throw new EngineException("You need " + String.format("%.2f", requiredAmount)
                    + " to open this event, but your balance is only " + String.format("%.2f", user.getBalance()) + ".");
        }

        user.subtractFromBalance(requiredAmount);
        event.addToAccountBalance(requiredAmount);

        if (event.getMethod() == TradingMethod.ORDER_BOOK && event.getBaseValueD() > 0)
        {
            int pairs = event.getInitialInvestment() / event.getBaseValueD();
            double costPerOption = requiredAmount / 2.0;
            event.findOrCreateHolding(actingUserName, 0).addShares(pairs, costPerOption, 0);
            event.findOrCreateHolding(actingUserName, 1).addShares(pairs, costPerOption, 0);
        }

        event.activate();
    }

    @Override
    public void closeEvent(int eventId, String actingUserName, int winningOptionIndex) throws EngineException
    {
        Event event = findEventById(eventId);
        User user = findUserByName(actingUserName);
        requireNotBlocked(user);

        if (!event.getMmUserName().equals(actingUserName))
        {
            throw new EngineException("Only " + event.getMmUserName() + " (the market maker) can close this event.");
        }
        if (event.getStatus() != EventStatus.ACTIVE)
        {
            throw new EngineException("This event is not active, so it cannot be closed.");
        }
        validateOptionIndex(winningOptionIndex);

        if (event.getMethod() == TradingMethod.LMSR)
        {
            closeLmsrEvent(event, user, winningOptionIndex);
        }
        else
        {
            closeOrderBookEvent(event, user, winningOptionIndex);
        }
    }

    @Override
    public PurchaseResult buyShares(int eventId, String actingUserName, int optionIndex, int quantity) throws EngineException
    {
        Event event = findEventById(eventId);
        User user = findUserByName(actingUserName);
        requireNotBlocked(user);

        if (event.getMethod() != TradingMethod.LMSR)
        {
            throw new EngineException("This event uses Order Book trading, not LMSR. Submit an order instead.");
        }
        if (event.getStatus() != EventStatus.ACTIVE)
        {
            throw new EngineException("This event is not active, trading is not allowed right now.");
        }
        validateOptionIndex(optionIndex);
        if (quantity <= 0)
        {
            throw new EngineException("Quantity must be a positive whole number.");
        }

        int otherOptionIndex = 1 - optionIndex;
        double currentQuantity = event.getQuantity(optionIndex);
        double otherQuantity = event.getQuantity(otherOptionIndex);
        double b = event.getLiquidityB();

        double costBefore = Lmsr.cost(currentQuantity, otherQuantity, b);
        double costAfter = Lmsr.cost(currentQuantity + quantity, otherQuantity, b);
        double sharesCost = costAfter - costBefore;

        double feeCost = 0;
        if (event.getCommissionType() == CommissionType.ON_PURCHASE)
        {
            feeCost = sharesCost * event.getCommissionPercent() / 100.0;
        }

        user.subtractFromBalance(sharesCost + feeCost);
        event.addToAccountBalance(sharesCost);
        if (feeCost > 0)
        {
            User mm = findUserByName(event.getMmUserName());
            mm.addToBalance(feeCost);
            event.addFeesCollected(feeCost);
        }
        event.recordLmsrPurchase(actingUserName, optionIndex, quantity, sharesCost, feeCost);

        return new PurchaseResult(sharesCost, feeCost);
    }

    @Override
    public OrderSubmitResult submitOrder(int eventId, String actingUserName, int optionIndex, OrderSide side,
            int quantity, double price) throws EngineException
    {
        Event event = findEventById(eventId);
        User user = findUserByName(actingUserName);
        requireNotBlocked(user);

        if (event.getMethod() != TradingMethod.ORDER_BOOK)
        {
            throw new EngineException("This event uses LMSR trading, not Order Book. Buy shares instead.");
        }

        return OrderBookEngine.submitOrder(event, user, users, optionIndex, side, quantity, price);
    }

    private void closeLmsrEvent(Event event, User mm, int winningOptionIndex) throws EngineException
    {
        double originalBalance = event.getAccountBalance();
        int totalWinningShares = event.getQuantity(winningOptionIndex);
        double owedToWinners = totalWinningShares * WINNING_SHARE_PAYOUT;

        double fee = 0;
        if (event.getCommissionType() == CommissionType.ON_CLOSE)
        {
            fee = owedToWinners * event.getCommissionPercent() / 100.0;
            mm.addToBalance(fee);
            event.addFeesCollected(fee);
        }
        double actualPayout = owedToWinners - fee;

        if (totalWinningShares > 0)
        {
            payLmsrWinners(event, winningOptionIndex, totalWinningShares, actualPayout);
        }

        double leftoverSubsidy = originalBalance - owedToWinners;
        mm.addToBalance(leftoverSubsidy);

        event.addToAccountBalance(-originalBalance);
        event.close(winningOptionIndex);
    }

    private void payLmsrWinners(Event event, int winningOptionIndex, int totalWinningShares, double actualPayout)
            throws EngineException
    {
        List<String> paidUsers = new ArrayList<>();
        for (Trade trade : event.getTradeHistory())
        {
            if (!trade.getOptionName().equals(event.getOptionName(winningOptionIndex)))
            {
                continue;
            }
            String userName = trade.getUserName();
            if (paidUsers.contains(userName))
            {
                continue;
            }
            paidUsers.add(userName);

            int theirQuantity = sumUserQuantityInOption(event, userName, winningOptionIndex);
            double theirPayout = actualPayout * theirQuantity / totalWinningShares;
            findUserByName(userName).addToBalance(theirPayout);
        }
    }

    private int sumUserQuantityInOption(Event event, String userName, int optionIndex)
    {
        int total = 0;
        for (Trade trade : event.getTradeHistory())
        {
            if (trade.getUserName().equals(userName) && trade.getOptionName().equals(event.getOptionName(optionIndex)))
            {
                total += trade.getQuantity();
            }
        }
        return total;
    }

    private void closeOrderBookEvent(Event event, User mm, int winningOptionIndex) throws EngineException
    {
        double originalBalance = event.getAccountBalance();
        int totalWinningShares = 0;
        for (Holding holding : event.getHoldings())
        {
            if (holding.getOptionIndex() == winningOptionIndex)
            {
                totalWinningShares += holding.getQuantity();
            }
        }
        double owedToWinners = totalWinningShares * event.getBaseValueD();

        double fee = 0;
        if (event.getCommissionType() == CommissionType.ON_CLOSE)
        {
            fee = owedToWinners * event.getCommissionPercent() / 100.0;
            mm.addToBalance(fee);
            event.addFeesCollected(fee);
        }
        double actualPayout = owedToWinners - fee;

        if (totalWinningShares > 0)
        {
            for (Holding holding : event.getHoldings())
            {
                if (holding.getOptionIndex() == winningOptionIndex && holding.getQuantity() > 0)
                {
                    double theirShare = actualPayout * holding.getQuantity() / totalWinningShares;
                    findUserByName(holding.getUserName()).addToBalance(theirShare);
                }
            }
        }

        double leftover = originalBalance - owedToWinners;
        mm.addToBalance(leftover);

        event.addToAccountBalance(-originalBalance);
        event.close(winningOptionIndex);
    }

    private boolean eventInvolvesUser(Event event, String userName)
    {
        if (event.getMethod() == TradingMethod.LMSR)
        {
            for (Trade trade : event.getTradeHistory())
            {
                if (trade.getUserName().equals(userName))
                {
                    return true;
                }
            }
        }
        else
        {
            for (Holding holding : event.getHoldings())
            {
                if (holding.getUserName().equals(userName) && holding.getQuantity() > 0)
                {
                    return true;
                }
            }
            for (Order order : event.getOrderBook())
            {
                if (order.getUserName().equals(userName))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private List<EventInfo> toEventInfoList(List<Event> eventList)
    {
        List<EventInfo> infoList = new ArrayList<>();
        for (Event event : eventList)
        {
            infoList.add(event.toEventInfo());
        }
        return infoList;
    }

    private void validateOptionIndex(int optionIndex) throws EngineException
    {
        if (optionIndex != 0 && optionIndex != 1)
        {
            throw new EngineException("Invalid option chosen.");
        }
    }

    private void requireNotBlocked(User user) throws EngineException
    {
        if (user.isBlocked())
        {
            throw new EngineException("User '" + user.getName()
                    + "' is blocked (a previous action put their balance below zero) and cannot perform any more actions.");
        }
    }

    private void requireEventsLoaded() throws EngineException
    {
        if (events.isEmpty())
        {
            throw new EngineException("No events are loaded. Use the load command to load an events file first.");
        }
    }

    private void requireUsersLoaded() throws EngineException
    {
        if (users.isEmpty())
        {
            throw new EngineException("No users are loaded. Use the load command to load an events file first.");
        }
    }

    private Event findEventById(int eventId) throws EngineException
    {
        requireEventsLoaded();
        for (Event event : events)
        {
            if (event.getId() == eventId)
            {
                return event;
            }
        }
        throw new EngineException("No event found with id " + eventId + ".");
    }

    private User findUserByName(String userName) throws EngineException
    {
        requireUsersLoaded();
        for (User user : users)
        {
            if (user.getName().equals(userName))
            {
                return user;
            }
        }
        throw new EngineException("No user found with the name '" + userName + "'.");
    }
}
