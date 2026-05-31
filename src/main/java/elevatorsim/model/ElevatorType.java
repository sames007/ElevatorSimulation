package elevatorsim.model;

public enum ElevatorType {
    STANDARD("Standard"),
    EXPRESS("Express"),
    FREIGHT("Freight"),
    GLASS("Glass");

    private final String label;

    ElevatorType(String label) {
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
