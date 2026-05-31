module elevator.simulation {
    requires java.desktop;
    requires javafx.controls;
    requires javafx.fxml;

    exports elevatorsim;
    exports elevatorsim.model;
    exports elevatorsim.model.elevator;
    exports elevatorsim.model.passenger;
    exports elevatorsim.simulation;
    exports elevatorsim.tools;

    opens elevatorsim.ui to javafx.fxml;
}
