package advancedmedia;

import java.util.ArrayList;
import java.util.Arrays;
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

public class Controller2D implements ControllerInterface {

    private double canvasX, canvasY;                // Размеры канваса - вычисляются динамически в зависимости от текущего размера окна
    private double scrAxeSize;                      // Длины осей в экранных единицах

    private Heat2D model;                           // Экземпляр класса модели - Heat2D
    private CurrentProjection projection;           // Матрица проецирования физических координат на координаты экрана
    private ViewSolution viewSolution;              // Класс, содержащий интерполированное решение (создается из настоящего решения модели)

    // Элементы представления и контроллера
    private GridPane grid;                          // Контейнер для элементов управления представлением
    private StackPane stackPane;                    // Контенер для вывода представления и контрола управления цветом
    private Canvas mainCanvas = new Canvas();       // Канвас для отрисовки представления
    private Label timeLabel;                        // Текущее время модели
    private RadioButton colorRadio;                 // Контролы для переключения между режимами цветной/черно-белый
    private RadioButton gradRadio;
    private RadioButton monoRadio;
    private ToggleGroup colorGroup;
    private Button btnStart;                        // Кнопка Start/Pause

    private double currFi = 45;                     // Начальное значение углов проецирования
    private double teta0 = 30;

    final private double X = 1;                     // Максимальные значения по осям в представлении (могут отличаться от значения модели - тогда модель будет отмасштабирована)
    final private double Y = 1;
    final private double Zmax = 3.;
    final private double Zmin = -.5;

    // Контролы для управления масштабом цвета
    private boolean isColored = true;               // Флаг для преключения режима рисования цветной/черно-белый
    private boolean isGradient = true;              // Флаг для преключения режима рисования с градиентной заливкой элементов
    final private double COLOR_HEIGHT = 150;        // Высота градиентной шкалы в контроле управления цветным масштабом
    final private int colorNumber = 300;            // Количество оттенков для рисования элементов между Tmin и Tmax
    private ArrayList<Color> colors;                // Список оттенков от синего до красного
    private VBox colorsPane;                        // Контейнер для всех элементов контрола
    private TextField highTemp, lowTemp;            // Поля для ввода значений температуры
    private CheckBox tempCheckBox;                  // Переключение между режимами auto/manual

    private double Tmax = Zmax;                     // Максимум и минимум функции T на текущем временном слое
    private double Tmin = Zmin;

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

    Point3D[] point3 = new Point3D[4];              // Рабочие массивы
    //Point3D[] triang = new Point3D[3];              // Треугольник для работы с градиентами
    //Point3D center = new Point3D();                 // Центр заливаемого элемента
    double[] workX = new double[4];
    double[] workY = new double[4];

