package elevatorsim.model.elevator;

import elevatorsim.model.ElevatorType;
import elevatorsim.model.PassengerType;
import elevatorsim.model.passenger.Passenger;

public class FreightElevator extends Elevator {
    public FreightElevator(String id, int currentFloor) {
        super(id, ElevatorType.FREIGHT, 4, currentFloor);
    }

    @Override
    public boolean canServe(Passenger passenger) {
        return passenger.getType() == PassengerType.FREIGHT;
    }
}
