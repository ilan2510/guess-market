package guessmarket.engine;

import guessmarket.engine.exception.EngineException;

import java.util.List;

public class OrderBookEngine
{

    private OrderBookEngine()
    {
    }

    public static OrderSubmitResult submitOrder(Event event, User actingUser, List<User> allUsers,
            int optionIndex, OrderSide side, int quantity, double price) throws EngineException
    {
        validateOrder(event, actingUser, optionIndex, side, quantity, price);

        int remainingQuantity = quantity;
        double totalFeePaid = 0;

        while (remainingQuantity > 0)
        {
            Order matchedOrder = findBestMatch(event, optionIndex, side, price, actingUser.getName());
            if (matchedOrder != null)
            {
                int filled = Math.min(remainingQuantity, matchedOrder.getQuantity());
                totalFeePaid += executeDirectTrade(event, allUsers, actingUser, side, optionIndex, matchedOrder, filled);
                remainingQuantity -= filled;
                removeIfExhausted(event, matchedOrder);
                continue;
            }

            if (side == OrderSide.BUY && event.isAllowMint())
            {
                int otherOptionIndex = 1 - optionIndex;
                Order mintPartner = findBestMintPartner(event, otherOptionIndex, price, actingUser.getName());
                if (mintPartner != null)
                {
                    int filled = Math.min(remainingQuantity, mintPartner.getQuantity());
                    totalFeePaid += executeMint(event, allUsers, actingUser, optionIndex,
                            mintPartner, otherOptionIndex, filled);
                    remainingQuantity -= filled;
                    removeIfExhausted(event, mintPartner);
                    continue;
                }
            }

            break;
        }

        int restingQuantity = remainingQuantity;
        if (restingQuantity > 0)
        {
            event.getOrderBook().add(new Order(actingUser.getName(), optionIndex, side, restingQuantity, price));
        }

        int filledQuantity = quantity - restingQuantity;
        return new OrderSubmitResult(filledQuantity, restingQuantity, totalFeePaid);
    }

    public static int getAvailableToSell(Event event, String userName, int optionIndex)
    {
        Holding holding = event.findHolding(userName, optionIndex);
        int owned = 0;
        if (holding != null)
        {
            owned = holding.getQuantity();
        }
        int alreadyOffered = 0;
        for (Order order : event.getOrderBook())
        {
            if (order.getUserName().equals(userName) && order.getOptionIndex() == optionIndex
                    && order.getSide() == OrderSide.SELL)
            {
                alreadyOffered += order.getQuantity();
            }
        }
        return owned - alreadyOffered;
    }

    private static void removeIfExhausted(Event event, Order order)
    {
        if (order.getQuantity() == 0)
        {
            event.getOrderBook().remove(order);
        }
    }

    private static void validateOrder(Event event, User actingUser, int optionIndex, OrderSide side,
            int quantity, double price) throws EngineException
    {
        if (event.getStatus() != EventStatus.ACTIVE)
        {
            throw new EngineException("This event is not active, trading is not allowed right now.");
        }
        if (optionIndex != 0 && optionIndex != 1)
        {
            throw new EngineException("Invalid option chosen.");
        }
        if (quantity <= 0)
        {
            throw new EngineException("Quantity must be a positive whole number.");
        }
        double maxPrice = event.getBaseValueD() - 0.01;
        if (price <= 0 || price > maxPrice)
        {
            throw new EngineException("Price must be greater than 0.00 and at most "
                    + String.format("%.2f", maxPrice) + " (the event's base value minus 0.01).");
        }
        if (side == OrderSide.SELL)
        {
            int available = getAvailableToSell(event, actingUser.getName(), optionIndex);
            if (quantity > available)
            {
                throw new EngineException("You only have " + available + " share(s) of "
                        + event.getOptionName(optionIndex) + " available to sell.");
            }
        }
    }

    private static Order findBestMatch(Event event, int optionIndex, OrderSide incomingSide, double incomingPrice,
            String actingUserName)
    {
        OrderSide restingSideNeeded = OrderSide.SELL;
        if (incomingSide == OrderSide.SELL)
        {
            restingSideNeeded = OrderSide.BUY;
        }

        Order best = null;
        for (Order order : event.getOrderBook())
        {
            if (order.getOptionIndex() != optionIndex || order.getSide() != restingSideNeeded)
            {
                continue;
            }
            if (order.getUserName().equals(actingUserName))
            {
                continue;
            }

            boolean crosses;
            if (incomingSide == OrderSide.BUY)
            {
                crosses = incomingPrice >= order.getPrice();
            }
            else
            {
                crosses = order.getPrice() >= incomingPrice;
            }
            if (!crosses)
            {
                continue;
            }

            if (best == null)
            {
                best = order;
            }
            else if (incomingSide == OrderSide.BUY && order.getPrice() < best.getPrice())
            {
                best = order;
            }
            else if (incomingSide == OrderSide.SELL && order.getPrice() > best.getPrice())
            {
                best = order;
            }
        }
        return best;
    }

