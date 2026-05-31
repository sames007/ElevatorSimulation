package elevatorsim.model.elevator;

import elevatorsim.model.Direction;
import elevatorsim.model.ElevatorType;
import elevatorsim.model.ExternalRequest;
import elevatorsim.model.InternalRequest;
import elevatorsim.model.passenger.Passenger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

public abstract class Elevator {
    private final String id;
    private final ElevatorType type;
    private final int capacity;
    private int currentFloor;
    private Direction direction = Direction.IDLE;
    private boolean doorsOpen;
    private final List<Passenger> passengers = new ArrayList<>();
    private final List<ExternalRequest> externalRequests = new ArrayList<>();
    private final List<InternalRequest> internalRequests = new ArrayList<>();

    protected Elevator(String id, ElevatorType type, int capacity, int currentFloor) {
        this.id = id;
        this.type = type;
        this.capacity = capacity;
        this.currentFloor = currentFloor;
    }

    public abstract boolean canServe(Passenger passenger);

    public boolean canAccept(ExternalRequest request) {
        return canServe(request.passenger())
                && servesFloor(request.sourceFloor())
                && servesFloor(request.passenger().getEndFloor())
                && getReservedPassengerSlots() < capacity;
    }

    public boolean servesFloor(int floor) {
        return true;
    }

    public void assignExternalRequest(ExternalRequest request) {
        if (!canAccept(request)) {
            throw new IllegalArgumentException(getId() + " cannot accept " + request.summary());
        }
        externalRequests.add(request);
    }

    public void tick(int minFloor, int maxFloor, Consumer<String> eventLog, Consumer<Passenger> completionLog) {
        doorsOpen = false;
        if (handleCurrentFloor(eventLog, completionLog)) {
            return;
        }

        Integer targetFloor = chooseNextTarget();
        if (targetFloor == null) {
            direction = Direction.IDLE;
            return;
        }

        direction = Direction.between(currentFloor, targetFloor);
        if (direction == Direction.IDLE) {
            handleCurrentFloor(eventLog, completionLog);
            return;
        }

        currentFloor += direction == Direction.UP ? 1 : -1;
        if (currentFloor < minFloor) {
            currentFloor = minFloor;
        } else if (currentFloor > maxFloor) {
            currentFloor = maxFloor;
        }

        eventLog.accept(id + " moved " + direction.label().toLowerCase(Locale.ROOT)
                + " to floor " + currentFloor);
        handleCurrentFloor(eventLog, completionLog);
        if (!hasTargets()) {
            direction = Direction.IDLE;
        }
    }

    private boolean handleCurrentFloor(Consumer<String> eventLog, Consumer<Passenger> completionLog) {
        boolean stopped = false;

        List<InternalRequest> arrivedPassengers = internalRequests.stream()
                .filter(request -> request.destinationFloor() == currentFloor)
                .toList();
        for (InternalRequest request : arrivedPassengers) {
            passengers.remove(request.passenger());
            internalRequests.remove(request);
            completionLog.accept(request.passenger());
            eventLog.accept(id + " dropped off " + request.passenger().getDisplayName()
                    + " at floor " + currentFloor);
            stopped = true;
        }

        List<ExternalRequest> boardingRequests = externalRequests.stream()
                .filter(request -> request.sourceFloor() == currentFloor)
                .filter(this::canBoardNow)
                .toList();
        for (ExternalRequest request : boardingRequests) {
            if (passengers.size() >= capacity) {
                break;
            }
            Passenger passenger = request.passenger();
            passengers.add(passenger);
            externalRequests.remove(request);
            internalRequests.add(passenger.pressDestinationButton());
            eventLog.accept(passenger.getDisplayName() + " entered " + id
                    + " at floor " + currentFloor
                    + " and pressed floor " + passenger.getEndFloor());
            stopped = true;
        }

        if (stopped) {
            doorsOpen = true;
        }
        if (!hasTargets()) {
            direction = Direction.IDLE;
        }
        return stopped;
    }

    private boolean canBoardNow(ExternalRequest request) {
        return direction == Direction.IDLE || direction == request.direction() || passengers.isEmpty();
    }

    private Integer chooseNextTarget() {
        if (!hasTargets()) {
            return null;
        }
        if (direction == Direction.IDLE) {
            return nearestTarget();
        }

        // Internal stops and same-direction hall calls stay on the active route.
        // Opposite-direction hall calls remain assigned but wait for the return trip.
        Integer nextAlignedTarget = nearestTargetInDirection(direction, true);
        if (nextAlignedTarget != null) {
            return nextAlignedTarget;
        }

        if (passengers.isEmpty()) {
            Integer nextAnyTarget = nearestTargetInDirection(direction, false);
            if (nextAnyTarget != null) {
                return nextAnyTarget;
            }
        }

        return nearestTargetInDirection(direction.opposite(), false);
    }

    private Integer nearestTarget() {
        return allTargetFloors(false).stream()
                .min(Comparator.comparingInt((Integer floor) -> Math.abs(floor - currentFloor))
                        .thenComparingInt(Integer::intValue))
                .orElse(null);
    }

    private Integer nearestTargetInDirection(Direction targetDirection, boolean onlyAlignedExternalRequests) {
        Optional<Integer> target = allTargetFloors(onlyAlignedExternalRequests).stream()
                .filter(floor -> isFloorInDirection(floor, targetDirection))
                .min(targetDirection == Direction.UP
                        ? Comparator.naturalOrder()
                        : Comparator.reverseOrder());
        return target.orElse(null);
    }

    private List<Integer> allTargetFloors(boolean onlyAlignedExternalRequests) {
        List<Integer> floors = new ArrayList<>();
        for (InternalRequest request : internalRequests) {
            floors.add(request.destinationFloor());
        }
        for (ExternalRequest request : externalRequests) {
            if (!onlyAlignedExternalRequests || passengers.isEmpty() || request.direction() == direction) {
                floors.add(request.sourceFloor());
            }
        }
        return floors;
    }

    private boolean isFloorInDirection(int floor, Direction targetDirection) {
        return switch (targetDirection) {
            case UP -> floor > currentFloor;
            case DOWN -> floor < currentFloor;
            case IDLE -> floor == currentFloor;
        };
    }

    private boolean hasTargets() {
        return !externalRequests.isEmpty() || !internalRequests.isEmpty();
    }

    public boolean isFloorAhead(int floor) {
        return switch (direction) {
            case UP -> floor >= currentFloor;
            case DOWN -> floor <= currentFloor;
            case IDLE -> true;
        };
    }

    public String describeTargets() {
        String pickupTargets = externalRequests.stream()
                .map(request -> request.sourceFloor() + " " + request.direction().label())
                .reduce((left, right) -> left + ", " + right)
                .orElse("none");
        String dropTargets = internalRequests.stream()
                .map(request -> String.valueOf(request.destinationFloor()))
                .reduce((left, right) -> left + ", " + right)
                .orElse("none");
        return "pickups: " + pickupTargets + " | destinations: " + dropTargets;
    }

    public String getId() {
        return id;
    }

    public ElevatorType getType() {
        return type;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public Direction getDirection() {
        return direction;
    }

    public boolean isDoorsOpen() {
        return doorsOpen;
    }

    public int getReservedPassengerSlots() {
        return passengers.size() + externalRequests.size();
    }

    public List<Passenger> getPassengers() {
        return List.copyOf(passengers);
    }

    public List<ExternalRequest> getAssignedExternalRequests() {
        return List.copyOf(externalRequests);
    }
}
