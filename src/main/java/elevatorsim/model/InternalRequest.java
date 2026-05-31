package elevatorsim.model;

import elevatorsim.model.passenger.Passenger;

import java.util.Objects;

public record InternalRequest(int destinationFloor, Passenger passenger) {
    public InternalRequest {
        Objects.requireNonNull(passenger, "passenger");
    }
}