    private static Order findBestMintPartner(Event event, int otherOptionIndex, double incomingPrice,
            String actingUserName)
    {
        double baseValueD = event.getBaseValueD();
        Order best = null;
        for (Order order : event.getOrderBook())
        {
            if (order.getOptionIndex() != otherOptionIndex || order.getSide() != OrderSide.BUY)
            {
                continue;
            }
            if (order.getUserName().equals(actingUserName))
            {
                continue;
            }
            if (order.getPrice() + incomingPrice < baseValueD)
            {
                continue;
            }
            if (best == null || order.getPrice() > best.getPrice())
            {
                best = order;
            }
        }
        return best;
    }

    private static double executeDirectTrade(Event event, List<User> allUsers, User actingUser, OrderSide actingSide,
            int optionIndex, Order matchedOrder, int quantity) throws EngineException
    {
        double price = matchedOrder.getPrice();
        double tradeValue = quantity * price;

        String buyerName = actingUser.getName();
        String sellerName = matchedOrder.getUserName();
        if (actingSide == OrderSide.SELL)
        {
            buyerName = matchedOrder.getUserName();
            sellerName = actingUser.getName();
        }

        User buyer = findUser(allUsers, buyerName);
        User seller = findUser(allUsers, sellerName);

        double fee = calculatePurchaseFee(event, tradeValue);
        buyer.subtractFromBalance(tradeValue + fee);
        seller.addToBalance(tradeValue);
        if (fee > 0)
        {
            payFeeToMm(event, allUsers, fee);
        }

        Holding sellerHolding = event.findOrCreateHolding(sellerName, optionIndex);
        double sellerCostRemoved = 0;
        if (sellerHolding.getQuantity() > 0)
        {
            sellerCostRemoved = sellerHolding.getAmountPaid() * quantity / sellerHolding.getQuantity();
        }

        event.findOrCreateHolding(buyerName, optionIndex).addShares(quantity, tradeValue, fee);
        sellerHolding.removeShares(quantity, sellerCostRemoved);

        matchedOrder.reduceQuantity(quantity);
        event.setLastTradePrice(optionIndex, price);

        return fee;
    }

    private static double executeMint(Event event, List<User> allUsers, User incomingUser, int incomingOptionIndex,
            Order restingOrder, int restingOptionIndex, int quantity) throws EngineException
    {
        double restingFillPrice = restingOrder.getPrice();
        double incomingFillPrice = event.getBaseValueD() - restingFillPrice;

        double incomingCost = quantity * incomingFillPrice;
        double restingCost = quantity * restingFillPrice;

        User restingBuyer = findUser(allUsers, restingOrder.getUserName());

        double incomingFee = calculatePurchaseFee(event, incomingCost);
        double restingFee = calculatePurchaseFee(event, restingCost);

        incomingUser.subtractFromBalance(incomingCost + incomingFee);
        restingBuyer.subtractFromBalance(restingCost + restingFee);
        event.addToAccountBalance(incomingCost + restingCost);

        double totalFee = incomingFee + restingFee;
        if (totalFee > 0)
        {
            payFeeToMm(event, allUsers, totalFee);
        }

        event.findOrCreateHolding(incomingUser.getName(), incomingOptionIndex).addShares(quantity, incomingCost, incomingFee);
        event.findOrCreateHolding(restingBuyer.getName(), restingOptionIndex).addShares(quantity, restingCost, restingFee);

        restingOrder.reduceQuantity(quantity);
        event.setLastTradePrice(incomingOptionIndex, incomingFillPrice);
        event.setLastTradePrice(restingOptionIndex, restingFillPrice);

        return totalFee;
    }

    private static double calculatePurchaseFee(Event event, double tradeValue)
    {
        if (event.getCommissionType() == CommissionType.ON_PURCHASE)
        {
            return tradeValue * event.getCommissionPercent() / 100.0;
        }
        return 0;
    }

    private static void payFeeToMm(Event event, List<User> allUsers, double fee) throws EngineException
    {
        User mm = findUser(allUsers, event.getMmUserName());
        mm.addToBalance(fee);
        event.addFeesCollected(fee);
    }

    private static User findUser(List<User> allUsers, String userName) throws EngineException
    {
        for (User user : allUsers)
        {
            if (user.getName().equals(userName))
            {
                return user;
            }
        }
        throw new EngineException("User not found: " + userName);
    }
}
