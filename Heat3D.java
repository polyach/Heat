/** **************************************************************************************************************************************************** */
/* 13.11.2018  Пример использования функций пакета программ решения разреженных линейных систем уравнений большой размерности с симметричной матрицей    */
 /*            Решается трехмерное уравнение теплопроводности в кубической области {(x, y, z): 0<=x<=X, 0<=y<=Y, 0<=z<=Z } c граничными условиями        */
 /*            первого рода:                                                                                                                             */
 /*                              2                                                                                                                       */
 /*            f'(x, y, z, t) - a * Δf(x, y, z, t) = g(x, y, z, t), где Δ - оператор Лапласа;                                                            */
 /*             t                                                                                                                                        */
 /*                                                                                                                                                      */
 /*            f│ = h (x, y, z, t)│ - граничные условия;                                                                                                */
 /*              г    o             г                                                                                                                    */
 /*                                                                                                                                                      */
 /*            f│  =  f (x, y, z)    - начальные условия                                                                                                */
 /*              t=0    o                                                                                                                                */
 /*                                                                                                                                                      */
 /*            В программе демонстрируется использование функции разложения симметричной матрицы (один раз) и функции                                    */
 /*            итерационного вычисления решения в цикле для каждого временного слоя.                                                                     */
 /*            Функции определены в виде методов в классе Invert.                                                                            */
 /*            Реализован паттерн программирования MVC (Модель в этом файле, Представление и Контроллер в файле Controller3D.java).                      */
 /*                                                                                                                                                      */
 /*            Cтруктура программы :                                                                                                                     */
 /*            1. Определение функций, переменных и констант.                                                                                            */
 /*            2. Запрос массивов представления матриц и векторов (в соответствующих конструкторах классов матриц).                                      */
 /*            3. Заполнение матрицы задачи с использ. семиточечного шаблона (также в конструторе исходной матрицы задачи).                              */
 /*            4. Вызов функции разложения матрицы линейного уравнения (в конструкторе факторизованной матрицы задачи).                                  */
 /*            5. В цикле по слоям времени:                                                                                                              */
 /*               Рисование решения на экране в виде прямоугольного параллелепипеда с вырезом.                                                           */
 /*               Вызов функции итерационного вычисления решения на очередном временном слое с использованием факторизованной матрицы.                   */
/** **************************************************************************************************************************************************** */
package advancedmedia;

public class Heat3D {

//    final private double X = 1;                     // Границы области расчета
//    final private double Y = 1;
//    final private double Z = 1;
//    final private double X = 0.036;                    
//    final private double Y = 0.036;
//    final private double Z = 0.047;
    final private double X = 0.092;                     
    final private double Y = 0.092;
    final private double Z = 0.119;
    
    public double getX() {
        return X;
    }

    public double getY() {
        return Y;
    }

    public double getZ() {
        return Z;
    }

    final private int mX = 24;                              // Количество отрезков разбиения по осям
    final private int mY = 24;
    final private int mZ = 30;

    final private double dX = X / mX;                       // Размеры отрезков разбиения в физических единицах
    final private double dY = Y / mY;
    final private double dZ = Z / mZ;
    final private double dX2 = dX * dX;
    final private double dY2 = dY * dY;
    final private double dZ2 = dZ * dZ;

    final private int N = (mX - 1) * (mY - 1) * (mZ - 1);   // Размеры матриц и векторов в линейных уравнениях

    private double g(double x, double y, double z, double t)    // Правая часть уравнения
    {
//        if (Math.sqrt((x - 0.5) * (x - 0.5) + (y - 0.5) * (y - 0.5) + (z - 0.5) * (z - 0.5)) < 0.2 && t < 10) {
//            return .3;
//        } else {
//            return 0;
//        }

        return 0;

//        double k = 1;
//        double sin = Math.sin(Math.PI * (x + y + z));
//        return sin * (3 * ALPHA * Math.PI * Math.PI * Math.cos(k * t) - k * Math.sin(k * t));
    }

    private double f0(double x, double y, double z)             // Начальные условия
    {
        return 3;

//        return Math.sin(4 * Math.PI * (x + y)) / 20.;
//        return Math.sin(Math.sqrt(x * x + y * y) * Math.PI * 2);
//        return Math.sin(Math.PI * (x + y + z));
    }

    private double h0(double x, double y, double z, double t)   // Граничные условия
    {
//        if (x == 0 || x == X || y == 0 || y == Y || z == 0 || z == Z) {
            return 100;
//        }
//        return 0;

//        double k = 1;
//        return Math.sin(Math.PI * (x + y + z)) * Math.cos(k * t);
    }

