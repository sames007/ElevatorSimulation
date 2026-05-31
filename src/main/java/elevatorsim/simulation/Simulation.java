package elevatorsim.simulation;

import elevatorsim.model.Direction;
import elevatorsim.model.ElevatorType;
import elevatorsim.model.ExternalRequest;
import elevatorsim.model.PassengerType;
import elevatorsim.model.elevator.Elevator;
import elevatorsim.model.elevator.ExpressElevator;
import elevatorsim.model.elevator.FreightElevator;
import elevatorsim.model.elevator.GlassElevator;
import elevatorsim.model.elevator.StandardElevator;
import elevatorsim.model.passenger.Passenger;
import elevatorsim.model.passenger.PassengerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

public class Simulation {
    private static final int MAX_LOG_ENTRIES = 300;

    private final SimulatorSettings settings;
    private final Random random;
    private final List<Elevator> elevators = new ArrayList<>();
    private final List<ExternalRequest> pendingExternalRequests = new ArrayList<>();
    private final List<Passenger> completedPassengers = new ArrayList<>();
    private final List<String> eventLog = new ArrayList<>();
    private int tickCount;

    public Simulation(SimulatorSettings settings) {
        this(settings, new Random());
    }

    public Simulation(SimulatorSettings settings, Random random) {
        this.settings = settings;
        this.random = random;
        reset();
    }

    public void reset() {
        elevators.clear();
        pendingExternalRequests.clear();
        completedPassengers.clear();
        eventLog.clear();
        tickCount = 0;

        addElevators(ElevatorType.STANDARD, settings.getElevatorCount(ElevatorType.STANDARD));
        addElevators(ElevatorType.EXPRESS, settings.getElevatorCount(ElevatorType.EXPRESS));
        addElevators(ElevatorType.FREIGHT, settings.getElevatorCount(ElevatorType.FREIGHT));
        addElevators(ElevatorType.GLASS, settings.getElevatorCount(ElevatorType.GLASS));

        if (elevators.isEmpty()) {
            elevators.add(new StandardElevator("StandardElevator-1", settings.minFloor()));
        }
        addEvent("Simulation reset with " + elevators.size() + " elevator(s).");
    }

    private void addElevators(ElevatorType type, int count) {
        for (int i = 1; i <= count; i++) {
            String id = type.label() + "Elevator-" + i;
            Elevator elevator = switch (type) {
                case STANDARD -> new StandardElevator(id, settings.minFloor());
                case EXPRESS -> new ExpressElevator(id, settings.minFloor());
                case FREIGHT -> new FreightElevator(id, settings.minFloor());
                case GLASS -> new GlassElevator(id, settings.minFloor());
            };
            elevators.add(elevator);
        }
    }

    public Passenger requestPassenger(PassengerType passengerType, int startFloor, int endFloor) {
        validateFloor(startFloor);
        validateFloor(endFloor);
        Passenger passenger = PassengerFactory.create(passengerType, startFloor, endFloor);
        requestPassenger(passenger);
        return passenger;
    }

    public void requestPassenger(Passenger passenger) {
        ExternalRequest request = passenger.requestElevator();
        pendingExternalRequests.add(request);
        addEvent(request.summary() + " to reach floor " + passenger.getEndFloor() + ".");
        assignPendingRequests();
    }

    public List<Passenger> generateRandomPassengers() {
        List<Passenger> generatedPassengers = new ArrayList<>();
        for (int i = 0; i < settings.generatedPassengerCount(); i++) {
            PassengerType passengerType = choosePassengerType();
            int startFloor = randomFloor();
            int endFloor = randomFloor();
            while (endFloor == startFloor) {
                endFloor = randomFloor();
            }
            Passenger passenger = requestPassenger(passengerType, startFloor, endFloor);
            generatedPassengers.add(passenger);
        }
        return generatedPassengers;
    }