    public Controller2D() {

        System.out.println("Controller2D(): " + Thread.currentThread());

        for (int i = 0; i < 4; i++) {                       // Рабочий массив из 3D-точек для отрисовки в разных методах
            point3[i] = new Point3D();
        }

        colors = new ArrayList<>();                         // Шкала оттенков для отрисовки цветной поверхности
        for (int i = 0; i <= colorNumber; i++) {
            colors.add(new Color(i * 1. / colorNumber, 0., 1. - i * 1. / colorNumber, 1.));
        }

        model = new Heat2D();                               // Инициализация модели (запрос массивов, заполнение их данными модели, факторизация матрицы)

        makeColorControl();                                 // Создание контрола управления масштабом температуры

        makeGridControl();                                  // Создание панели управления представлением

        makeViewPane();                                     // Создание панели вывода представления

        makeProjection();                                   // Инициализация матрицы проецирования на экран

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
            Color color = colors.get(colorNumber - i);
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
        grid.setMargin(timeLabel, new Insets(0, 30, 0, 20));

        grid.add(timeCheck, 1, 1);
        grid.setMargin(timeCheck, new Insets(0, 30, 0, 20));

        grid.add(labelFi, 2, 0);
        grid.add(sliderFi, 3, 0);
        //grid.setFillWidth(sliderFi, true);
        grid.add(labelTeta, 2, 1);
        grid.add(sliderTeta, 3, 1);
        //grid.setFillWidth(sliderTeta, true);

        colorGroup = new ToggleGroup();
        colorRadio = new RadioButton("В цвете");
        if (isColored) {
            colorRadio.setSelected(true);
        }
        colorRadio.setToggleGroup(colorGroup);
        gradRadio = new RadioButton("В градиентах");
        gradRadio.setToggleGroup(colorGroup);
        if (isGradient) {
            gradRadio.setSelected(true);
        }
        gradRadio.setMinHeight(30);
        monoRadio = new RadioButton("Моно");
        monoRadio.setToggleGroup(colorGroup);
        if (!isColored) {
            monoRadio.setSelected(true);
        }

        colorGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (colorGroup.getSelectedToggle() != null) {
                RadioButton button = (RadioButton) colorGroup.getSelectedToggle();
                if (button.equals(colorRadio)) {
                    isColored = true;
                    if (isGradient) {
                        isGradient = false;
                        synchronized (model.getSolution().prevSol) {
                            viewSolution = new ViewSolution(model.getSolution().prevSol);   // Новый класс интерполированного решения с новыми размерами
                        }
                    }
                    if (stackPane.getChildren().size() == 1) {
                        stackPane.getChildren().add(colorsPane);
                    }
                } else {
                    if (button.equals(gradRadio)) {
                        isColored = true;
                        if (!isGradient) {
                            isGradient = true;
                            synchronized (model.getSolution().prevSol) {
                                viewSolution = new ViewSolution(model.getSolution().prevSol);   // Новый класс интерполированного решения с новыми размерами
                            }
                        }
                        if (stackPane.getChildren().size() == 1) {
                            stackPane.getChildren().add(colorsPane);
                        }
                    } else {
                        isColored = false;
                        if (isGradient) {
                            isGradient = false;
                            synchronized (model.getSolution().prevSol) {
                                viewSolution = new ViewSolution(model.getSolution().prevSol);   // Новый класс интерполированного решения с новыми размерами
                            }
                        }
                        if (stackPane.getChildren().size() == 2) {
                            stackPane.getChildren().remove(1);
                        }
                    }
                }
                drawAllInRedrawService();
            }
        });

