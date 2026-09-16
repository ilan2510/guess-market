package guessmarket.engine;

public class OrderInfo
{

    private final String userName;
    private final int quantity;
    private final double price;
    private final OrderSide side;

    public OrderInfo(String userName, int quantity, double price, OrderSide side)
    {
        this.userName = userName;
        this.quantity = quantity;
        this.price = price;
        this.side = side;
    }

    public String getUserName()
    {
        return userName;
    }

    public int getQuantity()
    {
        return quantity;
    }

    public double getPrice()
    {
        return price;
    }

    public OrderSide getSide()
    {
        return side;
    }
}
