package guessmarket.engine;

public class UserInfo
{

    private final String name;
    private final double balance;
    private final boolean blocked;

    public UserInfo(String name, double balance, boolean blocked)
    {
        this.name = name;
        this.balance = balance;
        this.blocked = blocked;
    }

    public String getName()
    {
        return name;
    }

    public double getBalance()
    {
        return balance;
    }

    public boolean isBlocked()
    {
        return blocked;
    }
}
