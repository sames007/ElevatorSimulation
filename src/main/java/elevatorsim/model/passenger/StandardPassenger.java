package elevatorsim.model.passenger;

import elevatorsim.model.ElevatorType;
import elevatorsim.model.PassengerType;

import java.util.List;

public class StandardPassenger extends Passenger {
    public StandardPassenger(int startFloor, int endFloor) {
        super(startFloor, endFloor);
    }

    @Override
    public PassengerType getType() {
        return PassengerType.STANDARD;
    }

    @Override
    public List<ElevatorType> preferredElevators() {
        return List.of(ElevatorType.STANDARD, ElevatorType.GLASS, ElevatorType.EXPRESS);
    }
}
