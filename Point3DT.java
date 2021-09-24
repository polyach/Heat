package advancedmedia;

public class Point3DT extends Point3D {

    public double T;

    public Point3DT() {
        super();
        this.T = 0;
    }

    public Point3DT(double x, double y, double z, double T) {
        super(x, y, z);
        this.T = T;
    }

    @Override
    public int compareTo(Point3D t) {
        Point3DT p = (Point3DT) t;
        if (T < p.T) {
            return -1;
        } else if (T > p.T) {
            return 1;
        }
        return 0;
    }
}
