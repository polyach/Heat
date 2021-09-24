package advancedmedia;

import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;

public interface ControllerInterface {
    //public void initiateController();
    public GridPane getGrid();
    public StackPane getStackpane();
    public void setCanvasSize(double x, double y);
    //public void stop();
}
