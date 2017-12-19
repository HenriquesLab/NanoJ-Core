package nanoj.core.java.tools.fit;

import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Wand;
import ij.process.EllipseFitter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import nanoj.core.java.array.ArrayMath;
import nanoj.core.java.featureExtraction.Peaks;

import static java.lang.Math.PI;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 08/05/15
 * Time: 16:28
 */
public class EllipsoidFit {

    public static EllipseFitter getEllipseByBinaryEllipsoidFit(FloatProcessor ip) {

        //ip = ip.convertToFloat();
        ArrayMath.normalize((float[]) ip.getPixels(), 1);

        float[] maxs = Peaks.getMax(ip, null);
        int xMax = (int) maxs[1];
        int yMax = (int) maxs[2];

        //ip.autoThreshold();
        Wand w = new Wand(ip);
        w.autoOutline(xMax, yMax, 0.25, 1.0);
        //w.autoOutline(xMax, yMax);
        Roi roi = new PolygonRoi(w.xpoints, w.ypoints, w.npoints, Roi.FREEROI); // Roi.FREEROI:Roi.TRACED_ROI
        ip.setRoi(roi);
        EllipseFitter ef = new EllipseFitter();
        ef.fit(ip, null);
        ef.makeRoi(ip);
        return ef;
    }

    public static float[] getShiftAndTiltByBinaryEllipsoidFit(FloatProcessor ip) {

        EllipseFitter ef = getEllipseByBinaryEllipsoidFit(ip);
        float centerX = ip.getWidth() / 2f;
        float centerY = ip.getHeight() / 2f;

        float shiftX = (float) -(ef.xCenter - centerX);
        float shiftY = (float) -(ef.yCenter - centerY);
        float angle = (float) -(ef.theta - PI/2f);

        //System.out.println("dx=" + shiftX + " dy=" + shiftY + " angle=" + angle+" major="+ef.major+" minor"+ef.minor);
        return new float[] {shiftX, shiftY, angle, (float) ef.major, (float) ef.minor};
    }

    public static float[] getShiftAndTiltByEllipsoidFit(FloatProcessor ip) {

        float centerX = ip.getWidth() / 2f;
        float centerY = ip.getHeight() / 2f;

        EllipseFitter ef = getEllipseByBinaryEllipsoidFit(ip);

        GaussianEllipsoidFitWithAngle2D gef = new GaussianEllipsoidFitWithAngle2D();
        gef.setImageData(ip);
        gef.setPositionInitialGuess(ef.xCenter, ef.yCenter);
        gef.setSigmaInitialGuess(ef.major, ef.minor, false);
        gef.setAngleInitialGuess(ef.theta);
        gef.useFixedBackground = false;
        gef.doFit();

        float shiftX, shiftY, angle, major, minor;

        if (gef.getFailed()) {
            System.out.println("failed");
            shiftX = (float) -(ef.xCenter - centerX);
            shiftY = (float) -(ef.yCenter - centerY);
            angle = (float) -(ef.theta - PI/2f);
            major = (float) ef.major;
            minor = (float) ef.minor;
        }
        else {
            shiftX = (float) -(gef.getX() - centerX);
            shiftY = (float) -(gef.getY() - centerY);
            angle = (float) -(gef.getAngle());
            major = (float) gef.getSigmaX();
            minor = (float) gef.getSigmaY();
        }

        //System.out.println("major="+major+" minor="+minor);

        return new float[] {shiftX, shiftY, angle, major, minor};
    }

    public static float[][] getShiftAndTiltByBinaryEllipsoidFit(ImageStack ims) {
        int nSlices = ims.getSize();

        float[] shiftX = new float[nSlices];
        float[] shiftY = new float[nSlices];
        float[] angle = new float[nSlices];
        float[] major = new float[nSlices];
        float[] minor = new float[nSlices];

        for (int s = 0; s < nSlices; s++) {
            ImageProcessor ip = ims.getProcessor(s + 1);
            float[] values = getShiftAndTiltByBinaryEllipsoidFit(ip.convertToFloatProcessor());
            shiftX[s] = values[0];
            shiftY[s] = values[1];
            angle[s] = values[2];
            major[s] = values[3];
            minor[s] = values[4];
            //System.out.println("dx=" + shiftX[s] + " dy=" + shiftY[s] + " angle=" + toDegrees(angle[s]));
        }
        return new float[][] {shiftX, shiftY, angle, major, minor};
    }

    public static float[][] getShiftAndTiltByEllipsoidFit(ImageStack ims) {
        int nSlices = ims.getSize();

        float[] shiftX = new float[nSlices];
        float[] shiftY = new float[nSlices];
        float[] angle = new float[nSlices];

        for (int s = 0; s < nSlices; s++) {
            ImageProcessor ip = ims.getProcessor(s + 1);
            float[] values = getShiftAndTiltByEllipsoidFit(ip.convertToFloatProcessor());
            shiftX[s] = values[0];
            shiftY[s] = values[1];
            angle[s] = values[2];
            //System.out.println("dx=" + shiftX[s] + " dy=" + shiftY[s] + " angle=" + toDegrees(angle[s]));
        }
        return new float[][] {shiftX, shiftY, angle};
    }
}
