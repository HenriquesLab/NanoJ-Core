package nanoj.core.java.image.calculator;

import ij.process.FloatProcessor;
import nanoj.core.java.array.ArrayMath;

import static java.lang.Math.abs;
import static java.lang.Math.pow;

/**
 * Created by Henriques-lab on 17/06/2016.
 */
public class FloatProcessorCalculator {

    public static FloatProcessor add(FloatProcessor fp1, FloatProcessor fp2){
        float[] p1 = (float[]) fp1.getPixels();
        float[] p2 = (float[]) fp2.getPixels();
        return new FloatProcessor(fp1.getWidth(), fp1.getHeight(), ArrayMath.add(p1, p2));
    }

    public static FloatProcessor subtract(FloatProcessor fp1, FloatProcessor fp2){
        float[] p1 = (float[]) fp1.getPixels();
        float[] p2 = (float[]) fp2.getPixels();
        return new FloatProcessor(fp1.getWidth(), fp1.getHeight(), ArrayMath.subtract(p1, p2));
    }

    public static FloatProcessor multiply(FloatProcessor fp1, FloatProcessor fp2){
        float[] p1 = (float[]) fp1.getPixels();
        float[] p2 = (float[]) fp2.getPixels();
        return new FloatProcessor(fp1.getWidth(), fp1.getHeight(), ArrayMath.multiply(p1, p2));
    }

    public static FloatProcessor divide(FloatProcessor fp1, FloatProcessor fp2){
        float[] p1 = (float[]) fp1.getPixels();
        float[] p2 = (float[]) fp2.getPixels();
        return new FloatProcessor(fp1.getWidth(), fp1.getHeight(), ArrayMath.divideWithZeroDivisionCheck(p1, p2));
    }

    public static FloatProcessor squaredDifference(FloatProcessor fp1, FloatProcessor fp2){
        FloatProcessor fp = new FloatProcessor(fp1.getWidth(), fp1.getHeight());
        for (int n=0; n<fp.getPixelCount(); n++) {
            fp.setf(n, (float) pow(fp1.getf(n)-fp2.getf(n), 2));
        }
        return fp;
    }

    public static FloatProcessor absoluteDifference(FloatProcessor fp1, FloatProcessor fp2){
        FloatProcessor fp = new FloatProcessor(fp1.getWidth(), fp1.getHeight());
        for (int n=0; n<fp.getPixelCount(); n++) {
            fp.setf(n, abs(fp1.getf(n)-fp2.getf(n)));
        }
        return fp;
    }

    public static FloatProcessor invert(FloatProcessor fp1){
        FloatProcessor fp = new FloatProcessor(fp1.getWidth(), fp1.getHeight());
        for (int n=0; n<fp.getPixelCount(); n++) {
            fp.setf(n, 1 / fp1.getf(n));
        }
        return fp;
    }
}
