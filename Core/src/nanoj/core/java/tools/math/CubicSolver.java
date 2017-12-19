package nanoj.core.java.tools.math;

/**
 * Created by sculley on 14/04/15.
 */
public class CubicSolver {

    /**
     * Class Cubic solves for the real roots of a cubic equation with real
     * coefficients. The cubic equation is of the form
     * ax^3 + bx^2 + cx + d = 0
     * To solve a cubic equation, construct an instance of class Cubic; call the
     * Cubic object's solve() method, passing in the coefficients a, b,
     * c and d; and obtain the roots from the Cubic
     * object's fields. The number of (real) roots, either 1 or 3, is stored in
     * field nRoots. If there is one root, it is stored in field
     * x1, and fields x2 and x3 are set to NaN. If there
     * are three roots, they are stored in fields x1, x2, and
     * x3 in descending order.
     * The same Cubic object may be used to solve several cubic equations. Each time
     * the solve() method is called, the solution is stored in the Cubic
     * object's fields.
     * The formulas for the roots of a cubic equation come from:
     * E. Weisstein. "Cubic formula." From MathWorld--A Wolfram Web Resource.
     * http://mathworld.wolfram.com/CubicFormula.html
     *
     * @author  Alan Kaminsky
     * @version 02-Feb-2008
     */

// Hidden constants.

        private static final double TWO_PI = 2.0 * Math.PI;
        private static final double FOUR_PI = 4.0 * Math.PI;

// Exported fields.

        /**
         * The number of real roots.
         */
        public int nRoots;

        /**
         * The first real root.
         */
        public double x1;

        /**
         * The second real root.
         */
        public double x2;

        /**
         * The third real root.
         */
        public double x3;

// Exported constructors.

        /**
         * Construct a new Cubic object.
         */
        public CubicSolver()
        {
        }

// Exported operations.

        /**
         * Solve the cubic equation with the given coefficients. The results are
         * stored in this Cubic object's fields.
         *
         * @param  a  Coefficient of <I>x</I><SUP>3</SUP>.
         * @param  b  Coefficient of <I>x</I><SUP>2</SUP>.
         * @param  c  Coefficient of <I>x</I>.
         * @param  d  Constant coefficient.
         *
//         * @exception  DomainException
//         *     (unchecked exception) Thrown if <TT>a</TT> is 0; in other words, the
//         *     coefficients do not represent a cubic equation.
         */
        public void solve
        (double a,
         double b,
         double c,
         double d)
        {
            // Verify preconditions.
//            if (a == 0.0)
//            {
//                throw new DomainException ("Cubic.solve(): a = 0");
//            }

            // Normalize coefficients.
            double denom = a;
            a = b/denom;
            b = c/denom;
            c = d/denom;

            // Commence solution.
            double a_over_3 = a / 3.0;
            double Q = (3*b - a*a) / 9.0;
            double Q_CUBE = Q*Q*Q;
            double R = (9*a*b - 27*c - 2*a*a*a) / 54.0;
            double R_SQR = R*R;
            double D = Q_CUBE + R_SQR;

            if (D < 0.0)
            {
                // Three unequal real roots.
                nRoots = 3;
                double theta = Math.acos (R / Math.sqrt (-Q_CUBE));
                double SQRT_Q = Math.sqrt (-Q);
                x1 = 2.0 * SQRT_Q * Math.cos (theta/3.0) - a_over_3;
                x2 = 2.0 * SQRT_Q * Math.cos ((theta+TWO_PI)/3.0) - a_over_3;
                x3 = 2.0 * SQRT_Q * Math.cos ((theta+FOUR_PI)/3.0) - a_over_3;
                sortRoots();
            }
            else if (D > 0.0)
            {
                // One real root.
                nRoots = 1;
                double SQRT_D = Math.sqrt (D);
                double S = Math.cbrt (R + SQRT_D);
                double T = Math.cbrt (R - SQRT_D);
                x1 = (S + T) - a_over_3;
                x2 = Double.NaN;
                x3 = Double.NaN;
            }
            else
            {
                // Three real roots, at least two equal.
                nRoots = 3;
                double CBRT_R = Math.cbrt (R);
                x1 = 2*CBRT_R - a_over_3;
                x2 = x3 = CBRT_R - a_over_3;
                sortRoots();
            }
        }

// Hidden operations.

        /**
         * Sort the roots into descending order.
         */
        private void sortRoots()
        {
            if (x1 < x2)
            {
                double tmp = x1; x1 = x2; x2 = tmp;
            }
            if (x2 < x3)
            {
                double tmp = x2; x2 = x3; x3 = tmp;
            }
            if (x1 < x2)
            {
                double tmp = x1; x1 = x2; x2 = tmp;
            }
        }

// Unit test main program.

        /**
         * Unit test main program.
         * Usage: java edu.rit.numeric.Cubic a b c d
         */
        public static void main
        (String[] args)
                throws Exception
        {
            if (args.length != 4) usage();
            double a = Double.parseDouble (args[0]);
            double b = Double.parseDouble (args[1]);
            double c = Double.parseDouble (args[2]);
            double d = Double.parseDouble (args[3]);
            CubicSolver cubic = new CubicSolver();
            cubic.solve (a, b, c, d);
            System.out.println ("x1 = " + cubic.x1);
            if (cubic.nRoots == 3)
            {
                System.out.println ("x2 = " + cubic.x2);
                System.out.println ("x3 = " + cubic.x3);
            }
        }

        /**
         * Print a usage message and exit.
         */
        private static void usage()
        {
            System.err.println ("Usage: java edu.rit.numeric.Cubic <a> <b> <c> <d>");
            System.err.println ("Solves ax^3 + bx^2 + cx + d = 0");
            System.exit (1);
        }

    }
