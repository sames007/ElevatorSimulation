package elevatorsim.model;

public enum PassengerType {
    STANDARD("Standard"),
    VIP("VIP"),
    FREIGHT("Freight"),
    GLASS("Glass");

    private final String label;

    PassengerType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