//        colorRadio.setOnAction((event) -> {
//            if (colorRadio.isSelected()) {
//                isColored = true;
//                if (isGradient) {
//                    isGradient = false;
//                    synchronized (model.getSolution().prevSol) {
//                        viewSolution = new ViewSolution(model.getSolution().prevSol);   // Новый класс интерполированного решения с новыми размерами
//                    }
//                }
//                if (stackPane.getChildren().size() == 1) {
//                    stackPane.getChildren().add(colorsPane);
//                }
//            }
//            drawAllInRedrawService();
//        });
//        gradRadio.setOnAction((event) -> {
//            if (gradRadio.isSelected()) {
//                isColored = true;
//                if (!isGradient) {
//                    isGradient = true;
//                    synchronized (model.getSolution().prevSol) {
//                        viewSolution = new ViewSolution(model.getSolution().prevSol);   // Новый класс интерполированного решения с новыми размерами
//                    }
//                }
//                if (stackPane.getChildren().size() == 1) {
//                    stackPane.getChildren().add(colorsPane);
//                }
//            }
//            drawAllInRedrawService();
//        });
//        monoRadio.setOnAction((event) -> {
//            if (monoRadio.isSelected()) {
//                isColored = false;
//                if (isGradient) {
//                    isGradient = false;
//                    synchronized (model.getSolution().prevSol) {
//                        viewSolution = new ViewSolution(model.getSolution().prevSol);   // Новый класс интерполированного решения с новыми размерами
//                    }
//                }
//                if (stackPane.getChildren().size() == 2) {
//                    stackPane.getChildren().remove(1);
//                }
//            }
//            drawAllInRedrawService();
//        });
        VBox colorVbox = new VBox(colorRadio, gradRadio, monoRadio);
        grid.add(colorVbox, 4, 0);
        grid.setRowSpan(colorVbox, 2);
        grid.setMargin(colorVbox, new Insets(0, 20, 0, 20));
        colorVbox.setAlignment(Pos.CENTER_LEFT);

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

        grid.add(btnStart, 5, 0);
        grid.setRowSpan(btnStart, 2);
        //grid.setMargin(btnStart, new Insets(0, 10, 0, 0));
        //grid.setGridLinesVisible(true);
    }

    public void makeViewPane() {    // Создание панели вывода представления

        stackPane = new StackPane();        // Панель вывода поверхности и контрола управления масштабом температуры        
        stackPane.alignmentProperty().set(Pos.TOP_LEFT);

        if (isColored) {    // Добавляем нижним элементом пока пустой канвас, в дальнейшем он будет заменяться отрисованным на каждом временном слое
            stackPane.getChildren().addAll(new Canvas(), colorsPane);
        } else {
            stackPane.getChildren().add(new Canvas());
        }
    }

    public void makeProjection() {  // Инициализация матрицы проецирования на экран - вызывается в конструкторе, но перед началом работы должны быть установлены 
        projection = new CurrentProjection(currFi, teta0, // размеры методом setCanvasParams(double scrAxeSize, double xMid, double yMid)
                X, Y, Zmax - Zmin);
    }

    // Структуры и параметры для отрисовки решений
    private class ViewSolution {                        // Класс для представления интерполированного решения и для манипуляций с ним

        private boolean isResized = false;              // Флаг, применять интерполяцию или нет
        private double xMinElemSize = 8;                // Минимально разрешенный размер элемента сетки при рисовании представления (чтобы не рисовать лишнего при мелкой сетке в модели)
        private double yMinElemSize = 8;

        private double gradFactor = 2;                  // Увеличение размеров элемента для представляния с градиентной заливкой

        private double[][] solution;                    // Массив, содержащий интерполированное решение для вывода в представлении
        private DrawOrderElem[] drawOrder;              // Массив для определения порядка отрисовки элементов

        private int xSize, ySize;                       // Размер массивов solution и drawOrder - вычисляются исходя из заданного размера клеток xElemSize и yElemSize
        private double xElemSize, yElemSize;            // Размеры элемента сетки интерполяции (но не меньше минимальных)

        private double xOrigElemSize;                   // Размеры элемента оригинальной сетки в экранных координатах
        private double yOrigElemSize;

        private double xOrigElemPhysSize;               // Размеры элемента оригинальной сетки в координатах модели
        private double yOrigElemPhysSize;

        int xOrigGridSize, yOrigGridSize;               // Размеры оригинальной сетки

        Point3DT[] pnt3 = new Point3DT[4];              // Рабочий массив для использовани при отрисовки полигонов

        public ViewSolution(double[][] originalSolution) {

            System.out.println("new ViewSolution(): " + Thread.currentThread());

            for (int i = 0; i < 4; i++) {               // Инициализация рабочего массива
                pnt3[i] = new Point3DT();
            }

            double factor = 1;
            if (isGradient) {
                factor = gradFactor;
            }

            xOrigGridSize = originalSolution[0].length;
            yOrigGridSize = originalSolution.length;

            xOrigElemPhysSize = model.getX() / (xOrigGridSize - 1);  // Размеры элемента оригинальной сетки в координатах модели
            yOrigElemPhysSize = model.getY() / (yOrigGridSize - 1);

            xOrigElemSize = model.getX() * projection.getkX() / xOrigGridSize;
            if (xOrigElemSize < xMinElemSize * factor) {
                isResized = true;
                xSize = (int) (model.getX() * projection.getkX() / (xMinElemSize * factor) + 1);
            } else {
                xSize = xOrigGridSize - 1;
            }
            xElemSize = model.getX() / xSize;

            yOrigElemSize = model.getY() * projection.getkY() / yOrigGridSize;
            if (yOrigElemSize < yMinElemSize * factor) {
                isResized = true;
                ySize = (int) (model.getY() * projection.getkY() / (yMinElemSize * factor) + 1);
            } else {
                ySize = yOrigGridSize - 1;
            }
            yElemSize = model.getY() / ySize;

            solution = new double[ySize + 1][xSize + 1];
            makeInterpolation(originalSolution);                        // Создание интерполированного решения на нулевом временном слое

            drawOrder = new DrawOrderElem[ySize * xSize];
            for (int i = 0; i < drawOrder.length; i++) {
                drawOrder[i] = new DrawOrderElem();
            }
            calculateDrawOrder();                                       // Создание начального порядка отрисовки элементов
            //redrawCanvas(0, false);
        }

        private void makeInterpolation(double[][] originalSolution) {   // Создание билинейно-интерполированного решения из настоящего решения модели

            double x, y;
            double x1, y1;
            double x2, y2;
            double x3, y3;
            double x4, y4;
            int iOrig, jOrig;
            double xOrig, yOrig;
            double T1, T2, T3, T4;

            long time5 = System.currentTimeMillis();

            synchronized (originalSolution) {
                synchronized (solution) {
                    if (isResized) {
                        for (int i = 0; i <= ySize; i++) {
                            for (int j = 0; j <= xSize; j++) {

                                x = j * xElemSize;
                                y = i * yElemSize;

                                iOrig = (int) ((y - 1e-12 * i) / yOrigElemPhysSize);
                                jOrig = (int) ((x - 1e-12 * j) / xOrigElemPhysSize);

                                xOrig = jOrig * xOrigElemPhysSize;
                                yOrig = iOrig * yOrigElemPhysSize;

                                double alpha = (xOrig + xOrigElemPhysSize - x) / xOrigElemPhysSize;
                                double beta = (yOrig + yOrigElemPhysSize - y) / yOrigElemPhysSize;

                                T1 = originalSolution[iOrig][jOrig] * alpha * beta;

                                if (iOrig < yOrigGridSize) {
                                    T2 = originalSolution[iOrig + 1][jOrig] * alpha * (1 - beta);
                                } else {
                                    T2 = 0;
                                }

                                if (iOrig < yOrigGridSize && jOrig < xOrigGridSize) {
                                    T3 = originalSolution[iOrig + 1][jOrig + 1] * (1 - alpha) * (1 - beta);
                                } else {
                                    T3 = 0;
                                }

                                if (jOrig < xOrigGridSize) {
                                    T4 = originalSolution[iOrig][jOrig + 1] * (1 - alpha) * beta;
                                } else {
                                    T4 = 0;
                                }
                                solution[i][j] = T1 + T2 + T3 + T4;
                            }

                            if (Thread.currentThread().isInterrupted()) {
                                System.out.println("makeInterpolation(): interrupted = " + (System.currentTimeMillis() - workingStart) + " " + Thread.currentThread());
                                return;
                            }
                        }
                    } else {
                        solution = originalSolution;
                    }
                }
            }
            System.out.println("makeInterpolation(): calling end time = " + (System.currentTimeMillis() - workingStart) + " "
                    + Thread.currentThread() + " time: " + (System.currentTimeMillis() - time5));
        }

        private void calculateDrawOrder() {

            long time4 = System.currentTimeMillis();

            Point3D vectorR = new Point3D();

            if (0 <= currFi && currFi < 90) {
                vectorR.x = 0;
                vectorR.y = 0;
            } else if (90 <= currFi && currFi < 180) {
                vectorR.x = scrAxeSize * projection.getkX();
                vectorR.y = 0;
            } else if (180 <= currFi && currFi < 270) {
                vectorR.x = scrAxeSize * projection.getkX();
                vectorR.y = scrAxeSize * projection.getkY();
            } else if (270 <= currFi && currFi < 360) {
                vectorR.x = 0;
                vectorR.y = scrAxeSize * projection.getkY();
            }

            double fi = currFi / 180 * Math.PI;

            double a = Math.cos(fi);
            double b = Math.sin(fi);
            double c = -(vectorR.x * a + vectorR.y * b);
            double ab = Math.sqrt(a * a + b * b);

            double x, y;

            synchronized (drawOrder) {
                for (int i = 0; i < ySize; i++) // Заполнение массива и вычисление расстояний
                {
                    for (int j = 0; j < xSize; j++) {
                        int elem = j + xSize * i;
                        drawOrder[elem].i = i;
                        drawOrder[elem].j = j;
                        x = j * xElemSize;
                        y = i * yElemSize;
                        drawOrder[elem].distance = Math.abs(a * x + b * y + c) / ab;    // Расстояние от элемента до прямой, проходящей через
                    }                                                                   // один из углов области и перпендикулярной углу зрения

                    if (Thread.currentThread().isInterrupted()) {
                        System.out.println("calculateDrawOrder(): interrupted = " + (System.currentTimeMillis() - workingStart) + " " + Thread.currentThread());
                        return;
                    }
                }
                Arrays.sort(drawOrder);             // Сортировка по расстоянию - дает порядок рисования
            }
            System.out.println("calculateDrawOrder(): calling end time = " + (System.currentTimeMillis() - workingStart) + " "
                    + Thread.currentThread() + " time: " + (System.currentTimeMillis() - time4));
        }

        private void redrawCanvas(double time, boolean isSimple) {

            System.out.println("redrawCanvas(): calling start time = " + (System.currentTimeMillis() - workingStart) + " " + Thread.currentThread());

            long time1 = System.currentTimeMillis();

            Canvas myNewCanvas = new Canvas(canvasX, canvasY);

            myNewCanvas.setUserData(time);
            drawAxes(myNewCanvas, false);

            if (Thread.currentThread().isInterrupted()) {
                System.out.println("redrawCanvas(): interrupted = " + (System.currentTimeMillis() - workingStart) + " " + Thread.currentThread());
                return;
            }

            if (!isSimple) {
                if (currFi != projection.getFi()) {
                    currFi = projection.getFi();
                    viewSolution.calculateDrawOrder();
                }

                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("redrawCanvas(): interrupted = " + (System.currentTimeMillis() - workingStart) + " " + Thread.currentThread());
                    return;
                }

                viewSolution.drawSurface(myNewCanvas);
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

        public void drawSurface(Canvas newCanvas) {                 // Построение поверхности

            GraphicsContext gc = newCanvas.getGraphicsContext2D();
            gc.setLineWidth(.5);

            synchronized (drawOrder) {
                synchronized (solution) {

                    if (isColored && tempCheckBox.isSelected()) {           // Определение максимума и минимума

                        Tmax = solution[0][0];
                        Tmin = solution[0][0];
                        for (int i = 0; i <= ySize; i++) {
                            for (int j = 0; j <= xSize; j++) {
                                Tmax = Math.max(Tmax, solution[i][j]);
                                Tmin = Math.min(Tmin, solution[i][j]);
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

                    for (int elem = 0; elem < drawOrder.length; elem++) {

                        int i = drawOrder[elem].i;
                        int j = drawOrder[elem].j;

                        pnt3[0].x = pnt3[3].x = j * xElemSize;
                        pnt3[1].x = pnt3[2].x = (j + 1) * xElemSize;
                        pnt3[0].y = pnt3[1].y = i * yElemSize;
                        pnt3[2].y = pnt3[3].y = (i + 1) * yElemSize;

                        pnt3[0].z = pnt3[0].T = solution[i][j];
                        pnt3[1].z = pnt3[1].T = solution[i][j + 1];
                        pnt3[2].z = pnt3[2].T = solution[i + 1][j + 1];
                        pnt3[3].z = pnt3[3].T = solution[i + 1][j];
                        drawPolygon(newCanvas, pnt3);

//                pnt3[0].x = (pnt3[0].x + pnt3[1].x + pnt3[2].x + pnt3[3].x) / 4;
//                pnt3[0].y = (pnt3[0].y + pnt3[1].y + pnt3[2].y + pnt3[3].y) / 4;
//                pnt3[0].z = (pnt3[0].z + pnt3[1].z + pnt3[2].z + pnt3[3].z) / 4;
//                drawText(newCanvas, "" + elem, pnt3);
//
                        if (Thread.currentThread().isInterrupted()) {
                            System.out.println("drawSurface(): interrupted = " + (System.currentTimeMillis() - workingStart) + " " + Thread.currentThread());
                            return;
                        }
                    }
                }
            }
        }
    }

    public class DrawOrderElem implements Comparable<DrawOrderElem> {   // Элемент массива для вычисления порядка рисования элементов

        public double distance;                                         // Вычисляется расстояние от элемента до прямой, параллельной плоскости экрана,
        public int i, j;                                                // потом выполняется сортировка, в результате получается порядок рисования

        @Override
        public int compareTo(DrawOrderElem t) {
            if (distance < t.distance) {
                return -1;
            }
            if (distance > t.distance) {
                return 1;
            }
            return 0;
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
        if (viewSolution != null) {
            viewSolution.redrawCanvas(model.getTime(), true);
        }
    }

    private class RedrawService extends Service {

        @Override
        protected Task createTask() {
            return new Task() {
                @Override
                protected Void call() throws Exception {
                    viewSolution.redrawCanvas(model.getTime(), false);
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
                if (System.currentTimeMillis() - refreshTime > 20) {
                    refreshTime = System.currentTimeMillis();
                    if (isVisible) {
                        executorDrawService.execute(() -> {
                            double currT = model.getSolution().getTime();
                            synchronized (model.getSolution().prevSol) {
                                viewSolution.makeInterpolation(model.getSolution().prevSol);
                            }
                            viewSolution.redrawCanvas(currT, false);
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

        //isSceneDrawn = true;
        timeLabel.setText("Текущее время:  " + String.format("%06.1f", (double) mainCanvas.getUserData()));

        System.out.println("******************************************************redrawScene(): calling time = " + (System.currentTimeMillis() - workingStart) + " "
                + Thread.currentThread() + " time: " + (System.currentTimeMillis() - time2));
    }

    public void drawAxes(Canvas newCanvas, boolean isOnlyZ) // Рисование осей координат
    {
        GraphicsContext gc = newCanvas.getGraphicsContext2D();
        gc.setStroke(Color.BLACK);
        gc.setFill(Color.WHITE);
        gc.setLineWidth(1);
        gc.setLineJoin(StrokeLineJoin.ROUND);

        if (!isOnlyZ) {

            point3[0].x = 0;
            point3[0].y = 0;
            point3[0].z = 0;
            point3[1].x = X;
            point3[1].y = 0;
            point3[1].z = 0;
            drawLine(newCanvas, point3);
            point3[0].x = 0;
            point3[0].y = 0;
            point3[0].z = 0;
            point3[1].x = 0;
            point3[1].y = Y;
            point3[1].z = 0;
            drawLine(newCanvas, point3);
            point3[0].x = 0;
            point3[0].y = 0;
            point3[0].z = Zmin;
            point3[1].x = 0;
            point3[1].y = 0;
            point3[1].z = Zmax;
            drawLine(newCanvas, point3);

            point3[0].x = X * 1.05;                         // Подписи к осям
            point3[0].y = 0;
            point3[0].z = 0;
            drawText(newCanvas, "x", point3);

            point3[0].x = 0;
            point3[0].y = Y * 1.05;
            point3[0].z = 0;
            drawText(newCanvas, "y", point3);

            point3[0].x = 0;
            point3[0].y = 0;
            point3[0].z = Zmax * 1.05;
            drawText(newCanvas, "T", point3);
        } else {
            if (currFi >= 180 && currFi <= 360) {
                point3[0].x = 0;
                point3[0].y = 0;
                point3[0].z = Zmin;
                point3[1].x = 0;
                point3[1].y = 0;
                point3[1].z = Zmax;
                drawLine(newCanvas, point3);
                point3[0].x = 0;
                point3[0].y = 0;
                point3[0].z = Zmax * 1.05;
                drawText(newCanvas, "T", point3);
            }
        }
    }

    public void drawLine(Canvas newCanvas, Point3D[] pnt3) // Проецирование и рисование трехмерного отрезка
    {
        GraphicsContext gc = newCanvas.getGraphicsContext2D();

        projection.convertToScreen(pnt3, workX, workY, 2);
        gc.strokeLine(workX[0], workY[0], workX[1], workY[1]);
    }

    public void drawText(Canvas newCanvas, String s, Point3D[] pnt3) // Проецирование и рисование текстовой строки
    {
        GraphicsContext gc = newCanvas.getGraphicsContext2D();

        projection.convertToScreen(pnt3, workX, workY, 1);
        gc.strokeText(s, workX[0], workY[0]);
    }

    public void drawPolygon1(Canvas newCanvas, Point3D[] pnt3) // Проецирование и рисование четырехугольника
    {
//        Color currFillColor;
//        Color currStrokeColor;
//
//        GraphicsContext gc = newCanvas.getGraphicsContext2D();
//
//        if (isColored) {
//            if (isGradient) {
//                center.x = (pnt3[0].x + pnt3[1].x + pnt3[2].x + pnt3[3].x) / 4;
//                center.y = (pnt3[0].y + pnt3[1].y + pnt3[2].y + pnt3[3].y) / 4;
//                center.z = (pnt3[0].z + pnt3[1].z + pnt3[2].z + pnt3[3].z) / 4;
//
//                for (int i = 0; i < 4; i++) {
//                    triang[0] = pnt3[i];
//                    triang[1] = pnt3[(i + 1) % 4];
//                    triang[2] = center;
//                    drawTriangle(gc, triang);
//                }
//            } else {
//                projection.convertToScreen(pnt3, workX, workY, 4);
//                double Tmean = (pnt3[0].z + pnt3[1].z + pnt3[2].z + pnt3[3].z) / 4;
//                int colorIndex = (int) ((Tmean - Tmin) / (Tmax - Tmin) * colorNumber);
//                if (colorIndex > colorNumber) {
//                    currFillColor = Color.WHITESMOKE;
//                    currStrokeColor = Color.GRAY;
//                } else if (colorIndex < 0) {
//                    currFillColor = Color.BLACK;
//                    currStrokeColor = Color.GRAY;
//                } else {
//                    currFillColor = colors.get((int) ((Tmean - Tmin) / (Tmax - Tmin) * colorNumber));
//                    currStrokeColor = currFillColor;
//                }
//                gc.setFill(currFillColor);
//                gc.setStroke(currStrokeColor);
//                //gc.setStroke(currFillColor);
//                gc.fillPolygon(workX, workY, 4);
//                gc.strokePolygon(workX, workY, 4);
//            }
//        } else {
//            projection.convertToScreen(pnt3, workX, workY, 4);
//            gc.fillPolygon(workX, workY, 4);
//            gc.strokePolygon(workX, workY, 4);
//        }
    }

    public void drawPolygon(Canvas newCanvas, Point3DT[] pnt3) // Проецирование и рисование четырехугольника
    {
        GraphicsContext gc = newCanvas.getGraphicsContext2D();

        if (isColored) {
            if (isGradient && !(pnt3[0].T == pnt3[1].T && pnt3[1].T == pnt3[2].T && pnt3[2].T == pnt3[3].T)) {  // С градиентной заливкой элементов

                Color currFillColorMax;
                Color currFillColorMin;
                Color currStrokeColor;

                projection.convertToScreen(pnt3, workX, workY, 4);              // Проецирование точек элемента на систему координат экрана

                double[] gradScrX = new double[2];                              // Массив для проекции градиента
                double[] gradScrY = new double[2];

                double startGradX, startGradY;                                  // Координаты начала и конца градиента в экранных координатах
                double endGradX, endGradY;

                double gradX, gradY;                                            // Координаты проекций градиента на плоскости куба в системе координат модели
                double gradX1, gradY1;
                double gradX2, gradY2;

                if (pnt3[1].x != pnt3[0].x) {
                    gradX1 = (pnt3[1].T - pnt3[0].T) / (pnt3[1].x - pnt3[0].x);
                    gradX2 = (pnt3[3].T - pnt3[2].T) / (pnt3[3].x - pnt3[2].x);
                } else {
                    gradX1 = (pnt3[2].T - pnt3[1].T) / (pnt3[2].x - pnt3[1].x);
                    gradX2 = (pnt3[0].T - pnt3[3].T) / (pnt3[0].x - pnt3[3].x);
                }
                gradX = (gradX1 + gradX2) / 2;

                if (pnt3[1].y != pnt3[0].y) {
                    gradY1 = (pnt3[1].T - pnt3[0].T) / (pnt3[1].y - pnt3[0].y);
                    gradY2 = (pnt3[3].T - pnt3[2].T) / (pnt3[3].y - pnt3[2].y);

                } else {
                    gradY1 = (pnt3[2].T - pnt3[1].T) / (pnt3[2].y - pnt3[1].y);
                    gradY2 = (pnt3[0].T - pnt3[3].T) / (pnt3[0].y - pnt3[3].y);
                }
                gradY = (gradY1 + gradY2) / 2;

//                Point3DT[] grad = {new Point3DT(), new Point3DT(gradX, gradY, 0, 0)};
//                projection.convertToScreen(grad, gradScrX, gradScrY, 2);                        // Проецирование градиента на систему координат экрана
//                double gradientX = gradScrX[1] - gradScrX[0];
//                double gradientY = gradScrY[1] - gradScrY[0];
//                double gradientXY = Math.sqrt(gradientX * gradientX + gradientY * gradientY);
//                gradientX /= gradientXY;
//                gradientY /= gradientXY;
                double perpGradX = 0;                                                           // Перпендикуляр к градиенту в координатах модели
                double perpGradY = 0;
                perpGradX = -gradY;                                                             // Находим перпендикуляр к градиенту в трех случаях
                perpGradY = gradX;

                double[] perpX = new double[2];
                double[] perpY = new double[2];
                Point3DT[] perp = {new Point3DT(), new Point3DT(perpGradX, perpGradY, 0, 0)};
                projection.convertToScreen(perp, perpX, perpY, 2);                              // Проецирование перпендикуляра на систему координат экрана
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

            } else {                            // Без градиентной заливки элементов - заливка элемента цветом средней температуры
                Color currFillColor;
                Color currStrokeColor;

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
                    currStrokeColor = Color.WHITESMOKE;
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
        } else {
            projection.convertToScreen(pnt3, workX, workY, 4);
            gc.fillPolygon(workX, workY, 4);
            gc.strokePolygon(workX, workY, 4);
        }
    }

    public void drawTriangle(GraphicsContext gc, Point3D[] triang) {

//        Color currStrokeColor;
//
//        double startGradX, startGradY;
//        double endGradX, endGradY;
//
//        Arrays.sort(triang);
//        projection.convertToScreen(triang, workX, workY, 3);
//
//        if (Math.abs(triang[0].z - triang[1].z) < 1e-12) {
//            startGradX = (workX[0] + workX[1]) / 2;
//            startGradY = (workY[0] + workY[1]) / 2;
//        } else {
//            startGradX = workX[0];
//            startGradY = workY[0];
//        }
//        if (Math.abs(triang[1].z - triang[2].z) < 1e-12) {
//            endGradX = (workX[1] + workX[2]) / 2;
//            endGradY = (workY[1] + workY[2]) / 2;
//        } else {
//            endGradX = workX[2];
//            endGradY = workY[2];
//        }
//
//        double max = triang[2].z;
//        double min = triang[0].z;
//        Color currFillColorMax;
//        Color currFillColorMin;
//
//        int colorIndexMax = (int) ((max - Tmin) / (Tmax - Tmin) * colorNumber);
//        if (colorIndexMax > colorNumber) {
//            currFillColorMax = Color.WHITESMOKE;
//            currStrokeColor = Color.GRAY;
//        } else if (colorIndexMax < 0) {
//            currFillColorMax = Color.BLACK;
//            currStrokeColor = Color.GRAY;
//        } else {
//            currFillColorMax = colors.get((int) ((max - Tmin) / (Tmax - Tmin) * colorNumber));
//            currStrokeColor = Color.WHITE;
//        }
//
//        int colorIndexMin = (int) ((min - Tmin) / (Tmax - Tmin) * colorNumber);
//        if (colorIndexMin > colorNumber) {
//            currFillColorMin = Color.WHITESMOKE;
//            currStrokeColor = Color.GRAY;
//        } else if (colorIndexMin < 0) {
//            currFillColorMin = Color.BLACK;
//            currStrokeColor = Color.GRAY;
//        } else {
//            currFillColorMin = colors.get((int) ((min - Tmin) / (Tmax - Tmin) * colorNumber));
//            currStrokeColor = Color.WHITE;
//        }
//
//        double Tmean = (triang[0].z + triang[1].z + triang[2].z) / 3;
//        int colorIndex = (int) ((Tmean - Tmin) / (Tmax - Tmin) * colorNumber);
//        if (colorIndex >= colorNumber) {
//            currStrokeColor = Color.GRAY;
//        } else if (colorIndex < 0) {
//            currStrokeColor = Color.GRAY;
//        } else {
//            currStrokeColor = colors.get((int) ((Tmean - Tmin) / (Tmax - Tmin) * colorNumber));
//        }
//
//        Stop[] stops = new Stop[]{new Stop(0, currFillColorMin), new Stop(1, currFillColorMax)};
//        LinearGradient lg = new LinearGradient(startGradX, startGradY, endGradX, endGradY, false, CycleMethod.NO_CYCLE, stops);
//        gc.setFill(lg);
//        gc.setStroke(currStrokeColor);
//
//        gc.fillPolygon(workX, workY, 3);
//        gc.strokePolygon(workX, workY, 3);
//        gc.setStroke(Color.WHITE);
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

        if (canvasY != y) {
            synchronized (model.getSolution().prevSol) {
                viewSolution = new ViewSolution(model.getSolution().prevSol);   // Новый класс интерполированного решения с новыми размерами
            }
        }
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
