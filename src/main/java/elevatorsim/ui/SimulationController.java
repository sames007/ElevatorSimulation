package elevatorsim.ui;

import elevatorsim.model.Direction;
import elevatorsim.model.PassengerType;
import elevatorsim.model.elevator.Elevator;
import elevatorsim.simulation.Simulation;
import elevatorsim.simulation.SimulatorSettings;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class SimulationController {
    @FXML
    private GridPane buildingGrid;
    @FXML
    private Button startPauseButton;
    @FXML
    private Label simulationStateLabel;
    @FXML
    private ComboBox<PassengerType> passengerTypeComboBox;
    @FXML
    private Spinner<Integer> startFloorSpinner;
    @FXML
    private Spinner<Integer> endFloorSpinner;
    @FXML
    private VBox elevatorStatusBox;
    @FXML
    private TextArea eventLogTextArea;

    private final Map<String, Map<Integer, Label>> elevatorCells = new HashMap<>();
    private Simulation simulation;
    private SimulatorSettings settings;
    private Timeline timeline;
    private ScenarioMode scenarioMode = ScenarioMode.NONE;
    private boolean scenarioPassengerInjected;

    @FXML
    private void initialize() {
        settings = SimulatorSettings.load(Path.of("settings.txt"));
        simulation = new Simulation(settings);
        timeline = new Timeline(new KeyFrame(Duration.millis(settings.tickMillis()), event -> advanceSimulation()));
        timeline.setCycleCount(Animation.INDEFINITE);

        passengerTypeComboBox.getItems().setAll(PassengerType.values());
        passengerTypeComboBox.getSelectionModel().select(PassengerType.STANDARD);
        startFloorSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                settings.minFloor(), settings.maxFloor(), settings.minFloor()));
        endFloorSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                settings.minFloor(), settings.maxFloor(), Math.min(5, settings.maxFloor())));

        buildGrid();
        refreshView();
    }

    @FXML
    private void handleStartPause() {
        if (timeline.getStatus() == Animation.Status.RUNNING) {
            timeline.pause();
        } else {
            timeline.play();
        }
        refreshView();
    }

    @FXML
    private void handleStep() {
        timeline.pause();
        advanceSimulation();
    }

    @FXML
    private void handleReset() {
        timeline.stop();
        scenarioMode = ScenarioMode.NONE;
        scenarioPassengerInjected = false;
        simulation.reset();
        refreshView();
    }

    @FXML
    private void handleRequestPassenger() {
        PassengerType passengerType = passengerTypeComboBox.getValue();
        int startFloor = startFloorSpinner.getValue();
        int endFloor = endFloorSpinner.getValue();
        requestPassenger(passengerType, startFloor, endFloor);
    }

    @FXML
    private void handleGeneratePassengers() {
        simulation.generateRandomPassengers();
        refreshView();
    }

    @FXML
    private void handleSameDirectionScenario() {
        startScenario(ScenarioMode.SAME_DIRECTION);
    }

    @FXML
    private void handleOppositeDirectionScenario() {
        startScenario(ScenarioMode.OPPOSITE_DIRECTION);
    }

    private void advanceSimulation() {
        simulation.tick();
        injectScenarioPassengerIfNeeded();
        refreshView();
    }

    public void prepareOverviewForCapture() {
        timeline.stop();
        scenarioMode = ScenarioMode.NONE;
        scenarioPassengerInjected = false;
        simulation.reset();
        requestCapturePassenger(PassengerType.STANDARD, 0, 5);
        requestCapturePassenger(PassengerType.VIP, 3, 8);
        requestCapturePassenger(PassengerType.FREIGHT, 6, 1);
        requestCapturePassenger(PassengerType.GLASS, 2, 7);
        advanceForCapture(4);
    }

    public void prepareWalkthroughScenarioForCapture(boolean sameDirection) {
        prepareScenarioForCapture(sameDirection ? ScenarioMode.SAME_DIRECTION : ScenarioMode.OPPOSITE_DIRECTION);
    }

    public void advanceForCapture(int ticks) {
        timeline.stop();
        for (int i = 0; i < ticks; i++) {
            advanceSimulation();
        }
        refreshView();
    }

    private void startScenario(ScenarioMode mode) {
        timeline.stop();
        prepareScenarioForCapture(mode);
        timeline.play();
        refreshView();
    }

    private void prepareScenarioForCapture(ScenarioMode mode) {
        simulation.reset();
        scenarioMode = mode;
        scenarioPassengerInjected = false;

        int destinationFloor = Math.min(5, settings.maxFloor());
        requestCapturePassenger(PassengerType.STANDARD, settings.minFloor(), destinationFloor);
        refreshView();
    }

    private void injectScenarioPassengerIfNeeded() {
        if (scenarioMode == ScenarioMode.NONE || scenarioPassengerInjected) {
            return;
        }

        boolean elevatorReachedFirstFloor = simulation.getElevators().stream()
                .anyMatch(elevator -> elevator.getCurrentFloor() >= Math.min(1, settings.maxFloor()));
        if (!elevatorReachedFirstFloor || settings.maxFloor() < 2) {
            return;
        }

        if (scenarioMode == ScenarioMode.SAME_DIRECTION) {
            requestPassenger(PassengerType.STANDARD, 2, Math.min(4, settings.maxFloor()));
        } else if (scenarioMode == ScenarioMode.OPPOSITE_DIRECTION) {
            requestPassenger(PassengerType.STANDARD, 2, settings.minFloor());
        }
        scenarioPassengerInjected = true;
    }

    private void requestPassenger(PassengerType passengerType, int startFloor, int endFloor) {
        if (startFloor == endFloor) {
            simulation.addEvent("Ignored request because start and destination are the same floor.");
            refreshView();
            return;
        }

        try {
            simulation.requestPassenger(passengerType, startFloor, endFloor);
        } catch (IllegalArgumentException ex) {
            simulation.addEvent(ex.getMessage());
        }
        refreshView();
    }

    private void requestCapturePassenger(PassengerType passengerType, int startFloor, int endFloor) {
        int safeStart = clampFloor(startFloor);
        int safeEnd = clampFloor(endFloor);
        if (safeStart == safeEnd) {
            safeEnd = safeStart == settings.maxFloor() ? settings.minFloor() : settings.maxFloor();
        }
        simulation.requestPassenger(passengerType, safeStart, safeEnd);
    }

    private int clampFloor(int floor) {
        return Math.max(settings.minFloor(), Math.min(settings.maxFloor(), floor));
    }

    private void requestFromFloorButton(int floor, Direction direction) {
        int destinationFloor = direction == Direction.UP ? settings.maxFloor() : settings.minFloor();
        startFloorSpinner.getValueFactory().setValue(floor);
        endFloorSpinner.getValueFactory().setValue(destinationFloor);
        requestPassenger(passengerTypeComboBox.getValue(), floor, destinationFloor);
    }

    private void buildGrid() {
        buildingGrid.getChildren().clear();
        buildingGrid.getColumnConstraints().clear();
        elevatorCells.clear();

        addColumn(84, 84, Priority.NEVER);
        addColumn(76, 78, Priority.NEVER);
        addColumn(84, 86, Priority.NEVER);
        for (int i = 0; i < simulation.getElevators().size(); i++) {
            addColumn(98, 140, Priority.ALWAYS);
        }

        buildingGrid.add(headerLabel("Floor"), 0, 0);
        buildingGrid.add(headerLabel("Up"), 1, 0);
        buildingGrid.add(headerLabel("Down"), 2, 0);

        for (int i = 0; i < simulation.getElevators().size(); i++) {
            Elevator elevator = simulation.getElevators().get(i);
            buildingGrid.add(headerLabel(compactElevatorName(elevator)), 3 + i, 0);
            elevatorCells.put(elevator.getId(), new HashMap<>());
        }

        int row = 1;
        for (int floor = settings.maxFloor(); floor >= settings.minFloor(); floor--) {
            final int selectedFloor = floor;
            Label floorLabel = new Label(floorName(floor));
            floorLabel.getStyleClass().add("floor-label");
            buildingGrid.add(floorLabel, 0, row);

            Button upButton = floorButton("Up");
            upButton.setDisable(floor == settings.maxFloor());
            upButton.setOnAction(event -> requestFromFloorButton(selectedFloor, Direction.UP));
            buildingGrid.add(upButton, 1, row);

            Button downButton = floorButton("Down");
            downButton.setDisable(floor == settings.minFloor());
            downButton.setOnAction(event -> requestFromFloorButton(selectedFloor, Direction.DOWN));
            buildingGrid.add(downButton, 2, row);

            for (int elevatorIndex = 0; elevatorIndex < simulation.getElevators().size(); elevatorIndex++) {
                Elevator elevator = simulation.getElevators().get(elevatorIndex);
                Label cell = new Label();
                cell.setMaxWidth(Double.MAX_VALUE);
                cell.setMinHeight(42);
                cell.getStyleClass().add("elevator-cell");
                buildingGrid.add(cell, 3 + elevatorIndex, row);
                GridPane.setHalignment(cell, HPos.CENTER);
                elevatorCells.get(elevator.getId()).put(floor, cell);
            }
            row++;
        }
    }

    private void addColumn(double minWidth, double prefWidth, Priority grow) {
        ColumnConstraints constraints = new ColumnConstraints();
        constraints.setMinWidth(minWidth);
        constraints.setPrefWidth(prefWidth);
        constraints.setHgrow(grow);
        buildingGrid.getColumnConstraints().add(constraints);
    }

    private Label headerLabel(String text) {
        Label label = new Label(text);
        label.setMaxWidth(Double.MAX_VALUE);
        label.getStyleClass().add("grid-header");
        return label;
    }

    private Button floorButton(String text) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("floor-button");
        return button;
    }

    private void refreshView() {
        boolean running = timeline.getStatus() == Animation.Status.RUNNING;
        startPauseButton.setText(running ? "Pause" : "Start");
        simulationStateLabel.setText("Tick " + simulation.getTickCount()
                + " | completed " + simulation.getCompletedPassengers().size()
                + " | waiting " + simulation.totalWaitingRequests());

        refreshGrid();
        refreshElevatorStatus();
        eventLogTextArea.setText(String.join(System.lineSeparator(), simulation.getEventLog()));
        eventLogTextArea.setScrollTop(Double.MAX_VALUE);
    }

    private void refreshGrid() {
        for (Map<Integer, Label> cellsByFloor : elevatorCells.values()) {
            for (Label cell : cellsByFloor.values()) {
                cell.setText("");
                cell.getStyleClass().removeAll("active-elevator", "doors-open", "standard-elevator",
                        "express-elevator", "freight-elevator", "glass-elevator");
            }
        }

        for (Elevator elevator : simulation.getElevators()) {
            Label cell = elevatorCells.get(elevator.getId()).get(elevator.getCurrentFloor());
            if (cell == null) {
                continue;
            }
            cell.setText(compactElevatorName(elevator) + System.lineSeparator()
                    + elevator.getDirection().label()
                    + " | " + elevator.getPassengers().size() + "/" + elevator.getCapacity());
            cell.getStyleClass().addAll("active-elevator", styleClassFor(elevator));
            if (elevator.isDoorsOpen()) {
                cell.getStyleClass().add("doors-open");
            }
        }

        for (int floor = settings.minFloor(); floor <= settings.maxFloor(); floor++) {
            int upCount = simulation.waitingRequestCount(floor, Direction.UP);
            int downCount = simulation.waitingRequestCount(floor, Direction.DOWN);
            markWaitingRequests(floor, upCount, downCount);
        }
    }

    private void markWaitingRequests(int floor, int upCount, int downCount) {
        int row = settings.maxFloor() - floor + 1;
        buildingGrid.getChildren().stream()
                .filter(node -> GridPane.getRowIndex(node) != null && GridPane.getRowIndex(node) == row)
                .filter(node -> GridPane.getColumnIndex(node) != null)
                .forEach(node -> {
                    Integer column = GridPane.getColumnIndex(node);
                    if (column == 1 && node instanceof Button button) {
                        button.setText(upCount > 0 ? "Up (" + upCount + ")" : "Up");
                    }
                    if (column == 2 && node instanceof Button button) {
                        button.setText(downCount > 0 ? "Down (" + downCount + ")" : "Down");
                    }
                });
    }

    private void refreshElevatorStatus() {
        elevatorStatusBox.getChildren().clear();
        for (Elevator elevator : simulation.getElevators()) {
            Label status = new Label(elevator.getId()
                    + " | floor " + elevator.getCurrentFloor()
                    + " | " + elevator.getDirection().label()
                    + " | load " + elevator.getPassengers().size() + "/" + elevator.getCapacity()
                    + System.lineSeparator() + elevator.describeTargets());
            status.setMaxWidth(Double.MAX_VALUE);
            status.getStyleClass().add("status-card");
            elevatorStatusBox.getChildren().add(status);
        }
    }

    private String styleClassFor(Elevator elevator) {
        return switch (elevator.getType()) {
            case STANDARD -> "standard-elevator";
            case EXPRESS -> "express-elevator";
            case FREIGHT -> "freight-elevator";
            case GLASS -> "glass-elevator";
        };
    }

    private String floorName(int floor) {
        return floor == 0 ? "Ground" : "Floor " + floor;
    }

    private String compactElevatorName(Elevator elevator) {
        return elevator.getId().replace("Elevator-", " ");
    }

    private enum ScenarioMode {
        NONE,
        SAME_DIRECTION,
        OPPOSITE_DIRECTION
    }
}
