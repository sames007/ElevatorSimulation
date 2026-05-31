package elevatorsim.model.elevator;

import elevatorsim.model.ElevatorType;
import elevatorsim.model.PassengerType;
import elevatorsim.model.passenger.Passenger;

public class StandardElevator extends Elevator {
    public StandardElevator(String id, int currentFloor) {
        super(id, ElevatorType.STANDARD, 6, currentFloor);
    }

    @Override
    public boolean canServe(Passenger passenger) {
        return passenger.getType() == PassengerType.STANDARD
                || passenger.getType() == PassengerType.VIP
                || passenger.getType() == PassengerType.GLASS;
    }
}
