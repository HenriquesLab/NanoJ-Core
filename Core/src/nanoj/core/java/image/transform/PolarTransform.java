package nanoj.core.java.image.transform;

import ij.process.FloatProcessor;

import static java.lang.Math.*;

/**
 * Created by paxcalpt on 19/03/2016.
 */
public class PolarTransform {

    private static ZeroFillWithInterpolation ZFWI = new ZeroFillWithInterpolation();

    public static FloatProcessor polarTransform(FloatProcessor ip) {
        int w = ip.getWidth();
        int h = ip.getHeight();

        int radius = round(Math.max(w, h) / 2f);
        if (radius % 2 != 0) radius++;
        //if (radius<180) radius = 180;
        int angles = radius;

        return polarTransform(ip, radius, angles, true);
    }

    public static FloatProcessor polarTransform(FloatProcessor ip, int radius, int angles, boolean zeroFill) {
        int w = ip.getWidth();
        int h = ip.getHeight();

        ip.setInterpolationMethod(ip.BICUBIC);

        FloatProcessor ipLogPolar = new FloatProcessor(radius, angles);

        double cy = round(h/2d);
        double cx = round(w/2d);

        for (int r=0; r<radius; r++) {
            for (int a=0; a<angles; a++) {
                double angle = (((double) a)/angles)*2*Math.PI;
                double x = (cx + r*cos(angle));
                double y = (cy + r*sin(angle));
                float v = (float) ip.getInterpolatedPixel(x, y);
                ipLogPolar.setf(r, a, v);
            }
        }

        if (zeroFill) {
            return ZFWI.calculate(ipLogPolar, 10);
        }

        return ipLogPolar;
    }
}
