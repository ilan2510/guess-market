package guessmarket.engine;

public class Lmsr
{

    private Lmsr()
    {
    }

    public static double price(double qOptionA, double qOptionB, double b)
    {
        double expA = Math.exp(qOptionA / b);
        double expB = Math.exp(qOptionB / b);
        return expA / (expA + expB);
    }

    public static double cost(double qOptionA, double qOptionB, double b)
    {
        double expA = Math.exp(qOptionA / b);
        double expB = Math.exp(qOptionB / b);
        return b * Math.log(expA + expB);
    }
}
