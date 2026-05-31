package elevatorsim.model.elevator;

import elevatorsim.model.ElevatorType;
import elevatorsim.model.PassengerType;
import elevatorsim.model.passenger.Passenger;

public class GlassElevator extends Elevator {
    public GlassElevator(String id, int currentFloor) {
        super(id, ElevatorType.GLASS, 5, currentFloor);
    }

    @Override
    public boolean canServe(Passenger passenger) {
        return passenger.getType() == PassengerType.GLASS
                || passenger.getType() == PassengerType.STANDARD
                || passenger.getType() == PassengerType.VIP;
    }
}
