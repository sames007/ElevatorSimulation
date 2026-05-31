package elevatorsim.model.passenger;

import elevatorsim.model.ElevatorType;
import elevatorsim.model.PassengerType;

import java.util.List;

public class FreightPassenger extends Passenger {
    public FreightPassenger(int startFloor, int endFloor) {
        super(startFloor, endFloor);
    }

    @Override
    public PassengerType getType() {
        return PassengerType.FREIGHT;
    }

    @Override
    public List<ElevatorType> preferredElevators() {
        return List.of(ElevatorType.FREIGHT);
    }
}
