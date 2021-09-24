package advancedmedia;

public class CurrentProjection {        // Класс для проецирования физических 3D-точек модели на плоскость экрана и элементарных операций с холстом

    private double fi;                          // Расположение коорд. системы
    private double teta;                        // относительно оси проецирования
    private double kX, kY, kZ;                  // Коэффициенты масштабирования по осям (из физических координат в координаты холста)
    private double scrAxeSize;                  // Длина осей в экранных координатах
    private double xMid, yMid;                  // Начало осей координат - середина канваса
    private double x, y, z;                     // Физические размеры по осям

    private double[][] a = new double[3][2];    // Матрица проецирования на плоскость экрана монитора

    public CurrentProjection(double FI, double TETA, double x, double y, double z) {
        initProjectionMatrix(FI, TETA);
        setAxesParams(x, y, z);
    }

    public void setAxesParams(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        updateScaleKoeffs();
    }

    public void setCanvasParams(double scrAxeSize, double xMid, double yMid) {
        this.scrAxeSize = scrAxeSize;
        this.xMid = xMid;
        this.yMid = yMid;
        updateScaleKoeffs();
    }

    public void setFiTeta(double FI, double TETA) {
        initProjectionMatrix(FI, TETA);
    }

    public double getFi() {
        return fi * 180 / Math.PI;
    }

    public double getTeta() {
        return teta * 180 / Math.PI;
    }

    public double getkX() {
        return kX;
    }

    public double getkY() {
        return kY;
    }

    public double getkZ() {
        return kZ;
    }
    
    public void initProjectionMatrix(double FI, double TETA) // Инициализация матрицы проецирования
    {
        this.fi = Math.PI * (FI % 360) / 180.0;
        if (TETA > 90) {
            TETA = 90;
        }
        if (TETA < 0) {
            TETA = 0;
        }
        this.teta = Math.PI * TETA / 180.0;

        a[0][0] = -Math.sin(fi);
        a[0][1] = -Math.sin(teta) * Math.cos(fi);
        a[1][0] = Math.cos(fi);
        a[1][1] = -Math.sin(fi) * Math.sin(teta);
        a[2][0] = 0.0;
        a[2][1] = Math.cos(teta);
    }

    public void updateScaleKoeffs() {
        kX = scrAxeSize / x;
        kY = scrAxeSize / y;
        kZ = scrAxeSize / z;
    }

    public void convertToScreen(Point3D[] pnt3, double[] x, double[] y, int n) // Функция проецирования 3D-точек на плоскость экрана
    {
        for (int i = 0; i < n; i++) {
            x[i] = xMid + a[0][0] * pnt3[i].x * kX + a[1][0] * pnt3[i].y * kY + a[2][0] * pnt3[i].z * kZ;
            y[i] = yMid - (a[0][1] * pnt3[i].x * kX + a[1][1] * pnt3[i].y * kY + a[2][1] * pnt3[i].z * kZ);
        }
    }
}

