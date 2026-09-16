package guessmarket.engine;

public class Order
{

    private final String userName;
    private final int optionIndex;
    private final OrderSide side;
    private final double price;
    private int quantity;

    public Order(String userName, int optionIndex, OrderSide side, int quantity, double price)
    {
        this.userName = userName;
        this.optionIndex = optionIndex;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
    }

    public String getUserName()
    {
        return userName;
    }

    public int getOptionIndex()
    {
        return optionIndex;
    }

    public OrderSide getSide()
    {
        return side;
    }

    public double getPrice()
    {
        return price;
    }

    public int getQuantity()
    {
        return quantity;
    }

    void reduceQuantity(int amount)
    {
        quantity -= amount;
    }

    public OrderInfo toOrderInfo()
    {
        return new OrderInfo(userName, quantity, price, side);
    }
}
