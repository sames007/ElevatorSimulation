package elevatorsim.model.elevator;

import elevatorsim.model.ElevatorType;
import elevatorsim.model.PassengerType;
import elevatorsim.model.passenger.Passenger;

public class ExpressElevator extends Elevator {
    public ExpressElevator(String id, int currentFloor) {
        super(id, ElevatorType.EXPRESS, 8, currentFloor);
    }

    @Override
    public boolean canServe(Passenger passenger) {
        return passenger.getType() == PassengerType.VIP
                || passenger.getType() == PassengerType.STANDARD;
    }
}
