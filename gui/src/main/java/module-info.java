module com.energy.gui {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;


    opens com.energy.gui to javafx.fxml;
    exports com.energy.gui;
}