    public void tick() {
        tickCount++;
        assignPendingRequests();
        for (Elevator elevator : elevators) {
            elevator.tick(settings.minFloor(), settings.maxFloor(), this::addEvent, completedPassengers::add);
        }
        assignPendingRequests();
    }

    private void assignPendingRequests() {
        Iterator<ExternalRequest> iterator = pendingExternalRequests.iterator();
        while (iterator.hasNext()) {
            ExternalRequest request = iterator.next();
            Optional<Elevator> elevator = chooseElevator(request);
            if (elevator.isPresent()) {
                Elevator selectedElevator = elevator.get();
                selectedElevator.assignExternalRequest(request);
                iterator.remove();
                addEvent(request.passenger().getDisplayName() + " assigned to " + selectedElevator.getId() + ".");
            }
        }
    }

    private Optional<Elevator> chooseElevator(ExternalRequest request) {
        return elevators.stream()
                .filter(elevator -> elevator.canAccept(request))
                .min(Comparator.comparingInt(elevator -> score(elevator, request)));
    }

    private int score(Elevator elevator, ExternalRequest request) {
        int score = Math.abs(elevator.getCurrentFloor() - request.sourceFloor()) * 2;
        score += elevator.getReservedPassengerSlots() * 4;

        if (elevator.getDirection() == Direction.IDLE) {
            score -= 5;
        } else if (elevator.getDirection() == request.direction() && elevator.isFloorAhead(request.sourceFloor())) {
            score -= 16;
        } else if (elevator.isFloorAhead(request.sourceFloor())) {
            score -= 12;
        } else {
            score += 18;
        }

        if (request.passenger().preferredElevators().contains(elevator.getType())) {
            score -= 8;
        } else {
            score += 6;
        }

        return score;
    }

    private PassengerType choosePassengerType() {
        Map<PassengerType, Integer> weights = new EnumMap<>(PassengerType.class);
        weights.putAll(settings.passengerPercentages());
        int totalWeight = weights.values().stream().mapToInt(Integer::intValue).filter(value -> value > 0).sum();
        if (totalWeight <= 0) {
            return PassengerType.STANDARD;
        }

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (PassengerType passengerType : PassengerType.values()) {
            cumulative += Math.max(0, weights.getOrDefault(passengerType, 0));
            if (roll < cumulative) {
                return passengerType;
            }
        }
        return PassengerType.STANDARD;
    }

    private int randomFloor() {
        return random.nextInt(settings.totalFloors());
    }

    private void validateFloor(int floor) {
        if (floor < settings.minFloor() || floor > settings.maxFloor()) {
            throw new IllegalArgumentException("Floor " + floor + " is outside the building.");
        }
    }

    public int waitingRequestCount(int floor, Direction direction) {
        int pendingCount = (int) pendingExternalRequests.stream()
                .filter(request -> request.sourceFloor() == floor && request.direction() == direction)
                .count();
        int assignedCount = elevators.stream()
                .flatMap(elevator -> elevator.getAssignedExternalRequests().stream())
                .filter(request -> request.sourceFloor() == floor && request.direction() == direction)
                .mapToInt(request -> 1)
                .sum();
        return pendingCount + assignedCount;
    }

    public int totalWaitingRequests() {
        int assignedRequests = elevators.stream()
                .mapToInt(elevator -> elevator.getAssignedExternalRequests().size())
                .sum();
        return pendingExternalRequests.size() + assignedRequests;
    }

    public void addEvent(String event) {
        eventLog.add("T" + tickCount + "  " + event);
        while (eventLog.size() > MAX_LOG_ENTRIES) {
            eventLog.removeFirst();
        }
    }

    public SimulatorSettings getSettings() {
        return settings;
    }

    public List<Elevator> getElevators() {
        return List.copyOf(elevators);
    }

    public List<Passenger> getCompletedPassengers() {
        return List.copyOf(completedPassengers);
    }

    public List<String> getEventLog() {
        return List.copyOf(eventLog);
    }

    public int getTickCount() {
        return tickCount;
    }
}
