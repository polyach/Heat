package advancedmedia;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.*;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.geometry.*;
import javafx.scene.shape.*;
import javafx.scene.canvas.*;
import javafx.scene.paint.*;
import javafx.util.Duration;
import javafx.animation.PathTransition;
import javafx.animation.Timeline;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class Controller3D implements ControllerInterface {

    static int mono = 0;
    static int grd = 0;

    private double canvasX, canvasY;                // Размеры канваса - вычисляются динамически в зависимости от текущего размера окна
    private double scrAxeSize;                      // Длины осей в экранных единицах

    private Heat3D model;                           // Экземпляр класса модели - Heat3D
    private CurrentProjection projection;           // Матрица проецирования физических координат на координаты экрана
    private ViewSolution3D viewSolution3D;          // Класс, содержащий интерполированное решение (создается из настоящего решения модели)

    // Элементы представления и контроллера
    private GridPane grid;                          // Контейнер для элементов управления представлением
    private StackPane stackPane;                    // Контенер для вывода представления и контрола управления цветом
    private Canvas mainCanvas = new Canvas();       // Канвас для отрисовки представления
    private Label timeLabel;                        // Текущее время модели
    private RadioButton colorRadio;                 // Контролы для переключения между режимами цветной/черно-белый
    private RadioButton gradRadio;
    private ToggleGroup colorGroup;
    private Button btnStart;                        // Кнопка Start/Pause
    private Label TValueLabel;                      // Значение температуры в вырезе

    private double currFi = 45;                     // Начальное значение углов проецирования
    private double teta0 = 30;

//    final private double X = 1;                   // Максимальные значения по осям в представлении (могут отличаться от значений модели - тогда представление будет отмасштабировано)
//    final private double Y = 1;
//    final private double Z = 1;
//    final private double X = 0.05;                  // Максимальные значения по осям в представлении (могут отличаться от значений модели - тогда представление будет отмасштабировано)
//    final private double Y = 0.05;
//    final private double Z = 0.05;
    final private double X = 0.13;                  // Максимальные значения по осям в представлении (могут отличаться от значений модели - тогда представление будет отмасштабировано)
    final private double Y = 0.13;
    final private double Z = 0.13;

    // Контролы для управления масштабом цвета
    private boolean isGradient = false;             // Флаг для преключения режима рисования с градиентной заливкой элементов
    final private double COLOR_HEIGHT = 150;        // Высота градиентной шкалы в контроле управления цветным масштабом
    final private int colorNumber = 200;            // Количество оттенков для рисования элементов между Tmin и Tmax
    private ArrayList<Color> colors;                // Список оттенков от синего до красного
    private VBox colorsPane;                        // Контейнер для всех элементов контрола
    private TextField highTemp, lowTemp;            // Поля для ввода значений температуры
    private CheckBox tempCheckBox;                  // Переключение между режимами auto/manual

    private double Tmax = 0;                        // Максимум и минимум функции T на текущем временном слое
    private double Tmin = 0;

    private boolean isCalculating = false;          // Флаг запуска/остановки цикла расчета
    private boolean isVisible = true;               // Флаг виден/не виден для отключения отрисовки в невидимом режиме

    private boolean isSceneDrawn = false;           // Флаг, означающий что сцена отрисована
    private boolean isSceneDrawQueued = false;      // Флаг, означающий постановку сцены в очередь на отрисовку

    private ExecutorService executorDrawService // ExecutorService для отрисовки канваса
            = Executors.newFixedThreadPool(2);
    private ExecutorService execService;            // ExecutorService для выполнения цикла расчета модели
    private RedrawService redrawService;            // Service, где происходит отрисовка
    private MyCalcService calcService;              // Service, где происходит расчет

    private Timer timer;                            // Timer для вывода канваса на экран

    private long refreshTime // Для отслеживания частоты отрисовки канваса
            = System.currentTimeMillis();
    private boolean realTime = false;               // Флаг переключения в реальный режим времени в модели
    private long workingStart // Временная точка начала расчета  
            = System.currentTimeMillis();

    Point3DT[] point3 = new Point3DT[4];              // Рабочие массивы
    //Point3DT[] triang = new Point3DT[3];              // Треугольник для работы с градиентами
    Point3DT center = new Point3DT();                 // Центр заливаемого элемента
    double[] workX = new double[4];
    double[] workY = new double[4];

    public Controller3D() {

        System.out.println("Controller3D(): " + Thread.currentThread());

        for (int i = 0; i < 4; i++) {                       // Рабочий массив из 3D-точек для отрисовки в разных методах
            point3[i] = new Point3DT();
        }

        colors = new ArrayList<>();                         // Шкала оттенков для отрисовки цветной поверхности
        for (int i = 0; i < colorNumber; i++) {
            colors.add(new Color(i * 1. / (colorNumber - 1), 0., 1. - i * 1. / (colorNumber - 1), 1.));
        }

        model = new Heat3D();                               // Инициализация модели (запрос массивов, заполнение их данными модели, факторизация матрицы)

        makeColorControl();                                 // Создание контрола управления масштабом температуры

        makeGridControl();                                  // Создание панели управления представлением

        makeViewPane();                                     // Создание панели вывода представления

        makeProjection();                                   // Инициализация матрицы проецирования на экран
        
        viewSolution3D = new ViewSolution3D(model.getSolution().getPrevSol());

        btnStart.requestFocus();

//        final Rectangle rectPath = new Rectangle(0, 0, 40, 40);
//        rectPath.setArcHeight(10);
//        rectPath.setArcWidth(10);
//        rectPath.setFill(Color.ORANGE);
//        Path path = new Path();
//        path.getElements().add(new MoveTo(20, 20));
//        path.getElements().add(new CubicCurveTo(380, 0, 380, 120, 200, 120));
//        path.getElements().add(new CubicCurveTo(0, 120, 0, 240, 380, 240));
//        PathTransition pathTransition = new PathTransition();
//        pathTransition.setDuration(Duration.millis(4000));
//        pathTransition.setPath(path);
//        pathTransition.setNode(rectPath);
//        pathTransition.setOrientation(PathTransition.OrientationType.ORTHOGONAL_TO_TANGENT);
//        pathTransition.setCycleCount(Timeline.INDEFINITE);
//        //pathTransition.setAutoReverse(true);
//        pathTransition.play();
//
//        stackPane.getChildren().add(rectPath);
    }

    public void makeColorControl() {    // Создание контрола управления масштабом тепмературы

        Canvas colorCanvas = new Canvas(10, COLOR_HEIGHT);      // Столбик с градиентом цвета от синего до красного для управления масштабом отображения температуры по оси T
        GraphicsContext gcColor = colorCanvas.getGraphicsContext2D();
        for (int i = 0; i < colorNumber; i++) {
            Color color = colors.get(colorNumber - i - 1);
            gcColor.setFill(color);
            double dh = COLOR_HEIGHT / colorNumber;
            gcColor.fillRect(0., i * dh, 10., dh);
        }

        // *********************************************************************
        // Панель масштаба температуры
        highTemp = new TextField("High");                       // Поля для ввода значений температуры
        highTemp.setAlignment(Pos.CENTER);
        highTemp.setMaxWidth(50);
        highTemp.setEditable(false);
        highTemp.setOnAction((event) -> {
            String s = highTemp.getText();
            try {
                double hTemp = Double.parseDouble(s.replace(',', '.'));
                Tmax = hTemp;
                drawAllInRedrawService();
            } catch (Exception ex) {
                highTemp.setText(String.format("%.2f", Math.rint(Tmax * 100) / 100));
            }
        });

        lowTemp = new TextField("Low");
        lowTemp.setAlignment(Pos.CENTER);
        lowTemp.setMaxWidth(50);
        lowTemp.setEditable(false);
        lowTemp.setOnAction((event) -> {
            String s = lowTemp.getText();
            try {
                double lTemp = Double.parseDouble(s.replace(',', '.'));
                Tmin = lTemp;
                drawAllInRedrawService();
            } catch (Exception ex) {
                lowTemp.setText(String.format("%.2f", Math.rint(Tmin * 100) / 100));
            }
        });

        Label tempLabel = new Label("Scale");                   // Надпись сверху
        tempLabel.setMinHeight(30);

        tempCheckBox = new CheckBox("Auto");
        tempCheckBox.setSelected(true);
        tempCheckBox.setOnAction((event) -> {
            if (tempCheckBox.isSelected()) {
                highTemp.setEditable(false);
                lowTemp.setEditable(false);
            } else {
                highTemp.setEditable(true);
                lowTemp.setEditable(true);
            }
            drawAllInRedrawService();
        });

        colorsPane = new VBox(tempLabel, highTemp, // Панель, образующая контрол масштабирования температурной шкалы и вмещающая все элементы управления ей
                colorCanvas, lowTemp, tempCheckBox);
        colorsPane.setAlignment(Pos.TOP_CENTER);
        colorsPane.setMaxWidth(70);
        //colorsPane.setMaxHeight(COLOR_HEIGHT + 100);
        //colorsPane.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
    }

    public void makeGridControl() { // Создание панели управления представлением

        grid = new GridPane();
        grid.setAlignment(Pos.CENTER);

        timeLabel = new Label("Текущее время:  " + 0);

        CheckBox timeCheck = new CheckBox("В реальном времени");
        timeCheck.setOnAction((event) -> {
            if (timeCheck.isSelected()) {
                realTime = true;
            } else {
                realTime = false;
            }
            model.setRealTime(realTime);
        });

        Label labelFi = new Label("φ = " + String.format("%.0f", currFi));
        Slider sliderFi = new Slider(0, 360, currFi);
        sliderFi.setShowTickMarks(true);
        sliderFi.setShowTickLabels(true);
        sliderFi.setMajorTickUnit(20);
        sliderFi.valueProperty().addListener((observable) -> {
            projection.setFiTeta(sliderFi.getValue(), projection.getTeta());
            labelFi.setText("φ = " + String.format("%.0f", projection.getFi()));
            drawAllInRedrawService();
        });

        Label labelTeta = new Label("θ = " + String.format("%.0f", teta0));
        Slider sliderTeta = new Slider(0, 90, teta0);
        sliderTeta.setShowTickMarks(true);
        sliderTeta.setShowTickLabels(true);
        sliderTeta.setMajorTickUnit(10);
        sliderTeta.valueProperty().addListener((observable) -> {
            projection.setFiTeta(projection.getFi(), sliderTeta.getValue());
            labelTeta.setText("θ = " + String.format("%.0f", projection.getTeta()));
            drawAllInRedrawService();
        });

        grid.add(timeLabel, 1, 0);
        grid.setMargin(timeLabel, new Insets(0, 10, 0, 10));

        grid.add(timeCheck, 1, 1);
        grid.setMargin(timeCheck, new Insets(0, 10, 0, 10));

        grid.add(labelFi, 2, 0);
        grid.add(sliderFi, 3, 0);
        //grid.setFillWidth(sliderFi, true);
        grid.add(labelTeta, 2, 1);
        grid.add(sliderTeta, 3, 1);
        //grid.setFillWidth(sliderTeta, true);

        colorGroup = new ToggleGroup();
        colorRadio = new RadioButton("В дискр. цвете");
        colorRadio.setToggleGroup(colorGroup);
        colorRadio.setSelected(true);
        gradRadio = new RadioButton("В град. цвете");
        gradRadio.setToggleGroup(colorGroup);
//        colorRadio.setOnAction((event) -> {
//            if (colorRadio.isSelected()) {
//                isGradient = false;
//            } else {
//                isGradient = true;
//            }
//            drawAllInRedrawService();
//        });
//        gradRadio.setOnAction((event) -> {
//            if (gradRadio.isSelected()) {
//                isGradient = true;
//            } else {
//                isGradient = false;
//            }
//            drawAllInRedrawService();
//        });
        colorGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (colorRadio.isSelected()) {
                isGradient = false;
            } else {
                isGradient = true;
            }
            drawAllInRedrawService();
        });

        grid.add(colorRadio, 4, 0);
        grid.add(gradRadio, 4, 1);
        grid.setMargin(colorRadio, new Insets(8, 10, 0, 10));
        grid.setMargin(gradRadio, new Insets(0, 10, 8, 10));

        Label xLabel = new Label("x: ");
        Label yLabel = new Label("y: ");
        Label zLabel = new Label("z: ");
        Label TLabel = new Label("T: ");
        TValueLabel = new Label("-----");

        Slider sliderCutX = new Slider(0, 1, 0.5);
        sliderCutX.setShowTickMarks(true);
        sliderCutX.setShowTickLabels(true);
        sliderCutX.setMajorTickUnit(.1);
        sliderCutX.setBlockIncrement(.05);
        sliderCutX.valueProperty().addListener((observable) -> {
            viewSolution3D.setxCut((int) (sliderCutX.getValue() * viewSolution3D.xGridSize));
            drawAllInRedrawService();
        });

        Slider sliderCutY = new Slider(0, 1, 0.5);
        sliderCutY.setShowTickMarks(true);
        sliderCutY.setShowTickLabels(true);
        sliderCutY.setMajorTickUnit(.1);
        sliderCutY.setBlockIncrement(.05);
        sliderCutY.valueProperty().addListener((observable) -> {
            viewSolution3D.setyCut((int) (sliderCutY.getValue() * viewSolution3D.yGridSize));
            drawAllInRedrawService();
        });

        Slider sliderCutZ = new Slider(0, 1, 0.5);
        sliderCutZ.setShowTickMarks(true);
        sliderCutZ.setShowTickLabels(true);
        sliderCutZ.setMajorTickUnit(.1);
        sliderCutZ.setBlockIncrement(.05);
        //sliderCutZ.setOrientation(Orientation.VERTICAL);
        sliderCutZ.valueProperty().addListener((observable) -> {
            viewSolution3D.setzCut((int) (sliderCutZ.getValue() * viewSolution3D.zGridSize));
            drawAllInRedrawService();
        });

        grid.add(xLabel, 5, 0);
        grid.add(yLabel, 5, 1);
        grid.add(zLabel, 7, 0);
        grid.add(TLabel, 7, 1);
        grid.add(sliderCutX, 6, 0);
        grid.add(sliderCutY, 6, 1);
        grid.add(sliderCutZ, 8, 0);
        grid.add(TValueLabel, 8, 1);

        grid.setMargin(zLabel, new Insets(0, 0, 0, 10));
        grid.setMargin(TLabel, new Insets(0, 0, 0, 10));
        grid.setMargin(TValueLabel, new Insets(0, 0, 0, 5));
        //grid.setRowSpan(sliderCutZ, 2);

        btnStart = new Button("Start");
        btnStart.setMinWidth(60);
        btnStart.setOnAction((event) -> {
            if (!isCalculating) {
                isCalculating = true;
                btnStart.setText("Pause");
                model.setStartTime(System.nanoTime());
                calcService = new MyCalcService();
                calcService.start();
                workingStart = System.currentTimeMillis();
                timer = new Timer("Timer thread", true);
                timer.scheduleAtFixedRate(new MyTimerTask(), 0, 20);
            } else {
                isCalculating = false;
                btnStart.setText("Start");
                calcService.cancel();
                timer.purge();
                timer.cancel();
            }
        });

        grid.add(btnStart, 9, 0);
        grid.setRowSpan(btnStart, 2);
        grid.setMargin(btnStart, new Insets(0, 10, 0, 10));
    }

    public void makeViewPane() {    // Создание панели вывода представления

        stackPane = new StackPane();                                // Панель вывода поверхности и контрола управления масштабом температуры        
        stackPane.alignmentProperty().set(Pos.TOP_LEFT);
        stackPane.getChildren().addAll(new Canvas(), colorsPane);
    }  // Добавляем нижним элементом пока пустой канвас, в дальнейшем он будет заменяться отрисованным на каждом временном слое

    public void makeProjection() {  // Инициализация матрицы проецирования на экран - вызывается в конструкторе, но перед началом работы должны быть установлены 
        projection = new CurrentProjection(currFi, teta0, // размеры методом setCanvasParams(double scrAxeSize, double xMid, double yMid)
                X, Y, Z);
    }

    // Структуры и параметры для отрисовки решений
    private class ViewSolution3D {                      // Класс для представления интерполированного решения и для манипуляций с ним

        private double[][][] solution;                  // Массив, содержащий решение для вывода в представлении

        public void setSolution(double[][][] solution) {
            synchronized (this.solution) {
                this.solution = solution.clone();
            }
        }

        public double[][][] getSolution() {
            return solution;
        }

        String drawOrder;                               // Последовательность вывода граней параллелепипеда

        private int xGridSize, yGridSize, zGridSize;    // Размеры оригинальной сетки

        public int getxGridSize() {
            return xGridSize;
        }

        public void setxGridSize(int xGridSize) {
            this.xGridSize = xGridSize;
        }

        public int getyGridSize() {
            return yGridSize;
        }

        public void setyGridSize(int yGridSize) {
            this.yGridSize = yGridSize;
        }

        public int getzGridSize() {
            return zGridSize;
        }

        public void setzGridSize(int zGridSize) {
            this.zGridSize = zGridSize;
        }

        private double xElemSize, yElemSize, zElemSize; // Размеры элемента сетки        

        private int xCut, yCut, zCut;                   // Координаты выреза

        public void setxCut(int xCut) {
            this.xCut = xCut;
        }

        public void setyCut(int yCut) {
            this.yCut = yCut;
        }

        public void setzCut(int zCut) {
            this.zCut = zCut;
        }

        public int getxCut() {
            return xCut;
        }

        public int getyCut() {
            return yCut;
        }

        public int getzCut() {
            return zCut;
        }

        Point3DT[] pnt3 = new Point3DT[6];              // Рабочий массив для использования при отрисовке полигонов

        public ViewSolution3D(double[][][] originalSolution) {

            System.out.println("new ViewSolution3D(): " + Thread.currentThread());

            synchronized (originalSolution) {
                solution = originalSolution;
            }

            for (int i = 0; i < pnt3.length; i++) {     // Инициализация рабочего массива
                pnt3[i] = new Point3DT();
            }

            xGridSize = originalSolution[0][0].length - 1;
            yGridSize = originalSolution[0].length - 1;
            zGridSize = originalSolution.length - 1;

            xCut = xGridSize / 2;
            yCut = yGridSize / 2;
            zCut = zGridSize / 2;

            xElemSize = model.getX() / xGridSize;       // Размеры элемента оригинальной сетки в координатах модели
            yElemSize = model.getY() / yGridSize;
            zElemSize = model.getZ() / zGridSize;

            calculateDrawOrder();                       // Определение порядка вывода плоскостей
        }

        private void calculateDrawOrder() {

            Point3D vectorR = new Point3D();

            if (0 <= currFi && currFi < 90) {
                drawOrder = "zyxZ";
            } else if (90 <= currFi && currFi < 180) {
                drawOrder = "zyXZ";
            } else if (180 <= currFi && currFi < 270) {
                drawOrder = "zXYZ";
            } else if (270 <= currFi && currFi < 360) {
                drawOrder = "zxYZ";
            }
        }

        private void redrawCanvas(double time, boolean isSimple) {

            System.out.println("redrawCanvas(): calling start time = " + (System.currentTimeMillis() - workingStart) + " " + Thread.currentThread());

            long time1 = System.currentTimeMillis();

            Canvas myNewCanvas = new Canvas(canvasX, canvasY);

            myNewCanvas.setUserData(time);

            if (currFi != projection.getFi()) {
                currFi = projection.getFi();
                calculateDrawOrder();
            }

            if (Thread.currentThread().isInterrupted()) {
                System.out.println("redrawCanvas(): interrupted = " + (System.currentTimeMillis() - workingStart) + " " + Thread.currentThread());
                return;
            }

            drawAxes(myNewCanvas, false);
            
//            Thread thread = new Thread(() -> drawAxes(myNewCanvas, false));
//            thread.start();
            

            if (Thread.currentThread().isInterrupted()) {
                System.out.println("redrawCanvas(): interrupted = " + (System.currentTimeMillis() - workingStart) + " " + Thread.currentThread());
                return;
            }

            if (!isSimple) {

                synchronized (this.solution) {
                    viewSolution3D.drawCube(myNewCanvas);
                }

                drawAxes(myNewCanvas, true);
            }

            if (Thread.currentThread().isInterrupted()) {
                System.out.println("redrawCanvas(): interrupted = " + (System.currentTimeMillis() - workingStart) + " " + Thread.currentThread());
                return;
            }

            synchronized (mainCanvas) {
                mainCanvas = myNewCanvas;
            }

            if (Platform.isFxApplicationThread()) {
                redrawScene();
            }

            System.out.println("redrawCanvas(): calling end time = " + (System.currentTimeMillis() - workingStart) + " " + Thread.currentThread()
                    + " time: " + (System.currentTimeMillis() - time1) + " priority = " + Thread.currentThread().getPriority());
        }

        public void drawCube(Canvas newCanvas) {    // Построение параллелепипеда

            mono = grd = 0;

            if (tempCheckBox.isSelected()) {        // Определение максимума и минимума

                Tmax = solution[0][0][0];
                Tmin = solution[0][0][0];
                for (int k = 0; k <= zGridSize; k++) {
                    for (int i = 0; i <= yGridSize; i++) {
                        for (int j = 0; j <= xGridSize; j++) {
                            Tmax = Math.max(Tmax, solution[k][i][j]);
                            Tmin = Math.min(Tmin, solution[k][i][j]);
                        }
                    }
                }
                if (Tmax - Tmin < 1e-10) {
                    Tmax = Tmin + 1e-10;
                }

                if (Platform.isFxApplicationThread()) {
                    highTemp.setText(String.format("%.2f", Math.rint(Tmax * 100) / 100));
                    lowTemp.setText(String.format("%.2f", Math.rint(Tmin * 100) / 100));
                } else {
                    Platform.runLater(() -> {
                        highTemp.setText(String.format("%.2f", Math.rint(Tmax * 100) / 100));
                        lowTemp.setText(String.format("%.2f", Math.rint(Tmin * 100) / 100));
                    });
                }
            }

            for (int i = 0; i < drawOrder.length(); i++) {
                switch (drawOrder.charAt(i)) {
                    case 'x':
                        drawPlainX(newCanvas, false);
                        break;
                    case 'X':
                        drawPlainX(newCanvas, true);
                        break;
                    case 'y':
                        drawPlainY(newCanvas, false);
                        break;
                    case 'Y':
                        drawPlainY(newCanvas, true);
                        break;
                    case 'z':
                        drawPlainZ(newCanvas, true);
                        break;
                    case 'Z':
                        drawPlainZ(newCanvas, false);
                        break;
                }
            }

//            pnt3[0].x = pnt3[3].x = X;
//            pnt3[1].x = pnt3[2].x = X * 1.5;
//            pnt3[0].y = pnt3[1].y = Y;
//            pnt3[2].y = pnt3[3].y = Y * 1.5;
//
//            pnt3[0].z = 0;
//            pnt3[1].z = 0;
//            pnt3[2].z = 0;
//            pnt3[3].z = 0;
//
//            pnt3[0].T = 10;
//            pnt3[1].T = 55;
//            pnt3[2].T = 100;
//            pnt3[3].T = 55;
//
//            drawPolygon(newCanvas, pnt3);
        }

        private void drawPlainX(Canvas newCanvas, boolean isZeroPlane) {

            int j;
            for (int k = 0; k < zGridSize; k++) {
                pnt3[0].z = pnt3[1].z = k * zElemSize;
                pnt3[2].z = pnt3[3].z = (k + 1) * zElemSize;

                for (int i = 0; i < yGridSize; i++) {
                    if (isZeroPlane) {                                          // Плоскость x = 0 
                        j = 0;
                    } else {
                        if (i >= yCut && k >= zCut) {                           // Плоскость выреза 
                            j = xCut;
                            if (j == 0) {
                                continue;
                            }
                        } else {                                                // Плоскость x = X
                            j = xGridSize;
                        }
                    }
                    pnt3[0].x = pnt3[1].x = pnt3[2].x = pnt3[3].x = j * xElemSize;

                    pnt3[0].y = pnt3[3].y = i * yElemSize;
                    pnt3[1].y = pnt3[2].y = (i + 1) * yElemSize;

                    pnt3[0].T = solution[k][i][j];
                    pnt3[1].T = solution[k][i + 1][j];
                    pnt3[2].T = solution[k + 1][i + 1][j];
                    pnt3[3].T = solution[k + 1][i][j];
                    
                    drawPolygon(newCanvas, pnt3);
                }

                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("drawCube(): interrupted = " + (System.currentTimeMillis() - workingStart) + " " + Thread.currentThread());
                    return;
                }
            }

            newCanvas.getGraphicsContext2D().setLineWidth(1);
            newCanvas.getGraphicsContext2D().setStroke(Color.BLACK);
            if (isGradient && isZeroPlane) {
                pnt3[0].x = pnt3[1].x = pnt3[2].x = pnt3[3].x = 0;
                pnt3[0].y = pnt3[1].y = 0;
                pnt3[2].y = pnt3[3].y = yGridSize * yElemSize;
                pnt3[0].z = pnt3[3].z = 0;
                pnt3[1].z = pnt3[2].z = zGridSize * zElemSize;
                drawLinePolygon(newCanvas, pnt3, 4);
            }
            if (isGradient && !isZeroPlane) {

                double yMax, zMax;
                if (zCut == 0) {
                    yMax = yCut * yElemSize;
                } else {
                    yMax = yGridSize * yElemSize;
                }
                if (yCut == 0) {
                    zMax = zCut * zElemSize;
                } else {
                    zMax = zGridSize * zElemSize;
                }

                pnt3[0].x = pnt3[1].x = pnt3[2].x = pnt3[3].x = pnt3[4].x = pnt3[5].x = xGridSize * xElemSize;
                pnt3[0].y = pnt3[1].y = 0;
                pnt3[2].y = pnt3[3].y = yCut * yElemSize;
                pnt3[4].y = pnt3[5].y = yMax;
                pnt3[0].z = pnt3[5].z = 0;
                pnt3[1].z = pnt3[2].z = zMax;
                pnt3[3].z = pnt3[4].z = zCut * zElemSize;
                drawLinePolygon(newCanvas, pnt3, 6);
            }
        }

        private void drawPlainY(Canvas newCanvas, boolean isZeroPlane) {

            int i;
            for (int k = 0; k < zGridSize; k++) {
                pnt3[0].z = pnt3[1].z = k * zElemSize;
                pnt3[2].z = pnt3[3].z = (k + 1) * zElemSize;

                for (int j = 0; j < xGridSize; j++) {
                    if (isZeroPlane) {                                          // Плоскость y = 0 
                        i = 0;
                    } else {
                        if (j >= xCut && k >= zCut) {
                            i = yCut;                                           // Плоскость выреза
                            if (i == 0) {
                                continue;
                            }
                        } else {                                                // Плоскость y = Y
                            i = yGridSize;
                        }
                    }

                    pnt3[0].y = pnt3[1].y = pnt3[2].y = pnt3[3].y = i * yElemSize;

                    pnt3[0].x = pnt3[3].x = j * xElemSize;
                    pnt3[1].x = pnt3[2].x = (j + 1) * xElemSize;

                    pnt3[0].T = solution[k][i][j];
                    pnt3[1].T = solution[k][i][j + 1];
                    pnt3[2].T = solution[k + 1][i][j + 1];
                    pnt3[3].T = solution[k + 1][i][j];
                    drawPolygon(newCanvas, pnt3);
                }

                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("drawCube(): interrupted = " + (System.currentTimeMillis() - workingStart) + " " + Thread.currentThread());
                    return;
                }
            }

            newCanvas.getGraphicsContext2D().setLineWidth(1);
            newCanvas.getGraphicsContext2D().setStroke(Color.BLACK);
            if (isGradient && isZeroPlane) {
                pnt3[0].y = pnt3[1].y = pnt3[2].y = pnt3[3].y = 0;
                pnt3[0].x = pnt3[1].x = 0;
                pnt3[2].x = pnt3[3].x = xGridSize * xElemSize;
                pnt3[0].z = pnt3[3].z = 0;
                pnt3[1].z = pnt3[2].z = zGridSize * zElemSize;
                drawLinePolygon(newCanvas, pnt3, 4);
            }
            if (isGradient && !isZeroPlane) {
                double xMax, zMax;
                if (zCut == 0) {
                    xMax = xCut * xElemSize;
                } else {
                    xMax = xGridSize * xElemSize;
                }
                if (xCut == 0) {
                    zMax = zCut * zElemSize;
                } else {
                    zMax = zGridSize * zElemSize;
                }
                pnt3[0].y = pnt3[1].y = pnt3[2].y = pnt3[3].y = pnt3[4].y = pnt3[5].y = yGridSize * yElemSize;
                pnt3[0].x = pnt3[1].x = 0;
                pnt3[2].x = pnt3[3].x = xCut * xElemSize;
                pnt3[4].x = pnt3[5].x = xMax;
                pnt3[0].z = pnt3[5].z = 0;
                pnt3[1].z = pnt3[2].z = zMax;
                pnt3[3].z = pnt3[4].z = zCut * zElemSize;
                drawLinePolygon(newCanvas, pnt3, 6);
            }
        }

        private void drawPlainZ(Canvas newCanvas, boolean isCutPlane) {

            int k;

            for (int i = 0; i < yGridSize; i++) {
                pnt3[0].y = pnt3[1].y = i * yElemSize;
                pnt3[2].y = pnt3[3].y = (i + 1) * yElemSize;

                for (int j = 0; j < xGridSize; j++) {
                    if (isCutPlane) {
                        k = zCut;                                               // Плоскость выреза 
                        if (k == 0 || j < xCut || i < yCut) {
                            continue;
                        }
                    } else {                                                    // Плоскость z = Z
                        k = zGridSize;
                        if (j >= xCut && i >= yCut) {
                            continue;
                        }
                    }

                    pnt3[0].z = pnt3[1].z = pnt3[2].z = pnt3[3].z = k * zElemSize;

                    pnt3[0].x = pnt3[3].x = j * xElemSize;
                    pnt3[1].x = pnt3[2].x = (j + 1) * xElemSize;

                    pnt3[0].T = solution[k][i][j];
                    pnt3[1].T = solution[k][i][j + 1];
                    pnt3[2].T = solution[k][i + 1][j + 1];
                    pnt3[3].T = solution[k][i + 1][j];
                    drawPolygon(newCanvas, pnt3);
                }

                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("drawCube(): interrupted = " + (System.currentTimeMillis() - workingStart) + " " + Thread.currentThread());
                    return;
                }
            }

            newCanvas.getGraphicsContext2D().setLineWidth(1);
            if (isGradient && isCutPlane) {
                if (yCut > 0 && zCut > 0) {
                    newCanvas.getGraphicsContext2D().setStroke(Color.WHITE);
                } else {
                    newCanvas.getGraphicsContext2D().setStroke(Color.BLACK);
                }
                pnt3[0].x = xCut * xElemSize;
                pnt3[0].y = pnt3[1].y = yCut * yElemSize;
                pnt3[0].z = pnt3[1].z = zCut * zElemSize;
                pnt3[1].x = xGridSize * xElemSize;
                drawLine(newCanvas, pnt3);

                if (xCut > 0 && zCut > 0) {
                    newCanvas.getGraphicsContext2D().setStroke(Color.WHITE);
                } else {
                    newCanvas.getGraphicsContext2D().setStroke(Color.BLACK);
                }
                pnt3[1].x = xCut * xElemSize;;
                pnt3[1].y = yGridSize * yElemSize;
                drawLine(newCanvas, pnt3);

                if (zCut > 0 && xCut > 0 && yCut > 0) {
                    newCanvas.getGraphicsContext2D().setStroke(Color.WHITE);
                } else {
                    newCanvas.getGraphicsContext2D().setStroke(Color.BLACK);
                }
                pnt3[1].y = yCut * yElemSize;
                pnt3[1].z = zGridSize * zElemSize;
                drawLine(newCanvas, pnt3);
            }

            if (isGradient && !isCutPlane) {
                newCanvas.getGraphicsContext2D().setStroke(Color.BLACK);
                double xMax, yMax;
                if (xCut == 0) {
                    yMax = yCut * yElemSize;
                } else {
                    yMax = yGridSize * yElemSize;
                }

                if (yCut == 0) {
                    xMax = xCut * xElemSize;
                } else {
                    xMax = xGridSize * xElemSize;
                }

                pnt3[0].z = pnt3[1].z = pnt3[2].z = pnt3[3].z = pnt3[4].z = pnt3[5].z = zGridSize * zElemSize;
                pnt3[0].x = pnt3[1].x = 0;
                pnt3[2].x = pnt3[3].x = xCut * xElemSize;
                pnt3[4].x = pnt3[5].x = xMax;
                pnt3[0].y = pnt3[5].y = 0;
                pnt3[1].y = pnt3[2].y = yMax;
                pnt3[3].y = pnt3[4].y = yCut * yElemSize;
                drawLinePolygon(newCanvas, pnt3, 6);
//                } else {
//                    pnt3[0].z = pnt3[1].z = pnt3[2].z = pnt3[3].z = zGridSize * zElemSize;
//                    pnt3[0].x = pnt3[1].x = 0;
//                    pnt3[2].x = pnt3[3].x = xGridSize * xElemSize;
//                    pnt3[0].y = pnt3[3].y = 0;
//                    pnt3[1].y = pnt3[2].y = yCut * yElemSize;
//                    drawLinePolygon(newCanvas, pnt3, 4);
//                }
            }
        }
    }

    public void stop() {
        System.out.println("stop(): " + Thread.currentThread());

        if (isCalculating) {
            isCalculating = false;
            calcService.cancel();
        }
        if (!executorDrawService.isShutdown()) {
            executorDrawService.shutdownNow();
        }
        if (redrawService != null) {
            redrawService.cancel();
            execService.shutdownNow();
        }
    }

    public void drawAllInRedrawService() {
        if (redrawService != null) {
            redrawService.cancel();
            execService.shutdownNow();
        }
        execService = Executors.newFixedThreadPool(3);
        redrawService = new RedrawService();
        redrawService.setExecutor(execService);
        redrawService.start();
//        if (viewSolution3D != null) {
//            viewSolution3D.redrawCanvas(model.getTime(), true);
//        }
    }

    private class RedrawService extends Service {

        @Override
        protected Task createTask() {
            return new Task() {
                @Override
                protected Void call() throws Exception {
                    viewSolution3D.redrawCanvas(model.getTime(), false);
                    return null;
                }

                @Override
                protected void succeeded() {
                    redrawScene();
                }
            };
        }
    }

    private class MyCalcTask<Void> extends Task {

        @Override
        protected Void call() throws Exception {

            Thread.currentThread().setPriority(Thread.NORM_PRIORITY);

            System.out.println("\nNew task call(): calling time = " + (System.currentTimeMillis() - workingStart) + " " + Thread.currentThread());

            while (true) {
                int retValue = model.calculateNextLayer();
                if (retValue != OK) {
                    analyseRetValue(retValue);
                    cancel();
                    timer.cancel();
                }
                if (System.currentTimeMillis() - refreshTime > 30) {
                    refreshTime = System.currentTimeMillis();
                    if (isVisible) {
                        executorDrawService.execute(() -> {
                            viewSolution3D.setSolution(model.getSolution().getPrevSol());
                            viewSolution3D.redrawCanvas(model.getSolution().getTime(), false);
                        });
                    }
                    if (isCancelled()) {
                        break;
                    }
                }
            }
            return null;
        }
    }

    private class MyCalcService extends Service {

        @Override
        protected Task createTask() {
            System.out.println("service createTask(): calling time = " + (System.currentTimeMillis() - workingStart) + " " + Thread.currentThread()
                    + " priority = " + Thread.currentThread().getPriority());

            return new MyCalcTask();
        }
    }

    class MyTimerTask extends TimerTask {

        @Override
        public void run() {
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
            if (!isSceneDrawQueued) {
                System.out.println("SceneDrawQueued: calling time = " + (System.currentTimeMillis() - workingStart) + " " + Thread.currentThread()
                        + " priority = " + Thread.currentThread().getPriority());
                isSceneDrawQueued = true;
                Platform.runLater(() -> {
                    redrawScene();
                    isSceneDrawQueued = false;
                });
            }
        }
    };

    public void setVisible(boolean isVisible) {
        if (isVisible) {
            this.isVisible = true;
            if (isCalculating) {
                timer = new Timer("Timer thread", true);
                timer.scheduleAtFixedRate(new MyTimerTask(), 0, 20);
            }
        } else {
            if (isCalculating) {
                timer.purge();
                timer.cancel();
            }
            this.isVisible = false;
        }
    }

    private void redrawScene() {

        long time2 = System.currentTimeMillis();

        stackPane.getChildren().remove(0);
        synchronized (mainCanvas) {
            stackPane.getChildren().add(0, mainCanvas);
        }

        isSceneDrawn = true;
        timeLabel.setText("Текущее время:  " + String.format("%06.1f", (double) mainCanvas.getUserData()));
        int xInd = viewSolution3D.getxCut();
        int yInd = viewSolution3D.getyCut();
        int zInd = viewSolution3D.getzCut();
        //TValueLabel.setText(String.format("%.3f", Math.rint(viewSolution3D.getSolution()[zInd][yInd][xInd] * 1000) / 1000) + " m = " + mono + " g = " + grd);
        TValueLabel.setText(String.format("%.3f", Math.rint(viewSolution3D.getSolution()[zInd][yInd][xInd] * 1000) / 1000));

        System.out.println("******************************************************redrawScene(): calling time = " + (System.currentTimeMillis() - workingStart) + " "
                + Thread.currentThread() + " time: " + (System.currentTimeMillis() - time2));
    }

    public void drawAxes(Canvas newCanvas, boolean isOnlyZ) // Рисование осей координат
    {
        GraphicsContext gc = newCanvas.getGraphicsContext2D();
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        gc.setLineJoin(StrokeLineJoin.ROUND);
        gc.setFont(new Font(10));

        if (!isOnlyZ) {
            point3[0].x = 0;
            point3[0].y = 0;
            point3[0].z = 0;
            point3[1].x = X;
            point3[1].y = 0;
            point3[1].z = 0;
            drawLine(newCanvas, point3);
            point3[1].x = 0;
            point3[1].y = Y;
            point3[1].z = 0;
            drawLine(newCanvas, point3);
            point3[1].x = 0;
            point3[1].y = 0;
            point3[1].z = Z;
            drawLine(newCanvas, point3);
        } else {
            if (currFi >= 180 && currFi <= 360) {
                point3[0].x = 0;
                point3[0].y = 0;
                point3[0].z = 0;
                point3[1].x = 0;
                point3[1].y = 0;
                point3[1].z = Z;
                drawLine(newCanvas, point3);
            }
        }
        drawLabels(newCanvas, isOnlyZ);
    }

    private void drawLabels(Canvas newCanvas, boolean isOnlyZ) {

        GraphicsContext gc = newCanvas.getGraphicsContext2D();

        double modelX = model.getX();                                           // Значения меток
        double modelY = model.getY();
        double modelZ = model.getZ();
        double width = 0;
        double height = 0;
        String s;
        Text text;

        double fontSize = 10;                                                   // Размер фонта для меток
        Font font = new Font(fontSize);
        gc.setFont(font);
        gc.setLineWidth(0.5);

        if (!isOnlyZ) {
            point3[0].x = X * 1.03;                                             // Подписи к осям
            point3[0].y = 0;
            point3[0].z = 0;
            drawText(newCanvas, "X", point3);

            point3[0].x = 0;
            point3[0].y = Y * 1.03;
            point3[0].z = 0;
            drawText(newCanvas, "Y", point3);

            text = new Text("Z");
            text.setFont(font);
            width = text.getBoundsInLocal().getWidth() / scrAxeSize * Math.sqrt((X * X + Y * Y));

            point3[0].x = -(width / 2) * Math.cos(currFi * Math.PI / 180);
            point3[0].y = -(width / 2) * Math.sin(currFi * Math.PI / 180);
            point3[0].z = Z * 1.02;
            drawText(newCanvas, "Z", point3);

            s = Double.toString(modelX);                                        // Метка к оси x
            text = new Text(s);
            text.setFont(font);
            width = text.getBoundsInLocal().getWidth() / scrAxeSize * X;
            height = text.getBoundsInLocal().getHeight() / scrAxeSize * Z;
            double fiShift = 0;
            if (currFi <= 90 || currFi >= 270) {
                fiShift = width;
            }

//            point3[0].x = modelX + width / s.length();
//            point3[0].y = -width / s.length();
//            point3[0].z = height / 2;
//            point3[1].x = point3[0].x;
//            point3[1].y = point3[0].y - width;
//            point3[1].z = point3[0].z;
//            point3[2].x = point3[0].x;
//            point3[2].y = point3[1].y;
//            point3[2].z = point3[0].z + height / 2;
//            point3[3].x = point3[0].x;
//            point3[3].y = point3[0].y;
//            point3[3].z = point3[0].z + height / 2;
//            drawLinePolygon(newCanvas, point3, 4);

            point3[1].x = modelX;
            point3[1].y = 0;
            point3[1].z = 0;
            point3[0].x = modelX + width / s.length();
            point3[0].y = -height;
            point3[0].z = -height / 2;
            drawLine(newCanvas, point3);
            point3[0].x += fiShift * Math.sin(currFi * Math.PI / 180);
            point3[0].y -= fiShift * Math.cos(currFi * Math.PI / 180);
            point3[0].z = -height;
            drawText(newCanvas, s, point3);

            s = Double.toString(modelY);                                        // Метка к оси y
            text = new Text(s);
            text.setFont(font);
            width = text.getBoundsInLocal().getWidth() / scrAxeSize * Y;
            height = text.getBoundsInLocal().getHeight() / scrAxeSize * Z;
            fiShift = 0;
            if (currFi >= 180) {
                fiShift = width;
            }

            point3[1].x = 0;
            point3[1].y = modelY;
            point3[1].z = 0;
            point3[0].x = -height;
            point3[0].y = modelY + width / s.length();
            point3[0].z = -height / 2;
            drawLine(newCanvas, point3);
            point3[0].x += fiShift * Math.sin(currFi * Math.PI / 180);
            point3[0].y -= fiShift * Math.cos(currFi * Math.PI / 180);
            point3[0].z = -height;
            drawText(newCanvas, s, point3);
        } else {
            if (currFi >= 180 && currFi <= 360) {
                text = new Text("Z");
                text.setFont(font);
                width = text.getBoundsInLocal().getWidth() / scrAxeSize * Math.sqrt((X * X + Y * Y));
                point3[0].x = -(width / 2) * Math.cos(currFi * Math.PI / 180);
                point3[0].y = -(width / 2) * Math.sin(currFi * Math.PI / 180);
                point3[0].z = Z * 1.02;
                drawText(newCanvas, "Z", point3);
            }
        }

        s = Double.toString(modelZ);                                        // Метка к оси z
        text = new Text(s);
        text.setFont(font);
        width = text.getBoundsInLocal().getWidth() / scrAxeSize * Y;
        height = text.getBoundsInLocal().getHeight() / scrAxeSize * Z;
        double alpha;
        double fiShift = 0;
        point3[1].x = 0;
        point3[1].y = 0;
        point3[1].z = modelZ;
        if (currFi < 270) {
            alpha = (currFi + 45) * Math.PI / 180;
            point3[0].x = -height * Math.sin(alpha);
            point3[0].y = height * Math.cos(alpha);
        } else {
            alpha = (currFi - 45) * Math.PI / 180;
            point3[0].x = height * Math.sin(alpha);
            point3[0].y = -height * Math.cos(alpha);
            fiShift = width;
        }
        point3[0].z = modelZ + height / 2;
        drawLine(newCanvas, point3);
        point3[0].x += fiShift * Math.sin(currFi * Math.PI / 180);
        point3[0].y -= fiShift * Math.cos(currFi * Math.PI / 180);

        drawText(newCanvas, s, point3);
    }

    public void drawLine(Canvas newCanvas, Point3DT[] pnt3) // Проецирование и рисование трехмерного отрезка
    {
        GraphicsContext gc = newCanvas.getGraphicsContext2D();

        projection.convertToScreen(pnt3, workX, workY, 2);
        gc.strokeLine(workX[0], workY[0], workX[1], workY[1]);
    }

    private void drawLinePolygon(Canvas newCanvas, Point3DT[] pnt3, int n) {
        double[] massX = new double[n];
        double[] massY = new double[n];

        GraphicsContext gc = newCanvas.getGraphicsContext2D();

        projection.convertToScreen(pnt3, massX, massY, n);
        //gc.setLineWidth(1);
        gc.strokePolygon(massX, massY, n);
    }

    public void drawText(Canvas newCanvas, String s, Point3D[] pnt3) // Проецирование и рисование текстовой строки
    {
        GraphicsContext gc = newCanvas.getGraphicsContext2D();

        projection.convertToScreen(pnt3, workX, workY, 1);
        gc.strokeText(s, workX[0], workY[0]);
        //gc.strokeOval(workX[0] - 2, workY[0] - 2, 5, 5);
    }

    public void drawPolygon_(Canvas newCanvas, Point3DT[] pnt3) // Проецирование и рисование четырехугольника
    {
        Color currFillColor;
        Color currStrokeColor;

        GraphicsContext gc = newCanvas.getGraphicsContext2D();

        if (isGradient && !(pnt3[0].T == pnt3[1].T && pnt3[1].T == pnt3[2].T && pnt3[2].T == pnt3[3].T)) {  // С градиентной заливкой элементов

            grd++;

            Color currFillColorMax;
            Color currFillColorMin;
            double[] gradScrX = new double[2];
            double[] gradScrY = new double[2];

            center.x = (pnt3[0].x + pnt3[1].x + pnt3[2].x + pnt3[3].x) / 4;
            center.y = (pnt3[0].y + pnt3[1].y + pnt3[2].y + pnt3[3].y) / 4;
            center.z = (pnt3[0].z + pnt3[1].z + pnt3[2].z + pnt3[3].z) / 4;
            center.T = (pnt3[0].T + pnt3[1].T + pnt3[2].T + pnt3[3].T) / 4;

            double startGradX, startGradY;
            double endGradX, endGradY;
            double gradX, gradY, gradZ;
            double gradX1, gradY1, gradZ1;
            double gradX2, gradY2, gradZ2;

            if (!(pnt3[0].x == pnt3[1].x && pnt3[1].x == pnt3[2].x && pnt3[2].x == pnt3[3].x)) // Элемент не параллелен плоскости x = const
            {

                if (pnt3[1].x != pnt3[0].x) {
                    gradX1 = (pnt3[1].T - pnt3[0].T) / (pnt3[1].x - pnt3[0].x);
                    gradX2 = (pnt3[3].T - pnt3[2].T) / (pnt3[3].x - pnt3[2].x);
                } else {
                    gradX1 = (pnt3[2].T - pnt3[1].T) / (pnt3[2].x - pnt3[1].x);
                    gradX2 = (pnt3[0].T - pnt3[3].T) / (pnt3[0].x - pnt3[3].x);
                }
                gradX = (gradX1 + gradX2) / 2;
            } else {
                gradX = 0;
            }

            if (!(pnt3[0].y == pnt3[1].y && pnt3[1].y == pnt3[2].y && pnt3[2].y == pnt3[3].y)) // Элемент не параллелен плоскости y = const
            {
                if (pnt3[1].y != pnt3[0].y) {
                    gradY1 = (pnt3[1].T - pnt3[0].T) / (pnt3[1].y - pnt3[0].y);
                    gradY2 = (pnt3[3].T - pnt3[2].T) / (pnt3[3].y - pnt3[2].y);

                } else {
                    gradY1 = (pnt3[2].T - pnt3[1].T) / (pnt3[2].y - pnt3[1].y);
                    gradY2 = (pnt3[0].T - pnt3[3].T) / (pnt3[0].y - pnt3[3].y);
                }
                gradY = (gradY1 + gradY2) / 2;
            } else {
                gradY = 0;
            }

            if (!(pnt3[0].z == pnt3[1].z && pnt3[1].z == pnt3[2].z && pnt3[2].z == pnt3[3].z)) // Элемент не параллелен плоскости z = const
            {
                if (pnt3[1].z != pnt3[0].z) {
                    gradZ1 = (pnt3[1].T - pnt3[0].T) / (pnt3[1].z - pnt3[0].z);
                    gradZ2 = (pnt3[3].T - pnt3[2].T) / (pnt3[3].z - pnt3[2].z);
                } else {
                    gradZ1 = (pnt3[2].T - pnt3[1].T) / (pnt3[2].z - pnt3[1].z);
                    gradZ2 = (pnt3[0].T - pnt3[3].T) / (pnt3[0].z - pnt3[3].z);
                }
                gradZ = (gradZ1 + gradZ2) / 2;
            } else {
                gradZ = 0;
            }

            double perpGradX = 0;
            double perpGradY = 0;
            double perpGradZ = 0;
            if (pnt3[0].z == pnt3[1].z && pnt3[1].z == pnt3[2].z && pnt3[2].z == pnt3[3].z) // Находим перпендикуляр к градиенту в трех случаях
            {
                perpGradX = -gradY;
                perpGradY = gradX;
                perpGradZ = 0;
            }
            if (pnt3[0].x == pnt3[1].x && pnt3[1].x == pnt3[2].x && pnt3[2].x == pnt3[3].x) {
                perpGradX = 0;
                perpGradY = -gradZ;
                perpGradZ = gradY;
            }
            if (pnt3[0].y == pnt3[1].y && pnt3[1].y == pnt3[2].y && pnt3[2].y == pnt3[3].y) {
                perpGradX = gradZ;
                perpGradY = 0;
                perpGradZ = -gradX;
            }

            projection.convertToScreen(pnt3, workX, workY, 4);
            double centerX = (workX[0] + workX[1] + workX[2] + workX[3]) / 4;
            double centerY = (workY[0] + workY[1] + workY[2] + workY[3]) / 4;

            double[] perpX = new double[2];
            double[] perpY = new double[2];
            Point3DT[] perp = {new Point3DT(), new Point3DT(perpGradX, perpGradY, perpGradZ, 0)};
            projection.convertToScreen(perp, perpX, perpY, 2);
            double dGradScrX = -(perpY[1] - perpY[0]);
            double dGradScrY = perpX[1] - perpX[0];

            double tmpGradX = dGradScrX;
            double tmpGradY = dGradScrY;

            double dGradScrXY = Math.sqrt(dGradScrX * dGradScrX + dGradScrY * dGradScrY);
            dGradScrX /= dGradScrXY;
            dGradScrY /= dGradScrXY;

            double maxT = pnt3[0].T;
            double minT = pnt3[0].T;
            double maxScrX = workX[0];
            double minScrX = workX[0];
            double maxScrY = workY[0];
            double minScrY = workY[0];
            for (int i = 1; i < 4; i++) {
                maxT = Math.max(maxT, pnt3[i].T);
                minT = Math.min(minT, pnt3[i].T);
                maxScrX = Math.max(maxScrX, workX[i]);
                minScrX = Math.min(minScrX, workX[i]);
                maxScrY = Math.max(maxScrY, workY[i]);
                minScrY = Math.min(minScrY, workY[i]);
            }
            double lenX = maxScrX - minScrX;
            double lenY = maxScrY - minScrY;

            int indTop = 0;
            int indLeft = 0;
            double xx, yy;
            if (Math.abs(dGradScrY) >= Math.abs(dGradScrX)) {
                for (int i = 0; i < 4; i++) {
                    if (workY[i] == minScrY) {
                        indTop = i;
                        break;
                    }
                }
                if (dGradScrY >= 0) {
                    startGradY = 0;
                    startGradX = (workX[indTop] - minScrX) / lenX;
                    xx = workX[indTop];
                    yy = workY[indTop];
                } else {
                    startGradY = 1;
                    startGradX = (workX[(indTop + 2) % 4] - minScrX) / lenX;
                    xx = workX[(indTop + 2) % 4];
                    yy = workY[(indTop + 2) % 4];
                }
                double koef = Math.abs(1 / dGradScrY);
                dGradScrY *= koef;
                dGradScrX *= koef;
            } else {
                for (int i = 0; i < 4; i++) {
                    if (workX[i] == minScrX) {
                        indLeft = i;
                        break;
                    }
                }
                if (dGradScrX >= 0) {
                    startGradX = 0;
                    startGradY = (workY[indLeft] - minScrY) / lenY;
                    xx = workX[indLeft];
                    yy = workY[indLeft];
                } else {
                    startGradX = 1;
                    startGradY = (workY[(indLeft + 2) % 4] - minScrY) / lenY;
                    xx = workX[(indLeft + 2) % 4];
                    yy = workY[(indLeft + 2) % 4];
                }
                double koef = Math.abs(1 / dGradScrX);
                dGradScrY *= koef;
                dGradScrX *= koef;
            }

            int colorIndexMax = (int) ((maxT - Tmin) / (Tmax - Tmin) * (colorNumber - 1));
            if (colorIndexMax >= colorNumber) {
                currFillColorMax = Color.WHITESMOKE;
            } else if (colorIndexMax < 0) {
                currFillColorMax = Color.BLACK;
            } else {
                currFillColorMax = colors.get(colorIndexMax);
            }

            int colorIndexMin = (int) ((minT - Tmin) / (Tmax - Tmin) * (colorNumber - 1));
            if (colorIndexMin >= colorNumber) {
                currFillColorMin = Color.WHITESMOKE;
            } else if (colorIndexMin < 0) {
                currFillColorMin = Color.BLACK;
            } else {
                currFillColorMin = colors.get(colorIndexMin);
            }

            double Tmean = (pnt3[0].T + pnt3[1].T + pnt3[2].T + pnt3[3].T) / 4;
            int colorIndex = (int) ((Tmean - Tmin) / (Tmax - Tmin) * (colorNumber - 1));

            if (colorIndex >= colorNumber) {
                currStrokeColor = Color.GRAY;
            } else if (colorIndex < 0) {
                currStrokeColor = Color.GRAY;
            } else {
                currStrokeColor = colors.get(colorIndex);
            }

            Stop[] stops = new Stop[]{new Stop(0, currFillColorMin), new Stop(1, currFillColorMax)};
            LinearGradient lg = new LinearGradient(startGradX, startGradY,
                    startGradX + dGradScrX, startGradY + dGradScrY,
                    true, CycleMethod.NO_CYCLE, stops);

            gc.setFill(lg);
            gc.setStroke(currStrokeColor);
            //gc.setLineWidth(1);
            gc.fillPolygon(workX, workY, 4);
            gc.strokePolygon(workX, workY, 4);
            //gc.setStroke(Color.GRAY);

            gc.setStroke(Color.BLACK);
            //gc.strokeLine(centerX, centerY, centerX + dGradScrX * 20, centerY + dGradScrY * 20);
            gc.setStroke(Color.GREEN);
            //gc.strokeLine(centerX, centerY, centerX + tmpGradX, centerY + tmpGradY);
            gc.setFill(Color.WHITE);
            //gc.fillOval(xx - 5, yy - 5, 10, 10);

        } else {                        // Без градиентной заливки элементов - заливка элемента цветом средней температуры
            mono++;

            projection.convertToScreen(pnt3, workX, workY, 4);
            double Tmean = (pnt3[0].T + pnt3[1].T + pnt3[2].T + pnt3[3].T) / 4;
            int colorIndex = (int) ((Tmean - Tmin) / (Tmax - Tmin) * (colorNumber - 1));
            if (colorIndex >= colorNumber) {
                currFillColor = Color.WHITESMOKE;
                currStrokeColor = Color.GRAY;
            } else if (colorIndex < 0) {
                currFillColor = Color.BLACK;
                currStrokeColor = Color.GRAY;
            } else {
                currFillColor = colors.get((colorIndex));
                currStrokeColor = Color.WHITE;
            }

            gc.setFill(currFillColor);
            gc.setStroke(currStrokeColor);
            //gc.setLineWidth(0.5);
            gc.fillPolygon(workX, workY, 4);
            gc.strokePolygon(workX, workY, 4);
        }
    }

    public void drawPolygon(Canvas newCanvas, Point3DT[] pnt3) // Проецирование и рисование четырехугольника
    {
        Color currFillColor;
        Color currStrokeColor;

        GraphicsContext gc = newCanvas.getGraphicsContext2D();

        if (isGradient && !(pnt3[0].T == pnt3[1].T && pnt3[1].T == pnt3[2].T && pnt3[2].T == pnt3[3].T)) {  // С градиентной заливкой элементов

            grd++;

            Color currFillColorMax;
            Color currFillColorMin;
            projection.convertToScreen(pnt3, workX, workY, 4);                  // Проецирование точек элемента на систему координат экрана

            double[] gradScrX = new double[2];                                  // Массив для проекции градиента
            double[] gradScrY = new double[2];

            double startGradX, startGradY;                                      // Координаты начала и конца градиента в экранных координатах
            double endGradX, endGradY;

            double gradX, gradY, gradZ;                                         // Координаты проекций градиента на плоскости куба в системе координат модели
            double gradX1, gradY1, gradZ1;
            double gradX2, gradY2, gradZ2;

            if (!(pnt3[0].x == pnt3[1].x && pnt3[1].x == pnt3[2].x && pnt3[2].x == pnt3[3].x)) // Элемент не параллелен плоскости x = const
            {

                if (pnt3[1].x != pnt3[0].x) {
                    gradX1 = (pnt3[1].T - pnt3[0].T) / (pnt3[1].x - pnt3[0].x);
                    gradX2 = (pnt3[3].T - pnt3[2].T) / (pnt3[3].x - pnt3[2].x);
                } else {
                    gradX1 = (pnt3[2].T - pnt3[1].T) / (pnt3[2].x - pnt3[1].x);
                    gradX2 = (pnt3[0].T - pnt3[3].T) / (pnt3[0].x - pnt3[3].x);
                }
                gradX = (gradX1 + gradX2) / 2;
            } else {
                gradX = 0;
            }

            if (!(pnt3[0].y == pnt3[1].y && pnt3[1].y == pnt3[2].y && pnt3[2].y == pnt3[3].y)) // Элемент не параллелен плоскости y = const
            {
                if (pnt3[1].y != pnt3[0].y) {
                    gradY1 = (pnt3[1].T - pnt3[0].T) / (pnt3[1].y - pnt3[0].y);
                    gradY2 = (pnt3[3].T - pnt3[2].T) / (pnt3[3].y - pnt3[2].y);

                } else {
                    gradY1 = (pnt3[2].T - pnt3[1].T) / (pnt3[2].y - pnt3[1].y);
                    gradY2 = (pnt3[0].T - pnt3[3].T) / (pnt3[0].y - pnt3[3].y);
                }
                gradY = (gradY1 + gradY2) / 2;
            } else {
                gradY = 0;
            }

            if (!(pnt3[0].z == pnt3[1].z && pnt3[1].z == pnt3[2].z && pnt3[2].z == pnt3[3].z)) // Элемент не параллелен плоскости z = const
            {
                if (pnt3[1].z != pnt3[0].z) {
                    gradZ1 = (pnt3[1].T - pnt3[0].T) / (pnt3[1].z - pnt3[0].z);
                    gradZ2 = (pnt3[3].T - pnt3[2].T) / (pnt3[3].z - pnt3[2].z);
                } else {
                    gradZ1 = (pnt3[2].T - pnt3[1].T) / (pnt3[2].z - pnt3[1].z);
                    gradZ2 = (pnt3[0].T - pnt3[3].T) / (pnt3[0].z - pnt3[3].z);
                }
                gradZ = (gradZ1 + gradZ2) / 2;
            } else {
                gradZ = 0;
            }

//            Point3DT[] grad = {new Point3DT(), new Point3DT(gradX, gradY, gradZ, 0)};
//            projection.convertToScreen(grad, gradScrX, gradScrY, 2);                                // Проецирование градиента на систему координат экрана
//            double gradientX = gradScrX[1] - gradScrX[0];
//            double gradientY = gradScrY[1] - gradScrY[0];
//            double gradientXY = Math.sqrt(gradientX * gradientX + gradientY * gradientY);
//            gradientX /= gradientXY;
//            gradientY /= gradientXY;
            double perpGradX = 0;                                                                   // Перпендикуляр к градиенту в координатах модели
            double perpGradY = 0;
            double perpGradZ = 0;
            if (pnt3[0].z == pnt3[1].z && pnt3[1].z == pnt3[2].z && pnt3[2].z == pnt3[3].z) // Находим перпендикуляр к градиенту в трех случаях
            {
                perpGradX = -gradY;
                perpGradY = gradX;
                perpGradZ = 0;
            }
            if (pnt3[0].x == pnt3[1].x && pnt3[1].x == pnt3[2].x && pnt3[2].x == pnt3[3].x) {
                perpGradX = 0;
                perpGradY = -gradZ;
                perpGradZ = gradY;
            }
            if (pnt3[0].y == pnt3[1].y && pnt3[1].y == pnt3[2].y && pnt3[2].y == pnt3[3].y) {
                perpGradX = gradZ;
                perpGradY = 0;
                perpGradZ = -gradX;
            }

            double[] perpX = new double[2];
            double[] perpY = new double[2];
            Point3DT[] perp = {new Point3DT(), new Point3DT(perpGradX, perpGradY, perpGradZ, 0)};
            projection.convertToScreen(perp, perpX, perpY, 2);                                      // Проецирование перпендикуляра на систему координат экрана
            double dPerpX = perpX[1] - perpX[0];
            double dPerpY = perpY[1] - perpY[0];
            double dPerp = Math.sqrt(dPerpX * dPerpX + dPerpY * dPerpY);
            dPerpX /= dPerp;
            dPerpY /= dPerp;

            double dGradScrX = -dPerpY;
            double dGradScrY = dPerpX;

            double maxT = pnt3[0].T;
            double minT = pnt3[0].T;

            int indMin = 0;
            int indMax = 0;
            for (int i = 1; i < 4; i++) {
                if (maxT < pnt3[i].T) {
                    maxT = pnt3[i].T;
                    indMax = i;
                }
                if (minT > pnt3[i].T) {
                    minT = pnt3[i].T;
                    indMin = i;
                }
            }
            startGradX = workX[indMin];
            startGradY = workY[indMin];

            endGradX = ((workX[indMin] * dGradScrY - workY[indMin] * dGradScrX) * dPerpX
                    + (workY[indMax] * dPerpX - workX[indMax] * dPerpY) * dGradScrX) / (dGradScrY * dPerpX - dGradScrX * dPerpY);
            endGradY = ((workY[indMin] * dGradScrX - workX[indMin] * dGradScrY) * dPerpY
                    + (workX[indMax] * dPerpY - workY[indMax] * dPerpX) * dGradScrY) / (dGradScrX * dPerpY - dGradScrY * dPerpX);

            int colorIndexMax = (int) ((maxT - Tmin) / (Tmax - Tmin) * (colorNumber - 1));
            if (colorIndexMax >= colorNumber) {
                currFillColorMax = Color.WHITESMOKE;
            } else if (colorIndexMax < 0) {
                currFillColorMax = Color.BLACK;
            } else {
                currFillColorMax = colors.get(colorIndexMax);
            }

            int colorIndexMin = (int) ((minT - Tmin) / (Tmax - Tmin) * (colorNumber - 1));
            if (colorIndexMin >= colorNumber) {
                currFillColorMin = Color.WHITESMOKE;
            } else if (colorIndexMin < 0) {
                currFillColorMin = Color.BLACK;
            } else {
                currFillColorMin = colors.get(colorIndexMin);
            }

            double Tmean = (pnt3[0].T + pnt3[1].T + pnt3[2].T + pnt3[3].T) / 4;
            int colorIndex = (int) ((Tmean - Tmin) / (Tmax - Tmin) * (colorNumber - 1));

            if (colorIndex >= colorNumber) {
                currStrokeColor = Color.GRAY;
            } else if (colorIndex < 0) {
                currStrokeColor = Color.GRAY;
            } else {
                currStrokeColor = colors.get(colorIndex);
            }

            Stop[] stops = new Stop[]{new Stop(0, currFillColorMin), new Stop(1, currFillColorMax)};
            LinearGradient lg = new LinearGradient(startGradX, startGradY,
                    endGradX, endGradY, false, CycleMethod.NO_CYCLE, stops);

            gc.setFill(lg);
            gc.setStroke(currStrokeColor);
            //gc.setLineWidth(1);
            gc.fillPolygon(workX, workY, 4);
            gc.strokePolygon(workX, workY, 4);
            //gc.setStroke(Color.GRAY);

        } else {                        // Без градиентной заливки элементов - заливка элемента цветом средней температуры
            mono++;

            projection.convertToScreen(pnt3, workX, workY, 4);
            double Tmean = (pnt3[0].T + pnt3[1].T + pnt3[2].T + pnt3[3].T) / 4;
            int colorIndex = (int) ((Tmean - Tmin) / (Tmax - Tmin) * (colorNumber - 1));
            if (colorIndex >= colorNumber) {
                currFillColor = Color.WHITESMOKE;
                currStrokeColor = Color.GRAY;
            } else if (colorIndex < 0) {
                currFillColor = Color.BLACK;
                currStrokeColor = Color.GRAY;
            } else {
                currFillColor = colors.get((colorIndex));
                currStrokeColor = Color.WHITE;
            }

            gc.setFill(currFillColor);
            if (isGradient) {
                gc.setStroke(currFillColor);
            } else {
                gc.setStroke(currStrokeColor);
            }
            gc.setLineWidth(0.5);
            gc.fillPolygon(workX, workY, 4);
            gc.strokePolygon(workX, workY, 4);
        }
    }

    public void drawTriangle(GraphicsContext gc, Point3DT[] triang) {

//        Color currFillColorMax;
//        Color currFillColorMin;
//        Color currStrokeColor;
//
//        double startGradX, startGradY;
//        double endGradX, endGradY;
//
//        if (triang[0].T == triang[1].T && triang[1].T == triang[2].T) // Нарисовать треугольник, заполненный цветом без градиента и выйти
//        {
//            mono++;
//
//            projection.convertToScreen(triang, workX, workY, 3);
//            int colorIndex = (int) ((triang[0].T - Tmin) / (Tmax - Tmin) * (colorNumber - 1));
//
//            if (colorIndex >= colorNumber) {
//                currFillColorMax = Color.WHITESMOKE;
//                currStrokeColor = Color.GRAY;
//                gc.setLineWidth(.3);
//            } else if (colorIndex < 0) {
//                currFillColorMax = Color.BLACK;
//                currStrokeColor = Color.GRAY;
//                gc.setLineWidth(.3);
//            } else {
//                currFillColorMax = colors.get((int) ((triang[0].T - Tmin) / (Tmax - Tmin) * (colorNumber - 1)));
//                currStrokeColor = currFillColorMax;
//                gc.setLineWidth(1);
//            }
//            gc.setFill(currFillColorMax);
//            gc.setStroke(currStrokeColor);
//
//            gc.fillPolygon(workX, workY, 3);
//            gc.strokePolygon(workX, workY, 3);
//        }
//
//        grd++;
//
//        Arrays.sort(triang);
//        projection.convertToScreen(triang, workX, workY, 3);
//
//        if (Math.abs(triang[0].T - triang[1].T) < 1e-12) {
//            startGradX = (workX[0] + workX[1]) / 2;
//            startGradY = (workY[0] + workY[1]) / 2;
//        } else {
//            startGradX = workX[0];
//            startGradY = workY[0];
//        }
//        if (Math.abs(triang[1].T - triang[2].T) < 1e-12) {
//            endGradX = (workX[1] + workX[2]) / 2;
//            endGradY = (workY[1] + workY[2]) / 2;
//        } else {
//            endGradX = workX[2];
//            endGradY = workY[2];
//        }
//
//        double max = triang[2].T;
//        double min = triang[0].T;
//
//        int colorIndexMax = (int) ((max - Tmin) / (Tmax - Tmin) * (colorNumber - 1));
//        if (colorIndexMax >= colorNumber) {
//            currFillColorMax = Color.WHITESMOKE;
//        } else if (colorIndexMax < 0) {
//            currFillColorMax = Color.BLACK;
//        } else {
//            currFillColorMax = colors.get((int) ((max - Tmin) / (Tmax - Tmin) * (colorNumber - 1)));
//        }
//
//        int colorIndexMin = (int) ((min - Tmin) / (Tmax - Tmin) * (colorNumber - 1));
//        if (colorIndexMin >= colorNumber) {
//            currFillColorMin = Color.WHITESMOKE;
//        } else if (colorIndexMin < 0) {
//            currFillColorMin = Color.BLACK;
//        } else {
//            currFillColorMin = colors.get((int) ((min - Tmin) / (Tmax - Tmin) * (colorNumber - 1)));
//        }
//
//        double Tmean = (triang[0].T + triang[1].T + triang[2].T) / 3;
//        int colorIndex = (int) ((Tmean - Tmin) / (Tmax - Tmin) * (colorNumber - 1));
//
//        if (colorIndex >= colorNumber) {
//            currStrokeColor = Color.GRAY;
//            gc.setLineWidth(.3);
//        } else if (colorIndex < 0) {
//            currStrokeColor = Color.GRAY;
//            gc.setLineWidth(.3);
//        } else {
//            currStrokeColor = colors.get((int) ((Tmean - Tmin) / (Tmax - Tmin) * (colorNumber - 1)));
//            gc.setLineWidth(1);
//        }
//
//        //Stop[] stops = new Stop[]{new Stop(0, currFillColorMin), new Stop(1, currFillColorMax)};
//        Stop[] stops = new Stop[]{new Stop(0, currFillColorMin), new Stop(1, currFillColorMax)};
//        LinearGradient lg = new LinearGradient(startGradX, startGradY, endGradX, endGradY, false, CycleMethod.NO_CYCLE, stops);
//
////        double x = endGradX - startGradX;
////        double y = endGradY - startGradY;
////        double xy = Math.sqrt(x * x + y * y);
////        LinearGradient lg = new LinearGradient(0, 0, x / xy, y / xy, true, CycleMethod.NO_CYCLE, stops);
//        gc.setFill(lg);
//        gc.setStroke(currStrokeColor);
//
//        gc.fillPolygon(workX, workY, 3);
//        gc.strokePolygon(workX, workY, 3);
//
//        gc.fill();
//        gc.stroke();
//
////        gc.setStroke(Color.WHITE);
////        gc.strokeLine(startGradX, startGradY, endGradX, endGradY);
////gc.setStroke(Color.WHITE);
    }

    @Override
    public GridPane getGrid() {
        return grid;
    }

    @Override
    public StackPane getStackpane() {
        return stackPane;
    }

    @Override
    public void setCanvasSize(double x, double y) {
        scrAxeSize = y / 2;
        projection.setCanvasParams(scrAxeSize, x / 2, y / 2);

//        if (canvasY != y) {
//            viewSolution3D = new ViewSolution3D(model.getSolution().getPrevSol());   // Новый класс решения с новыми размерами
//        }
        canvasX = x;
        canvasY = y;

        drawAllInRedrawService();
    }

    // Коды возврата
    final static int OK = 0;                        // Ok
    final static int NO_SATISFIED_ACCURACY = 1;     // Требуемая точность не достигнута, хотя итер. процесс не расходится
    final static int MEMORY_LACK = 2;               // Нехватка памяти при попытке захватить память для служебных массивов
    final static int INVALID_ACTUAL_PARAM = 3;      // Передача неправильного параметра
    final static int NULL_POINTER_TRANSMISSION = 4; // В функцию передан нулевой указатель
    final static int TOO_LARGE_INDEX = 5;           // Проверка выявила элемент, имеющий слишком большой столбцовый индекс
    final static int TWO_EQUAL_ELEM_INDEXES = 6;    // Проверка выявила элементы одной строки, имеющие один и тот же столбцовый индекс
    final static int ITER_PROCESS_DIVERGENCE = 7;   // Итерационный процесс расходится
    final static int BAD_MATRIX = 8;                // Матрица плохо обусловлена
    final static int TOO_FEW_ROOM_cn_MAS = 9;       // Не хватает места в строчно-упорядоченном массиве, т.е. переданные функции массивы a и cn малы
    final static int TOO_FEW_ROOM_ln_MAS = 10;      // Не хватает места в столбцово-упорядоченном массиве, т.е. переданный функции массив rn мал

    private void analyseRetValue(int retValue) // Анализ возвращаемого значения
    {
        switch (retValue) {
            case OK:
                System.out.println("Ok");
                break;
            case NO_SATISFIED_ACCURACY:
                System.out.println("failed\nOшибка: требуемая точность не достигнута");
                break;
            case MEMORY_LACK:
                System.out.println("failed\nOшибка: недостаточно свободной памяти");
                break;
            case INVALID_ACTUAL_PARAM:
                System.out.println("failed\nOшибка в переданных параметрах");
                break;
            case NULL_POINTER_TRANSMISSION:
                System.out.println("failed\nOшибка: функции передан нулевой указатель");
                break;
            case TOO_LARGE_INDEX:
                System.out.println("failed\nОшибка в передаваемых данных: индекс элемента матрицы больше ее размера");
                break;
            case TWO_EQUAL_ELEM_INDEXES:
                System.out.println("failed\nОшибка в передаваемых данных: два разных элемента матрицы");
                break;
            case ITER_PROCESS_DIVERGENCE:
                System.out.println("failed\nОшибка: итерационный процесс расходится");
                break;
            case BAD_MATRIX:
                System.out.println("failed\nОшибка: матрица плохо обусловлена");
                break;
            case TOO_FEW_ROOM_cn_MAS:
                System.out.println("failed\nОшибка: не хватает места в строчно-упорядоченном массиве");
                break;
            case TOO_FEW_ROOM_ln_MAS:
                System.out.println("failed\nОшибка: не хватает места в столбцово-упорядоченном массиве");
                break;
        }
    }
}
