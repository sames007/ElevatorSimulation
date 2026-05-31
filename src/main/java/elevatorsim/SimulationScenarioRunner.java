package elevatorsim;

import elevatorsim.model.PassengerType;
import elevatorsim.model.passenger.Passenger;
import elevatorsim.simulation.Simulation;
import elevatorsim.simulation.SimulatorSettings;

import java.util.List;
import java.util.Random;

public final class SimulationScenarioRunner {
    private SimulationScenarioRunner() {
    }

    public static void main(String[] args) {
        runSameDirectionScenario();
        runOppositeDirectionScenario();
        runRandomTrafficScenario();
        System.out.println("Scenario smoke tests passed.");
    }

    private static void runSameDirectionScenario() {
        Simulation simulation = new Simulation(SimulatorSettings.defaults());
        Passenger firstPassenger = simulation.requestPassenger(PassengerType.STANDARD, 0, 5);
        Passenger secondPassenger = null;
        boolean injectedSecondPassenger = false;

        for (int i = 0; i < 30; i++) {
            simulation.tick();
            if (!injectedSecondPassenger && simulation.getElevators().getFirst().getCurrentFloor() >= 1) {
                secondPassenger = simulation.requestPassenger(PassengerType.STANDARD, 2, 4);
                injectedSecondPassenger = true;
            }
        }

        List<String> log = simulation.getEventLog();
        require(secondPassenger != null, "Same-direction scenario did not inject the second passenger.");
        assertEventOrder(log,
                firstPassenger.getDisplayName() + " entered",
                "dropped off " + secondPassenger.getDisplayName() + " at floor 4");
        assertEventOrder(log,
                "dropped off " + secondPassenger.getDisplayName() + " at floor 4",
                "dropped off " + firstPassenger.getDisplayName() + " at floor 5");
    }

    private static void runOppositeDirectionScenario() {
        Simulation simulation = new Simulation(SimulatorSettings.defaults());
        Passenger firstPassenger = simulation.requestPassenger(PassengerType.STANDARD, 0, 5);
        Passenger secondPassenger = null;
        boolean injectedSecondPassenger = false;

        for (int i = 0; i < 35; i++) {
            simulation.tick();
            if (!injectedSecondPassenger && simulation.getElevators().getFirst().getCurrentFloor() >= 1) {
                secondPassenger = simulation.requestPassenger(PassengerType.STANDARD, 2, 0);
                injectedSecondPassenger = true;
            }
        }

        List<String> log = simulation.getEventLog();
        require(secondPassenger != null, "Opposite-direction scenario did not inject the second passenger.");
        assertEventOrder(log,
                "dropped off " + firstPassenger.getDisplayName() + " at floor 5",
                "entered StandardElevator-1 at floor 2");
        assertEventOrder(log,
                "entered StandardElevator-1 at floor 2",
                "dropped off " + secondPassenger.getDisplayName() + " at floor 0");
    }

    private static void runRandomTrafficScenario() {
        Simulation simulation = new Simulation(SimulatorSettings.defaults(), new Random(42));
        int passengerCount = simulation.generateRandomPassengers().size();
        for (int i = 0; i < 180; i++) {
            simulation.tick();
        }

        require(simulation.getCompletedPassengers().size() == passengerCount,
                "Expected all generated passengers to complete their trips.");
        require(simulation.totalWaitingRequests() == 0,
                "Expected no waiting external requests after the random traffic run.");
        simulation.getElevators().forEach(elevator -> require(
                elevator.getCurrentFloor() >= simulation.getSettings().minFloor()
                        && elevator.getCurrentFloor() <= simulation.getSettings().maxFloor(),
                elevator.getId() + " moved outside the configured floors."));
    }

    private static void assertEventOrder(List<String> log, String earlier, String later) {
        int earlierIndex = indexOfContaining(log, earlier);
        int laterIndex = indexOfContaining(log, later);
        if (earlierIndex < 0 || laterIndex < 0 || earlierIndex >= laterIndex) {
            throw new IllegalStateException("Expected event containing \"" + earlier
                    + "\" before \"" + later + "\".\nLog:\n" + String.join("\n", log));
        }
    }

    private static int indexOfContaining(List<String> log, String text) {
        for (int i = 0; i < log.size(); i++) {
            if (log.get(i).contains(text)) {
                return i;
            }
        }
        return -1;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
