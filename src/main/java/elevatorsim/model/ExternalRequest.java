package elevatorsim.model;

import elevatorsim.model.passenger.Passenger;

import java.util.Objects;

public record ExternalRequest(int sourceFloor, Direction direction, Passenger passenger) {
    public ExternalRequest {
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(passenger, "passenger");
        if (direction == Direction.IDLE) {
            throw new IllegalArgumentException("External requests must be either up or down.");
        }
    }

    public String summary() {
        return passenger.getDisplayName() + " requests " + direction.label()
                + " at floor " + sourceFloor;
    }
}