    public double realT(double x, double y, double z, double t) {
        double k = 1;
        return Math.sin(Math.PI * (x + y + z)) * Math.cos(k * t);
    }

//    final private double ALPHA = 1.6563e-4; // Константный коэффициент a^2 в уравнении теплопроводности
    final private double ALPHA = 1.43e-7;
//    final pri vate double ALPHA = 1e-7;
    final private double dT = 0.01;         // Шаг по времени
    private double t = 0;                   // Текущее время, для которого считается решение

    public double getTime() {
        return t;
    }

    // Параметры, используемые в процедуре разложения матрицы
    final private int FACTOR = 10;          // Предполагаемое увеличение числа элементов факторизованной матрицы по сравнению с исходной
    final private double BARRIER = 1e-4;    // Барьер обнуления малых элементов для процедуры разложения
    final private int P = 5;                // Cколько элементов резервировать в списках представления матрицы (в процедуре разложения)
    final private double DELTA = 0.000001;  // Минимально допустимое отличие от нуля элементов диагональной матрицы разложения

    // Параметры, используемые в процедуре итерационного вычисления решения
    final private double EPS = 1e-10;       // Точность итераций (условие выхода: ||Ax - b|| < EPS)
    final private int MAXIT = 100;          // Максимальное число итераций в итерационных процедурах

    private Solution solution;              // Класс с трехмерным массивом, содержащий решение на текущем временном слое, начиная с t = 0
    private long initTime;                  // Время вызова конструктора

    public Solution getSolution() {
        return solution;
    }

    private VectorB vectorB;                // Класс с массивом, представляющим правую часть уравнения Ax = b
    private MatrixA0 matrixA0;              // Класс с массивами, представляющими исходную матрицу
    private MatrixA matrixA;                // Класс с массивами, представляющими факторизованную матрицу
    private double[] vectorX;               // Массив, содержащий вектор решения x
    private Invert invert;                  // Класс с функциями решения линейных уравнений
    private int retValue;                   // Последний код возврата из функции факторизации или итерационного нахождения решения

    private boolean realTime = false;       // Нужно ли считать в реальном режиме времени (для мелких сеток не работает т.к. время расчета слоя больше реального)

    public void setRealTime(boolean realTime) {
        this.realTime = realTime;
    }
    private long startTime;

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public class Solution {             // Представление решения

        private double t = 0;           // Текущий момент времени

        public double getTime() {
            return t;
        }

        private double[][][] prevSol;   // Массив, содержащий решение в предыдущий момент времени

        public double[][][] getPrevSol() {
            return prevSol;
        }

        private double[][][] sol;       // Массив, содержащий решение в текущий момент времени

        public Solution() {

            System.out.println("Запрос двумерного массива, содержащего решение на каждом временном слое:");
            try {
                System.out.printf("Запрос %d байт памяти для массива solution ... ", (mZ + 1) * (mY + 1) * (mX + 1) * Double.BYTES);
                sol = new double[mZ + 1][mY + 1][mX + 1];
                System.out.println("Ok");
            } catch (OutOfMemoryError e) {
                System.out.println("failed");
                return;
            }

            for (int k = 0; k <= mZ; k++) {
                for (int i = 0; i <= mY; i++) {
                    for (int j = 0; j <= mX; j++) {
                        sol[k][i][j] = f0(dX * j, dY * i, dZ * k);
                    }
                }
            }
            prevSol = sol;
        }

        private void fillSolution(double t, double x[]) // Заполнение массива из полученного вектора решения системы линейных уравнений Ax = b
        {                                               // и заданных граничных условий для граничных точек
            this.t = t;

            sol = new double[mZ + 1][mY + 1][mX + 1];
            for (int k = 1; k <= mZ - 1; k++) {
                for (int i = 1; i <= mY - 1; i++) {
                    for (int j = 1; j <= mX - 1; j++) {         // Из вектора решения х для всех внутренних точек
                        sol[k][i][j] = x[j + (i - 1) * (mX - 1) + (k - 1) * (mY - 1) * (mX - 1)];
                    }
                }
            }
            for (int i = 0; i <= mY; i++) {
                for (int j = 0; j <= mX; j++) {                 // По границе на грани Z = 0 и противоположной
                    sol[0][i][j] = h0(dX * j, dY * i, 0, t);
                    sol[mZ][i][j] = h0(dX * j, dY * i, Z, t);
                }
            }

            for (int k = 0; k <= mZ; k++) {
                for (int j = 0; j <= mX; j++) {                 // По границе на грани Y = 0 и противоположной
                    sol[k][0][j] = h0(dX * j, 0, dZ * k, t);
                    sol[k][mY][j] = h0(dX * j, Y, dZ * k, t);
                }
            }

            for (int k = 0; k <= mZ; k++) {
                for (int i = 0; i <= mY; i++) {                 // По границе на грани X = 0 и противоположной
                    sol[k][i][0] = h0(0, dY * i, dZ * k, t);
                    sol[k][i][mX] = h0(X, dY * i, dZ * k, t);
                }
            }

            synchronized (prevSol) {
                prevSol = sol;
                sol = null;
            }
        }
    }

