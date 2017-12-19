package nanoj.core.java.tools.fit;

import ij.process.ImageProcessor;
import nanoj.core.java.array.ArrayCasting;
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.exception.TooManyEvaluationsException;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.BOBYQAOptimizer;

import java.util.ArrayList;

import static java.lang.Math.*;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 01/05/15
 * Time: 10:37
 * Loosely based on the code of Ji Yu for the Octane project
 */

public abstract class _BaseFit2D_ extends Thread {

    public int MAX_EVAL = 10000;

    //private final PowellOptimizer optimizer = new PowellOptimizer(1e-4, 1e-6);
    //private final SimplexOptimizer optimizer = new SimplexOptimizer(1e-4, 1e-6);
    protected PointValuePair pvp;
    private boolean failed = false;

    protected double backgroundValue = 0;
    public boolean useFixedBackground = true;

    //protected double[] initialParameters, lowerBounds, upperBounds;
    private final ArrayList<Double> initialParameters = new ArrayList<Double>();
    private final ArrayList<Double> lowerBounds = new ArrayList<Double>();
    private final ArrayList<Double> upperBounds = new ArrayList<Double>();

    protected double x0 = 0;
    protected double y0 = 0;
    protected int x0_int = 0;
    protected int y0_int = 0;

    protected int windowRadius = 0;
    protected int windowRadiusMax = 0;
    protected float[] pixels;
    protected int width;
    protected int height;


    /**
     * Assign image data to the module.
     * A copy of the image data is made and further operation will not alter the orginal image.
     *
     * @param ip The image processor that will be analyzed
     */
    public void setImageData(ImageProcessor ip) {
        //backgroundValue = ip.getAutoThreshold();
        ip = ip.convertToFloatProcessor();
        width = ip.getWidth();
        height = ip.getHeight();
        windowRadiusMax = (min(width, height) - 1) / 2;
        if (windowRadius == 0) windowRadius = windowRadiusMax;
        this.pixels = (float[]) ip.getPixels();
    }

    /**
     * Set fitting window size
     *
     * @param size The size of the fitting rectangle is (2 * size + 1)
     */
    public void setWindowSize(int size) {
        windowRadius = min((size-1)/2, windowRadiusMax);
    }

    public void setPositionInitialGuess(double x0, double y0) {
        this.x0 = x0;
        this.y0 = y0;
        this.x0_int = (int) round(x0);
        this.y0_int = (int) round(y0);
    }

    public boolean getFailed() {
        return failed;
    }

    protected void addParameter(double guessValue, double lowerBound, double upperBound) {
        initialParameters.add(guessValue);
        lowerBounds.add(lowerBound);
        upperBounds.add(upperBound);
    }

    protected abstract void prepareInitialGuess();

    @Override public void run() {
        doFit();
    }

    public double[] doFit() {

        prepareInitialGuess();
        assert (initialParameters != null);

        final int xStart = max(x0_int - windowRadius, 0);
        final int yStart = max(y0_int - windowRadius, 0);
        final int xEnd = min(x0_int + windowRadius, width - 1);
        final int yEnd = min(x0_int + windowRadius, height - 1);

        MultivariateFunction func = new MultivariateFunction() {
            @Override
            public double value(double[] point) {
                double v = 0;
                for (int xi = xStart; xi <= xEnd; xi++) {
                    for (int yi = yStart; yi <= yEnd; yi++) {
                        double delta = getFunctionValue(xi, yi, point) - getPixelValue(xi, yi);
                        v += delta * delta;
                    }
                }
                return v;
            }
        };

        try {
            int dim = initialParameters.size();
            BOBYQAOptimizer optimizer = new BOBYQAOptimizer(2*dim+1);

            if (lowerBounds!=null && upperBounds!=null) {
                pvp = optimizer.optimize(
                        new ObjectiveFunction(func),
                        new InitialGuess(ArrayCasting.toArray(initialParameters, 0d)),
                        new MaxEval(MAX_EVAL),
                        new SimpleBounds(ArrayCasting.toArray(lowerBounds, 0d), ArrayCasting.toArray(upperBounds, 0d)),
                        GoalType.MINIMIZE);
            }
            else {
                pvp = optimizer.optimize(
                        new ObjectiveFunction(func),
                        new InitialGuess(ArrayCasting.toArray(initialParameters, 0d)),
                        SimpleBounds.unbounded(dim),
                        new MaxEval(MAX_EVAL),
                        GoalType.MINIMIZE);
            }
            return pvp.getPoint();
        }
        catch (TooManyEvaluationsException e) {
            failed = true;
            return null;
        }
        catch (MathIllegalStateException e) {
            failed = true;
            return null;
        }
        catch (Exception e) {
            e.printStackTrace();
            failed = true;
            return null;
        }
    }

    /**
     * Get the pixel value of the image
     *
     * @param xi X coordinate
     * @param yi Y coordinate
     * @return The pixel value in double
     */
    protected double getPixelValue(int xi, int yi) {
        return pixels[xi + yi * width];
    }

    /**
     * The value of the Gaussian fitting function at specified coordinate
     *
     * @param xi    The X coordinate offset from the initial value
     * @param yi    The Y coordinate offset from the initial value
     * @param point The rest of the fitting parameters
     * @return The Gaussian function value
     */
    protected abstract double getFunctionValue(int xi, int yi, double[] point);

    /**
     * Substract the value of the last fitting from the image
     */
    protected void deflate() {
        for (int xi = -windowRadius; xi <= windowRadius; xi++) {
            for (int yi = -windowRadius; yi <= windowRadius; yi++) {
                pixels[xi + width * yi] -= getFunctionValue(xi, yi, pvp.getPoint());
            }
        }
    }


    /**
     * Get the fitting result
     *
     * @return Log Likelyhood of the fitting. Bigger value indicate higher confidence in fitting results.
     */
    public double getLogLikelyhood() {
        double m = 0;
        double m2 = 0;
        for (int xi = -windowRadius; xi <= windowRadius; xi++) {
            for (int yi = -windowRadius; yi <= windowRadius; yi++) {
                double v = getPixelValue(xi, yi);
                m += v;
                m2 += v * v;
            }
        }

        int nPixels = (1 + 2 * windowRadius) * (1 + 2 * windowRadius);
        m = m2 - m * m / nPixels; //variance of the grey values

        return nPixels * log(m / pvp.getValue());
    }
}
