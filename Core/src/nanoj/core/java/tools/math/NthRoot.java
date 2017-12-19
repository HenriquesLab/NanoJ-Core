package nanoj.core.java.tools.math;

/**
 * Created by user on 19/01/2015.
 */
public class NthRoot {
    public static double nthRoot(int n, double A) {
        return nthRoot(n, A, .0001);
    }

    public static double nthRoot(int n, double A, double p) {
        if(A < 0) {
            System.err.println("Values < 0");
            return -1;
        } else if(A == 0) {
            return 0;
        }
        double x_prev = A;
        double x = A / n;  //"guessed" value...
        while(Math.abs(x - x_prev) > p) {
            x_prev = x;
            x = ((n - 1.0) * x + A / Math.pow(x, n - 1.0)) / n;
        }
        return x;
    }
}