    private class MatrixA0 {            // Представление исходной матрицы

        private int n;                  // Размер исходной матрицы
        private int numElems;           // Число элементов в матрице
        private double[] _a0 = null;    // Массивы, содержащие исходную матрицу
        private int[] _cn0 = null;
        private int[] _pnt0_0 = null;
        private int[] _pnt0_1 = null;

        public MatrixA0(int n, int elems) {
            this.n = n;
            this.numElems = elems;
            try {
                System.out.println("Запрос массивов для представления исходной матрицы:");
                System.out.printf("Запрос %d байт памяти для массива _a0 ... ", numElems * Double.BYTES);
                _a0 = new double[numElems];
                System.out.println("Ok");

                System.out.printf("Запрос %d байт памяти для массива _cn0 ... ", numElems * Integer.BYTES);
                _cn0 = new int[numElems];
                System.out.println("Ok");

                System.out.printf("Запрос %d байт памяти для массива _pnt0_0 ... ", (n + 1) * Integer.BYTES);
                _pnt0_0 = new int[n + 1];
                System.out.println("Ok");

                System.out.printf("Запрос %d байт памяти для массива _pnt0_1 ... ", (n + 1) * Integer.BYTES);
                _pnt0_1 = new int[n + 1];
                System.out.println("Ok");
            } catch (OutOfMemoryError e) {
                System.out.println("failed");
                return;
            }

            // Заполнение массивов
            System.out.print("Заполнение матрицы элементами... ");

            int pos = 0;      // Текущая позиция в массивах _a0 и _cn0

            // Коэффициенты на диагоналях матрицы
            double c1 = 1. + 2 * ALPHA * dT * (1 / dX2 + 1 / dY2 + 1 / dZ2);    // на главной
            double c2 = -ALPHA * dT / dX2;                                      // на прилегающих к главной
            double c3 = -ALPHA * dT / dY2;                                      // на отнесенных на mX - 1 элементов в стороны от главной
            double c4 = -ALPHA * dT / dZ2;                                      // на отнесенных на (mX - 1)*(mY - 1) элементов в стороны от главной

            for (int k = 1; k < mZ; k++) // Внешний цикл по слоям сетки (вдоль оси z)
            {
                for (int i = 1; i < mY; i++) // Цикл по линиям сетки (вдоль оси y)
                {
                    for (int j = 1; j < mX; j++) // Цикл по узлам слоя сетки (вдоль оси x)
                    {
                        int eqNumber = (k - 1) * (mY - 1) * (mX - 1) + (i - 1) * (mX - 1) + j;  // Номер уравнения

                        _pnt0_0[eqNumber] = pos;                // _pnt0_0[№]+1 указывает на начало №-й строки матрицы в массивах _a0 и _cn0

                        _a0[++pos] = c1;                        // Диагональный элемент
                        _cn0[pos] = eqNumber;                   // и его столбцовый индекс

                        if (j < mX - 1) // В случае, если уравнение не для правой крайней в линии точки, заполняем
                        {                                       // элемент, располож. справа от диагонали и его столбцовый индекс
                            _a0[++pos] = c2;
                            _cn0[pos] = eqNumber + 1;
                        }

                        if (i < mY - 1) // В случае, если уравнение не для точек из крайней в слое линии, заполняем
                        {                                       // элементы, расположенные в этой линии и их столбцовые индексы
                            _a0[++pos] = c3;                    // Элемент матрицы, располож. справа через mX - 1 позиций от диагонального
                            _cn0[pos] = eqNumber + mX - 1;      // и его столбцовый индекс
                        }

                        if (k < mZ - 1) // В случае, если уравнение не для точек из последнего по оси z слоя, заполняем
                        {                                       // элементы, расположенные в этом слое и их столбцовые индексы
                            _a0[++pos] = c4;                            // Элемент матрицы, располож. справа через (mX - 1)*(mY - 1) позиций от диагонального
                            _cn0[pos] = eqNumber + (mX - 1) * (mY - 1); // и его столбцовый индекс
                        }

                        _pnt0_1[eqNumber] = pos;                // _pnt0_1[№] указывает на конец №-й строки матрицы в массивах _a0 и _cn0
                    }
                }
            }
            System.out.println("Ok");
        }
    }

