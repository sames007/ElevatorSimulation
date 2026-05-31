package elevatorsim.model.passenger;

import elevatorsim.model.PassengerType;

public final class PassengerFactory {
    private PassengerFactory() {
    }

    public static Passenger create(PassengerType type, int startFloor, int endFloor) {
        return switch (type) {
            case STANDARD -> new StandardPassenger(startFloor, endFloor);
            case VIP -> new VIPPassenger(startFloor, endFloor);
            case FREIGHT -> new FreightPassenger(startFloor, endFloor);
            case GLASS -> new GlassPassenger(startFloor, endFloor);
        };
    }
}
