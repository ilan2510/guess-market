package guessmarket.engine;

public class PurchaseResult {

    private final double sharesCost;
    private final double feeCost;

    public PurchaseResult(double sharesCost, double feeCost) {
        this.sharesCost = sharesCost;
        this.feeCost = feeCost;
    }

    public double getSharesCost() {
        return sharesCost;
    }

    public double getFeeCost() {
        return feeCost;
    }

    public double getTotalPaid() {
        return sharesCost + feeCost;
    }
}
