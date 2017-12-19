package nanoj.core.java.tools.fit;

import static java.lang.Math.*;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 01/05/15
 * Time: 10:55
 *
 * Loosely based on https://valelab.ucsf.edu/svn/micromanager2/tags/trunk/plugins/Gaussian/source/edu/valelab/GaussianFit/GaussianFit.java
 */
public class GaussianEllipsoidFitWithAngle2D extends _BaseFit2D_ {

    protected double sigmaX0 = 1;
    protected double sigmaY0 = 1;
    protected double angle0 = 0;
    private boolean useFixedSigma = false;

    private int IDX_A = -1;
    private int IDX_X0 = -1;
    private int IDX_Y0 = -1;
    private int IDX_THETA = -1;
    private int IDX_SX0 = -1;
    private int IDX_SY0 = -1;
    private int IDX_BG = -1;

    public void setSigmaInitialGuess(double sigmaX, double sigmaY, boolean fixed) {
        this.sigmaX0 = sigmaX;
        this.sigmaY0 = sigmaY;
        this.useFixedSigma = fixed;
    }

    public void setAngleInitialGuess(double angle) {
        this.angle0 = angle;
    }

    @Override protected void prepareInitialGuess() {
        double amplitude0 = getPixelValue((int) round(x0), (int) round(y0)) - backgroundValue;

        int counter = 0;

        addParameter(amplitude0, 0, 9999999); IDX_A = counter; counter++;
        addParameter(x0, 0, width); IDX_X0 = counter; counter++;
        addParameter(y0, 0, height); IDX_Y0 = counter; counter++;
        addParameter(angle0, -PI, PI); IDX_THETA = counter; counter++;

        if (!useFixedSigma) {
            addParameter(sigmaX0, 1, width / 2); IDX_SX0 = counter; counter++;
            addParameter(sigmaY0, 1, height / 2); IDX_SY0 = counter; counter++;
        }
        if (!useFixedBackground) {
            addParameter(backgroundValue, 0, 9999999); IDX_BG = counter; counter++;
        }
    }
    
    @Override protected double getFunctionValue(int xi, int yi, double[] p) {

        double A = p[IDX_A];
        double x0 = p[IDX_X0];
        double y0 = p[IDX_Y0];
        double theta = p[IDX_THETA];

        double sigmaX22, sigmaY22;
        if (!useFixedSigma) {
            sigmaX22 = 2*(p[IDX_SX0]*p[IDX_SX0]);
            sigmaY22 = 2*(p[IDX_SY0]*p[IDX_SY0]);
        }
        else {
            sigmaX22 = 2*sigmaX0*sigmaX0;
            sigmaY22 = 2*sigmaY0*sigmaY0;
        }

        double background = useFixedBackground? backgroundValue: p[IDX_BG];

        // f =  A * e^(-(a*(x-xc)^2 + c*(y-yc)^2 + 2*b*(x-xc)*(y-yc))) + B
        // a = cos(theta)^2/2/sigma_x^2 + sin(theta)^2/2/sigma_y^2;
        // b = -sin(2*theta)/4/sigma_x^2 + sin(2*theta)/4/sigma_y^2 ;
        // c = sin(theta)^2/2/sigma_x^2 + cos(theta)^2/2/sigma_y^2;

        double cosAngle = cos(theta);
        double sinAngle = sin(theta);
        double cosAngle2 = cosAngle * cosAngle;
        double sinAngle2 = sinAngle * sinAngle;
        double sin2angle = sin(2 * theta);
        double a = cosAngle2 / sigmaX22 + sinAngle2 / sigmaY22;
        double b = -sin2angle / (2 * sigmaX22) + sin2angle / (2 * sigmaY22);
        double c = sinAngle2 / sigmaX22 + cosAngle2 / sigmaY22;

        //a = 1/sigmaX22; b=0; c=1/sigmaY22;
        return A * exp(-(a*pow(xi-x0, 2) + 2*b*(xi-x0)*(yi - y0) + c*pow(yi-y0, 2))) + background;
    }

    public double getA() {
        return pvp.getPoint()[IDX_A];
    }

    public double getX() {
        return pvp.getPoint()[IDX_X0];
    }

    public double getY() {
        return pvp.getPoint()[IDX_Y0];
    }

    public double getSigmaX() {
        if (useFixedSigma) return sigmaX0;
        return abs(pvp.getPoint()[IDX_SX0]);
    }

    public double getSigmaY() {
        if (useFixedSigma) return sigmaY0;
        return abs(pvp.getPoint()[IDX_SY0]);
    }

    public double getAngle() {
        return pvp.getPoint()[IDX_THETA];
    }
}