    private class MatrixA {             // Представление факторизованной матрицы

        private int n;                  // Размер исходной матрицы
        private int numElems;           // Число элементов в матрице

        private double[] _a = null;     // Указатели на массивы, для размещения факторизованной матрицы. 
        private double[] _diag = null;  // Используются также как рабочее пространство для вычислений. 
        private int[] _cn = null;       // После вызова функции factorize() содержат приближенное разложение переданной матрицы.
        private int[] _pnt_0 = null;
        private int[] _pnt_1 = null;    // Массивы должны быть аллокированы пользователем
        private int[] _s = null;        // и переданы в функцию invert() или factorize()

        private int[] _ln = null;       // Указатели на рабочие массивы, используемые
        private int ln_len;             // исключительно внутри процедуры факторизации.
        private int[] _pnt_2 = null;    // Массивы должны быть аллокированы пользователем
        private int[] _pnt_3 = null;    // и переданы в функцию invert() или factorize().

        public MatrixA(int n, int numElems) {
            this.n = n;
            this.numElems = numElems;
            this.ln_len = numElems;

            try {
                System.out.println("Запрос массивов для представления факторизованной матрицы:");

                System.out.printf("Запрос %d байт памяти для массива _a ... ", numElems * Double.BYTES);
                _a = new double[numElems];
                System.out.println("Ok");

                System.out.printf("Запрос %d байт памяти для массива _cn ... ", numElems * Integer.BYTES);
                _cn = new int[numElems];
                System.out.println("Ok");

                System.out.printf("Запрос %d байт памяти для массива _pnt_0 ... ", (n + 1) * Integer.BYTES);
                _pnt_0 = new int[n + 1];
                System.out.println("Ok");

                System.out.printf("Запрос %d байт памяти для массива _pnt_1 ... ", (n + 1) * Integer.BYTES);
                _pnt_1 = new int[n + 1];
                System.out.println("Ok");

                System.out.printf("Запрос %d байт памяти для массива _s ... ", (n + 1) * Integer.BYTES);
                _s = new int[n + 1];
                System.out.println("Ok");

                System.out.printf("Запрос %d байт памяти для массива _diag ... ", (n + 1) * Double.BYTES);
                _diag = new double[n + 1];
                System.out.println("Ok");

                System.out.println("Запрос pабочих массивов:");

                System.out.printf("Запрос %d байт памяти для массива _ln ... ", ln_len * Integer.BYTES);
                _ln = new int[ln_len];
                System.out.println("Ok");

                System.out.printf("Запрос %d байт памяти для массива _pnt_2 ... ", (n + 1) * Integer.BYTES);
                _pnt_2 = new int[n + 1];
                System.out.println("Ok");

                System.out.printf("Запрос %d байт памяти для массива _pnt_3 ... ", (n + 1) * Integer.BYTES);
                _pnt_3 = new int[n + 1];
                System.out.println("Ok");
            } catch (OutOfMemoryError e) {
                System.out.println("failed");
                return;
            }

            // Факторизация
            System.out.println("Барьер: " + BARRIER);
            System.out.print("Вызов функции разложения матрицы ... ");

            long time0 = System.currentTimeMillis();

            // Вызов функции из dll-библиотеки для нахождения факторизации матрицы - помещается в массивы _a, _cn, _pnt_0, _pnt_1,
            int retValue = invert.factorize(n, P, BARRIER, // Разложение матрицы
                    matrixA0._a0, matrixA0._cn0, matrixA0._pnt0_0, matrixA0._pnt0_1,
                    _a, _cn, numElems, _pnt_0, _pnt_1,
                    _ln, ln_len, _pnt_2, _pnt_3,
                    _diag, _s, DELTA);

            if (retValue != OK) {       // Анализ кода возврата           
                System.out.println("failed");
                return;
            }

            System.out.println("Время факторизации: " + (System.currentTimeMillis() - time0));

            _ln = null;                 // Эти массивы больше не нужны, обнуляем ссылки для их удаления
            _pnt_2 = null;
            _pnt_3 = null;
        }
    }

