package guessmarket.engine;

public class Holding
{

    private final String userName;
    private final int optionIndex;
    private int quantity;
    private double amountPaid;
    private double feePaid;

    public Holding(String userName, int optionIndex)
    {
        this.userName = userName;
        this.optionIndex = optionIndex;
        this.quantity = 0;
        this.amountPaid = 0;
        this.feePaid = 0;
    }

    public String getUserName()
    {
        return userName;
    }

    public int getOptionIndex()
    {
        return optionIndex;
    }

    public int getQuantity()
    {
        return quantity;
    }

    public double getAmountPaid()
    {
        return amountPaid;
    }

    public double getFeePaid()
    {
        return feePaid;
    }

    void addShares(int addedQuantity, double addedAmountPaid, double addedFee)
    {
        quantity += addedQuantity;
        amountPaid += addedAmountPaid;
        feePaid += addedFee;
    }

    void removeShares(int removedQuantity, double removedAmountPaid)
    {
        quantity -= removedQuantity;
        amountPaid -= removedAmountPaid;
    }
}
