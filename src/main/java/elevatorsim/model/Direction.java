package elevatorsim.model;

public enum Direction {
    UP("Up"),
    DOWN("Down"),
    IDLE("Idle");

    private final String label;

    Direction(String label) {
        this.label = label;
    }

    public static Direction between(int fromFloor, int toFloor) {
        if (toFloor > fromFloor) {
            return UP;
        }
        if (toFloor < fromFloor) {
            return DOWN;
        }
        return IDLE;
    }

    public Direction opposite() {
        return switch (this) {
            case UP -> DOWN;
            case DOWN -> UP;
            case IDLE -> IDLE;
        };
    }

    public String label() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
