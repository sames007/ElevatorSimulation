package elevatorsim.model.passenger;

import elevatorsim.model.Direction;
import elevatorsim.model.ElevatorType;
import elevatorsim.model.ExternalRequest;
import elevatorsim.model.InternalRequest;
import elevatorsim.model.PassengerType;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Passenger {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    private final int id;
    private final int startFloor;
    private final int endFloor;

    protected Passenger(int startFloor, int endFloor) {
        if (startFloor == endFloor) {
            throw new IllegalArgumentException("Start and end floor must be different.");
        }
        this.id = NEXT_ID.getAndIncrement();
        this.startFloor = startFloor;
        this.endFloor = endFloor;
    }

    public int getId() {
        return id;
    }

    public int getStartFloor() {
        return startFloor;
    }

    public int getEndFloor() {
        return endFloor;
    }

    public Direction desiredDirection() {
        return Direction.between(startFloor, endFloor);
    }

    public ExternalRequest requestElevator() {
        return new ExternalRequest(startFloor, desiredDirection(), this);
    }

    public InternalRequest pressDestinationButton() {
        return new InternalRequest(endFloor, this);
    }

    public boolean canRide(ElevatorType elevatorType) {
        return preferredElevators().contains(elevatorType);
    }

    public String getDisplayName() {
        return getType().label() + " P" + id;
    }

    public abstract PassengerType getType();

    public abstract List<ElevatorType> preferredElevators();
}
