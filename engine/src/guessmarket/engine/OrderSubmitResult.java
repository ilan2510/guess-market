package guessmarket.engine;

public class OrderSubmitResult
{

    private final int filledQuantity;
    private final int restingQuantity;
    private final double feePaid;

    public OrderSubmitResult(int filledQuantity, int restingQuantity, double feePaid)
    {
        this.filledQuantity = filledQuantity;
        this.restingQuantity = restingQuantity;
        this.feePaid = feePaid;
    }

    public int getFilledQuantity()
    {
        return filledQuantity;
    }

    public int getRestingQuantity()
    {
        return restingQuantity;
    }

    public double getFeePaid()
    {
        return feePaid;
    }
}
