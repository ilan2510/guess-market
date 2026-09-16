package guessmarket.engine;

public class User
{

    private final String name;
    private double balance;
    private boolean blocked;

    public User(String name, double balance)
    {
        this.name = name;
        this.balance = balance;
        this.blocked = false;
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

    void addToBalance(double amount)
    {
        balance += amount;
    }

    // Allowed to push the balance negative on purpose: the spec says a single action that
    // would go negative should still complete, the user is just blocked from anything after it.
    void subtractFromBalance(double amount)
    {
        balance -= amount;
        if (balance < 0)
        {
            blocked = true;
        }
    }

    public UserInfo toUserInfo()
    {
        return new UserInfo(name, balance, blocked);
    }
}
