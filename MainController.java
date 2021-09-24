package advancedmedia;

import javafx.application.*;
import javafx.scene.*;
import javafx.stage.*;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.geometry.*;

public class MainController extends Application {

    private double sceneWidthDiff;                  // Разница между размерами Stage и Scene
    private double sceneHeightDiff;
    private double gridHeight;                      // Высота Gridpane, содержащая контроллер управления

    private Controller2D controller2D;              // Экземпляр класса контроллера для двумерной задачи
    private Controller3D controller3D;              // Экземпляр класса контроллера для трехмерной задачи
    private ControllerInterface controller;         // Интерфейс для доступа к любому из контроллеров
    boolean is2D = false;                           // Флаг режима 2D/3D

    // Элементы представления и управления
    private VBox rootNode;                          // Корневой контейнер
    private GridPane grid;                          // Панель управления (создается в контроллерах)
    private StackPane stackPane;                    // Панель с канвасом и контролом управления цветом (создается в контроллерах)
    RadioButton radio2D;                            // Радиокнопки для переключения режимов 2D/3D
    RadioButton radio3D;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() throws Exception {

        System.out.println("init(): " + Thread.currentThread());

        if (is2D) {
            controller = controller2D = new Controller2D();      // Инициализация двумерной задачи (запрос массивов, заполнение их данными модели, факторизация матрицы)
        } else {
            controller = controller3D = new Controller3D();      // Инициализация трехмерной задачи
        }
    }

    @Override
    public void stop() throws Exception {
        if (controller2D != null) {
            controller2D.stop();
        }
        if (controller3D != null) {
            controller3D.stop();
        }
        super.stop();
    }

    @Override
    public void start(Stage myStage) {

        System.out.println("start(): " + Thread.currentThread());
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

        if (is2D) {
            myStage.setTitle("2D задача распространения тепла");
        } else {
            myStage.setTitle("3D задача распространения тепла");
        }

        radio2D = new RadioButton("2D");
        radio3D = new RadioButton("3D");
        if (is2D) {
            radio2D.setSelected(true);
        } else {
            radio3D.setSelected(true);
        }
        ToggleGroup grp = new ToggleGroup();
        radio2D.setToggleGroup(grp);
        radio3D.setToggleGroup(grp);
//        radio2D.setOnAction((event) -> {
//            if (radio2D.isSelected()) {
//                is2D = true;
//                if (controller2D == null) {
//                    controller2D = new Controller2D();
//                }
//                controller = controller2D;
//                controller2D.setVisible(true);
//                controller3D.setVisible(false);
//                myStage.setTitle("2D задача распространения тепла");
//            } 
//            else {
//                is2D = false;
//                if (controller3D == null) {
//                    controller3D = new Controller3D();
//                }
//                controller = controller3D;
//                controller2D.setVisible(false);
//                controller3D.setVisible(true);
//            }
//            redrawAll(myStage);
//        });
//        radio3D.setOnAction((event) -> {
//            if (radio3D.isSelected()) {
//                is2D = false;
//                if (controller3D == null) {
//                    controller3D = new Controller3D();
//                }
//                controller = controller3D;
//                controller2D.setVisible(false);
//                controller3D.setVisible(true);
//                myStage.setTitle("3D задача распространения тепла");
//            } 
//            else {
//                is2D = true;
//                if (controller2D == null) {
//                    controller2D = new Controller2D();
//                }
//                controller = controller2D;
//                controller2D.setVisible(true);
//                controller3D.setVisible(false);
//            }
//            redrawAll(myStage);
//        });

        grp.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (radio2D.isSelected()) {
                is2D = true;
                if (controller2D == null) {
                    controller2D = new Controller2D();
                }
                controller = controller2D;
                controller2D.setVisible(true);
                controller3D.setVisible(false);
                myStage.setTitle("2D задача распространения тепла");
            } else {
                is2D = false;
                if (controller3D == null) {
                    controller3D = new Controller3D();
                }
                controller = controller3D;
                controller2D.setVisible(false);
                controller3D.setVisible(true);
                myStage.setTitle("3D задача распространения тепла");
            }
            redrawAll(myStage);
        });

        // Инициализация максимально возможного окна
        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
        myStage.setX(primaryScreenBounds.getMinX());
        myStage.setY(primaryScreenBounds.getMinY());
        myStage.setWidth(primaryScreenBounds.getWidth());
        myStage.setHeight(primaryScreenBounds.getHeight());
        myStage.setMinWidth(600);
        myStage.setMinHeight(500);

        // Создание пустой сцены в первый раз для определения параметров рисования
        Scene myScene = new Scene(new VBox(controller.getGrid()));
        myStage.setScene(myScene);
        myStage.show();
        sceneWidthDiff = myStage.getWidth() - myScene.getWidth();
        sceneHeightDiff = myStage.getHeight() - myScene.getHeight();
        gridHeight = controller.getGrid().getHeight();

        
        myStage.widthProperty().addListener((observable, oldValue, newValue) -> {   // Добавление слушателей к свойствам графических размеров
            updateSizeParams(myStage);
        });
        myStage.heightProperty().addListener((observable, oldValue, newValue) -> {
            updateSizeParams(myStage);
        });

        redrawAll(myStage);                                                         // Отрисовка полной сцены в первый раз
        
        if(is2D)
            radio2D.requestFocus();
        else
            radio3D.requestFocus();
    }

    public void redrawAll(Stage stage) {

        updateSizeParams(stage);                                                // Получить параметры stage и найти размеры холста

        rootNode = new VBox();
        rootNode.setAlignment(Pos.CENTER);
        Scene myScene = new Scene(rootNode);

        grid = controller.getGrid();                                            // Панель управления представлением берется из текущего контроллера
        stackPane = controller.getStackpane();                                  // Панель вывода канваса и контрола управления масштабом температуры также берем из контроллера
        grid.add(radio2D, 0, 0);
        grid.add(radio3D, 0, 1);
        grid.setMargin(radio2D, new Insets(8, 20, 0, 20));
        grid.setMargin(radio3D, new Insets(0, 20, 8, 20));
        //grid.setGridLinesVisible(true);
        rootNode.getChildren().addAll(stackPane, grid);                         // Компоновка элементов в rootNode
        stage.setScene(myScene);
        stage.show();
    }

    public void updateSizeParams(Stage myStage) {

        double canvasX = myStage.getWidth() - sceneWidthDiff;;
        double canvasY = myStage.getHeight() - sceneHeightDiff - gridHeight - 20;

        controller.setCanvasSize(canvasX, canvasY);
    }
}
