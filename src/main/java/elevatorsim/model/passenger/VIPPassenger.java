package elevatorsim.model.passenger;

import elevatorsim.model.ElevatorType;
import elevatorsim.model.PassengerType;

import java.util.List;

public class VIPPassenger extends Passenger {
    public VIPPassenger(int startFloor, int endFloor) {
        super(startFloor, endFloor);
    }

    @Override
    public PassengerType getType() {
        return PassengerType.VIP;
    }

    @Override
    public List<ElevatorType> preferredElevators() {
        return List.of(ElevatorType.EXPRESS, ElevatorType.GLASS, ElevatorType.STANDARD);
    }
}
