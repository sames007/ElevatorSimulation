package elevatorsim.simulation;

import elevatorsim.model.ElevatorType;
import elevatorsim.model.PassengerType;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

public class SimulatorSettings {
    private final int totalFloors;
    private final int generatedPassengerCount;
    private final int tickMillis;
    private final Map<ElevatorType, Integer> elevatorCounts;
    private final Map<PassengerType, Integer> passengerPercentages;

    public SimulatorSettings(
            int totalFloors,
            int generatedPassengerCount,
            int tickMillis,
            Map<ElevatorType, Integer> elevatorCounts,
            Map<PassengerType, Integer> passengerPercentages
    ) {
        this.totalFloors = Math.max(2, totalFloors);
        this.generatedPassengerCount = Math.max(1, generatedPassengerCount);
        this.tickMillis = Math.max(150, tickMillis);
        this.elevatorCounts = sanitizeCounts(elevatorCounts);
        this.passengerPercentages = sanitizePercentages(passengerPercentages);
    }

    public static SimulatorSettings defaults() {
        Map<ElevatorType, Integer> elevatorCounts = new EnumMap<>(ElevatorType.class);
        elevatorCounts.put(ElevatorType.STANDARD, 1);
        elevatorCounts.put(ElevatorType.EXPRESS, 1);
        elevatorCounts.put(ElevatorType.FREIGHT, 1);
        elevatorCounts.put(ElevatorType.GLASS, 1);

        Map<PassengerType, Integer> passengerPercentages = new EnumMap<>(PassengerType.class);
        passengerPercentages.put(PassengerType.STANDARD, 55);
        passengerPercentages.put(PassengerType.VIP, 20);
        passengerPercentages.put(PassengerType.FREIGHT, 10);
        passengerPercentages.put(PassengerType.GLASS, 15);

        return new SimulatorSettings(10, 12, 650, elevatorCounts, passengerPercentages);
    }

    public static SimulatorSettings load(Path path) {
        SimulatorSettings defaults = defaults();
        if (!Files.exists(path)) {
            return defaults;
        }

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path)) {
            properties.load(reader);
        } catch (IOException ex) {
            return defaults;
        }

        Map<ElevatorType, Integer> elevatorCounts = new EnumMap<>(ElevatorType.class);
        elevatorCounts.put(ElevatorType.STANDARD, readInt(properties, "standard.elevators",
                defaults.getElevatorCount(ElevatorType.STANDARD)));
        elevatorCounts.put(ElevatorType.EXPRESS, readInt(properties, "express.elevators",
                defaults.getElevatorCount(ElevatorType.EXPRESS)));
        elevatorCounts.put(ElevatorType.FREIGHT, readInt(properties, "freight.elevators",
                defaults.getElevatorCount(ElevatorType.FREIGHT)));
        elevatorCounts.put(ElevatorType.GLASS, readInt(properties, "glass.elevators",
                defaults.getElevatorCount(ElevatorType.GLASS)));

        Map<PassengerType, Integer> passengerPercentages = new EnumMap<>(PassengerType.class);
        passengerPercentages.put(PassengerType.STANDARD, readInt(properties, "standard.passenger.percent",
                defaults.getPassengerPercentage(PassengerType.STANDARD)));
        passengerPercentages.put(PassengerType.VIP, readInt(properties, "vip.passenger.percent",
                defaults.getPassengerPercentage(PassengerType.VIP)));
        passengerPercentages.put(PassengerType.FREIGHT, readInt(properties, "freight.passenger.percent",
                defaults.getPassengerPercentage(PassengerType.FREIGHT)));
        passengerPercentages.put(PassengerType.GLASS, readInt(properties, "glass.passenger.percent",
                defaults.getPassengerPercentage(PassengerType.GLASS)));

        return new SimulatorSettings(
                readInt(properties, "floors", defaults.totalFloors()),
                readInt(properties, "passenger.count", defaults.generatedPassengerCount()),
                readInt(properties, "tick.millis", defaults.tickMillis()),
                elevatorCounts,
                passengerPercentages
        );
    }

    private static int readInt(Properties properties, String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static Map<ElevatorType, Integer> sanitizeCounts(Map<ElevatorType, Integer> counts) {
        Map<ElevatorType, Integer> sanitized = new EnumMap<>(ElevatorType.class);
        for (ElevatorType type : ElevatorType.values()) {
            sanitized.put(type, Math.max(0, counts.getOrDefault(type, 0)));
        }
        return sanitized;
    }

    private static Map<PassengerType, Integer> sanitizePercentages(Map<PassengerType, Integer> percentages) {
        Map<PassengerType, Integer> sanitized = new EnumMap<>(PassengerType.class);
        for (PassengerType type : PassengerType.values()) {
            sanitized.put(type, Math.max(0, percentages.getOrDefault(type, 0)));
        }
        return sanitized;
    }

    public int totalFloors() {
        return totalFloors;
    }

    public int minFloor() {
        return 0;
    }

    public int maxFloor() {
        return totalFloors - 1;
    }

    public int generatedPassengerCount() {
        return generatedPassengerCount;
    }

    public int tickMillis() {
        return tickMillis;
    }

    public int getElevatorCount(ElevatorType type) {
        return elevatorCounts.getOrDefault(type, 0);
    }

    public int getPassengerPercentage(PassengerType type) {
        return passengerPercentages.getOrDefault(type, 0);
    }

    public Map<ElevatorType, Integer> elevatorCounts() {
        return Map.copyOf(elevatorCounts);
    }

    public Map<PassengerType, Integer> passengerPercentages() {
        return Map.copyOf(passengerPercentages);
    }
}