    private class VectorB {             // Представление правой части уравнения Ax = b

        private int n;
        private double[] b;

        public VectorB(int n) {
            this.n = n;

            System.out.printf("Запрос %d байт памяти для массива вектора правой части b ... ", (n + 1) * Double.BYTES);
            try {
                b = new double[n + 1];
                System.out.println("Ok");
            } catch (OutOfMemoryError e) {
                System.out.println("failed");
                return;
            }
        }

        private void fillVectorB(double t, double[][][] prevSol) {

            // Коэффициенты для переноса значений в правую часть
            double c2 = -ALPHA * dT / dX2;                      // на прилегающих к главной
            double c3 = -ALPHA * dT / dY2;                      // на отнесенных на Mx - 1 элементов в стороны от главной
            double c4 = -ALPHA * dT / dZ2;                      // на отнесенных на (My - 1)*(Mx - 1) элементов в стороны от главной

            for (int k = 1; k < mZ; k++) {                      // Значения правой части из основного уравнения для всех точек
                for (int i = 1; i < mY; i++) {
                    for (int j = 1; j < mX; j++) {
                        int eqNumber = (k - 1) * (mY - 1) * (mX - 1) + (i - 1) * (mX - 1) + j;  // Номер элемента
                        b[eqNumber] = g(j * dX, i * dY, k * dZ, t) * dT + prevSol[k][i][j];
                    }
                }
            }

            for (int i = 1; i < mY; i++) {                  // Для приграничных точек на грани z = 0 и противоположной стороны нужно вычесть 
                for (int j = 1; j < mX; j++) {              // из правой части известные из гран. условий значения
                    int eqNumber = (i - 1) * (mX - 1) + j;                                      // Номер элемента на грани z = 0
                    b[eqNumber] -= c4 * h0(j * dX, i * dY, 0, t);
                    eqNumber = (mZ - 2) * (mY - 1) * (mX - 1) + (i - 1) * (mX - 1) + j;         // Номер элемента на грани z = Z
                    b[eqNumber] -= c4 * h0(j * dX, i * dY, Z, t);
                }
            }

            for (int k = 1; k < mZ; k++) {                  // Для приграничных точек на грани y = 0 и противоположной стороны нужно вычесть 
                for (int j = 1; j < mX; j++) {              // из правой части известные из гран. условий значения
                    int eqNumber = (k - 1) * (mY - 1) * (mX - 1) + j;                           // Номер элемента на грани y = 0
                    b[eqNumber] -= c3 * h0(j * dX, 0, k * dZ, t);
                    eqNumber = (k - 1) * (mY - 1) * (mX - 1) + (mY - 2) * (mX - 1) + j;         // Номер элемента на грани y = Y
                    b[eqNumber] -= c3 * h0(j * dX, Y, k * dZ, t);
                }
            }

            for (int k = 1; k < mZ; k++) {                  // Для приграничных точек на грани x = 0 и противоположной стороны нужно вычесть 
                for (int i = 1; i < mY; i++) {              // из правой части известные из гран. условий значения
                    int eqNumber = (k - 1) * (mY - 1) * (mX - 1) + (i - 1) * (mX - 1) + 1;          // Номер элемента на грани x = 0
                    b[eqNumber] -= c2 * h0(0, i * dY, k * dZ, t);
                    eqNumber = (k - 1) * (mY - 1) * (mX - 1) + (i - 1) * (mX - 1) + mX - 1;   // Номер элемента на грани x = X
                    b[eqNumber] -= c2 * h0(X, i * dY, k * dZ, t);
                }
            }
        }
    }

    public Heat3D() {

        initTime = System.currentTimeMillis();

        invert = new Invert();                      // Экземпляр класса для нахождения решения линейных уравнений

        solution = new Solution();                  // Класс с двумерным массивом, содержащий решение на текущем временном слое, начиная с t = 0
        matrixA0 = new MatrixA0(N, N * 4);          // Класс с массивами, представляющими исходную матрицу
        matrixA = new MatrixA(N, N * 4 * FACTOR);   // Класс с массивами, представляющими факторизованную матрицу
        vectorB = new VectorB(N);                   // Класс с массивом, представляющим правую часть уравнения Ax = b

        try {       // Запрос массива для вектора решения
            System.out.printf("Запрос %d байт памяти для массива вектора решения x ... ", (N + 1) * Double.BYTES);
            vectorX = new double[N + 1];
            System.out.println("Ok");
        } catch (OutOfMemoryError e) {
            System.out.println("failed");
            return;
        }
    }

