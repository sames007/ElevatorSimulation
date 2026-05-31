package elevatorsim.model.passenger;

import elevatorsim.model.ElevatorType;
import elevatorsim.model.PassengerType;

import java.util.List;

public class GlassPassenger extends Passenger {
    public GlassPassenger(int startFloor, int endFloor) {
        super(startFloor, endFloor);
    }

    @Override
    public PassengerType getType() {
        return PassengerType.GLASS;
    }

    @Override
    public List<ElevatorType> preferredElevators() {
        return List.of(ElevatorType.GLASS, ElevatorType.STANDARD);
    }
}
