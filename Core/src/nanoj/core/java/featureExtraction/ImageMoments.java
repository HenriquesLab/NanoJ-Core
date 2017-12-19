package nanoj.core.java.featureExtraction;

import ij.process.FloatProcessor;

import static java.lang.StrictMath.atan;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 01/01/16
 * Time: 14:27
 */

/**
 * Based on Hu Moments (examples described in https://en.wikipedia.org/wiki/Image_moment#Examples_2)
 */
public class ImageMoments {

    private double M00 = 0;
    private double M10 = 0;
    private double M01 = 0;
    private double M11 = 0;
    private double M20 = 0;
    private double M02 = 0;
    public final double CX;
    public final double CY;
    public final double angle;

    public ImageMoments(FloatProcessor fp){
        for (int y=0; y<fp.getHeight(); y++) {
            for (int x=0; x<fp.getWidth(); x++) {
                float v = fp.getf(x, y);
                M00 += v;
                M10 += x * v;
                M01 += y * v;
                M11 += x * y * v;
                M20 += x * x * v;
                M02 += y * y * v;
            }
        }

        CX = M10 / M00;
        CY = M01 / M00;

        double CX2 = CX*CX;
        double CY2 = CY*CY;
        double _u20 = M20/M00-CX2;
        double _u02 = M02/M00-CY2;
        double _u11 = M11/M00-CX*CY;
        if (_u20 !=_u02) angle = 0.5 * atan(2*_u11 / (_u20-_u02));
        else angle = 0;
    }
}