    public int calculateNextLayer() {
        if (retValue == OK) {       // Если факторизация матрицы прошла успешно, можем делать вычисления дальше

            long time3 = System.currentTimeMillis();

            t += dT;
            //System.out.printf("Итерации по методу наименьших квадратов с ускорением: t = %f ... ", t);

            if (realTime) {
                while (System.nanoTime() - startTime < dT * 1000000000) {
                    Thread.yield();
                }
                startTime = System.nanoTime();
            }

            vectorB.fillVectorB(t, solution.prevSol); // Заполнение правой части уравнения Ax = b на очередном шаге по времени

            // Вызов функции из библиотеки для нахождения решения - помещается в vectorX
            retValue = invert.iterations2(N, matrixA0._a0, matrixA0._cn0, matrixA0._pnt0_0, matrixA0._pnt0_1,
                    matrixA._a, matrixA._cn, matrixA._pnt_0, matrixA._pnt_1,
                    matrixA._diag, matrixA._s,
                    vectorB.b, vectorX, EPS, MAXIT);

            //analyseRetValue(retValue);    // Анализ кода возврата
            if (retValue == OK) {           // Формирование ответа на трехмерной сетке
                solution.fillSolution(t, vectorX);
                
                //validateSolve();          // Проверка правильности решения (вычисляется для известного решения, задаваемого в методе realT(...));
            }

            System.out.println("3D: calculateNextLayer(): " + Math.rint(t / dT) + " calling end time = " + (System.currentTimeMillis() - initTime) + " "
                    + Thread.currentThread() + " time: " + (System.currentTimeMillis() - time3) + " priority = " + Thread.currentThread().getPriority());

            return retValue;
        } else {
            return retValue;                // Возвращаем неудачный код завершения с предыдущего шага или факторизации    
        }
    }

    public void validateSolve() {
        double norm_dx = 0;
        double norm_x = 0;
        double x, dx;
        for (int k = 0; k <= mZ; k++) {
            for (int i = 0; i <= mY; i++) {
                for (int j = 0; j <= mX; j++) {
                    x = solution.prevSol[k][i][j];
                    dx = x - realT(j * dX, i * dY, k * dZ, t);
                    norm_x += x * x;
                    norm_dx += dx * dx;
                }
            }
        }
        norm_x = Math.sqrt(norm_x);
        norm_dx = Math.sqrt(norm_dx);

        System.out.println("validateSolve(): norm_dx = " + norm_dx + " norm_x = " + norm_x);
    }

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

//    private void analyseRetValue(int retValue) // Анализ возвращаемого значения
//    {
//        switch (retValue) {
//            case OK:
//                System.out.println("Ok");
//                break;
//            case NO_SATISFIED_ACCURACY:
//                System.out.println("failed\nOшибка: требуемая точность не достигнута");
//                break;
//            case MEMORY_LACK:
//                System.out.println("failed\nOшибка: недостаточно свободной памяти");
//                break;
//            case INVALID_ACTUAL_PARAM:
//                System.out.println("failed\nOшибка в переданных параметрах");
//                break;
//            case NULL_POINTER_TRANSMISSION:
//                System.out.println("failed\nOшибка: функции передан нулевой указатель");
//                break;
//            case TOO_LARGE_INDEX:
//                System.out.println("failed\nОшибка в передаваемых данных: индекс элемента матрицы больше ее размера");
//                break;
//            case TWO_EQUAL_ELEM_INDEXES:
//                System.out.println("failed\nОшибка в передаваемых данных: два разных элемента матрицы");
//                break;
//            case ITER_PROCESS_DIVERGENCE:
//                System.out.println("failed\nОшибка: итерационный процесс расходится");
//                break;
//            case BAD_MATRIX:
//                System.out.println("failed\nОшибка: матрица плохо обусловлена");
//                break;
//            case TOO_FEW_ROOM_cn_MAS:
//                System.out.println("failed\nОшибка: не хватает места в строчно-упорядоченном массиве");
//                break;
//            case TOO_FEW_ROOM_ln_MAS:
//                System.out.println("failed\nОшибка: не хватает места в столбцово-упорядоченном массиве");
//                break;
//        }
//    }
}
