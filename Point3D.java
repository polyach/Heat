package advancedmedia;

public class Point3D implements Comparable<Point3D> {

        public double x, y, z;

        public Point3D() {
            this.x = this.y = this.z = 0.;
        }

        public Point3D(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public int compareTo(Point3D t) {
            if (z < t.z) {
                return -1;
            } else if (z > t.z) {
                return 1;
            }
            return 0;
        }
    }