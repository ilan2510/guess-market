package guessmarket.engine;

public class ParticipantInfo
{

    private final String userName;
    private final int[] quantities;
    private final double[] values;
    private final double[] amountsPaid;
    private final double feePaid;

    public ParticipantInfo(String userName, int[] quantities, double[] values, double[] amountsPaid, double feePaid)
    {
        this.userName = userName;
        this.quantities = quantities;
        this.values = values;
        this.amountsPaid = amountsPaid;
        this.feePaid = feePaid;
    }

    public String getUserName()
    {
        return userName;
    }

    public int getQuantity(int optionIndex)
    {
        return quantities[optionIndex];
    }

    public double getValue(int optionIndex)
    {
        return values[optionIndex];
    }

    public double getAmountPaid(int optionIndex)
    {
        return amountsPaid[optionIndex];
    }

    public double getFeePaid()
    {
        return feePaid;
    }
}
