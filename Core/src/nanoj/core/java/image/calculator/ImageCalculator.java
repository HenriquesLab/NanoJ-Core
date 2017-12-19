package nanoj.core.java.image.calculator;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import nanoj.core.java.array.ArrayMath;

import java.awt.*;

import static java.lang.Math.*;
import static nanoj.core.java.array.ArrayMath.*;
import static nanoj.core.java.array.ImageStackToFromArray.*;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 06/01/15
 * Time: 20:33
 */
public class ImageCalculator {
    public static ImagePlus add(ImagePlus imp1, ImagePlus imp2){
        float[] data1 = ImagePlusToFloatArray(imp1);
        float[] data2 = ImagePlusToFloatArray(imp2);
        assert (data1.length == data2.length);
        for (int n=0; n<data1.length;n++)
            data1[n]+=data2[n];
        ImagePlus imp = ImagePlusFromFloatArray(data1, imp1.getWidth(), imp1.getHeight());
        return imp;
    }

    public static ImageStack add(ImageStack ims1, ImageStack ims2){
        float[] data1 = ImageStackToFloatArray(ims1);
        float[] data2 = ImageStackToFloatArray(ims2);
        assert (data1.length == data2.length);
        for (int n=0; n<data1.length;n++)
            data1[n]+=data2[n];
        ImageStack ims = ImageStackFromFloatArray(data1, ims1.getWidth(), ims1.getHeight());
        return ims;
    }

    public static ImagePlus subtract(ImagePlus imp1, ImagePlus imp2){
        return new ImagePlus("", subtract(imp1.getImageStack(), imp2.getImageStack()));
    }

    public static ImageStack subtract(ImageStack ims1, ImageStack ims2){
        float[] data1 = ImageStackToFloatArray(ims1);
        float[] data2 = ImageStackToFloatArray(ims2);
        assert (data1.length == data2.length);
        for (int n=0; n<data1.length;n++)
            data1[n]-=data2[n];
        ImageStack ims = ImageStackFromFloatArray(data1, ims1.getWidth(), ims1.getHeight());
        return ims;
    }

    public static FloatProcessor difference(FloatProcessor ip1, FloatProcessor ip2){
        FloatProcessor ip = (FloatProcessor) ip1.duplicate();
        float[] pixels1 = (float[]) ip.getPixels();
        float[] pixels2 = (float[]) ip2.getPixels();
        for (int n=0; n<pixels1.length;n++) {
            pixels1[n] = abs(pixels1[n] - pixels2[n]);
        }
        ip.setPixels(pixels1);
        return ip;
    }

    public static ImagePlus divide(ImagePlus imp1, ImagePlus imp2){
        float[] data1 = ImagePlusToFloatArray(imp1);
        float[] data2 = ImagePlusToFloatArray(imp2);
        assert (data1.length == data2.length);
        for (int n=0; n<data1.length;n++)
            data1[n]/=data2[n];
        ImagePlus imp = ImagePlusFromFloatArray(data1, imp1.getWidth(), imp1.getHeight());
        return imp;
    }

    public static ImagePlus multiply(ImagePlus imp1, ImagePlus imp2){
        float[] data1 = ImagePlusToFloatArray(imp1);
        float[] data2 = ImagePlusToFloatArray(imp2);
        assert (data1.length == data2.length);
        for (int n=0; n<data1.length;n++)
            data1[n]*=data2[n];
        ImagePlus imp = ImagePlusFromFloatArray(data1, imp1.getWidth(), imp1.getHeight());
        return imp;
    }

    public static ImageStack multiply(ImageStack ims1, ImageStack ims2){
        float[] data1 = ImageStackToFloatArray(ims1);
        float[] data2 = ImageStackToFloatArray(ims2);
        assert (data1.length == data2.length);
        for (int n=0; n<data1.length;n++)
            data1[n]*=data2[n];
        return ImageStackFromFloatArray(data1, ims1.getWidth(), ims1.getHeight());
    }

    public static ImagePlus multiply(ImagePlus imp, float v){
        float[] data = ImagePlusToFloatArray(imp);
        for (int n=0; n<data.length;n++)
            data[n]*=v;
        return ImagePlusFromFloatArray(data, imp.getWidth(), imp.getHeight());
    }

    public static ImageStack multiply(ImageStack ims, float v){
        float[] data = ImageStackToFloatArray(ims);
        for (int n=0; n<data.length;n++)
            data[n]*=v;
        return ImageStackFromFloatArray(data, ims.getWidth(), ims.getHeight());
    }

    public static ImagePlus square(ImagePlus imp){
        float[] data = ImagePlusToFloatArray(imp);
        for (int n=0; n<data.length;n++)
            data[n]*=data[n];
        return ImagePlusFromFloatArray(data, imp.getWidth(), imp.getHeight());
    }

    public static ImageStack square(ImageStack ims) {
        float[] data = ImageStackToFloatArray(ims);
        for (int n=0; n<data.length;n++)
            data[n]*=data[n];
        return ImageStackFromFloatArray(data, ims.getWidth(), ims.getHeight());
    }

    public static float meanValue(ImageStack ims){
        float[] data = ImageStackToFloatArray(ims);
        float mean = ArrayMath.getAverageValue(data);
        return mean;
    }

    public static void normalize(FloatProcessor ip) {
        ArrayMath.normalize((float[]) ip.getPixels(), 1);
    }

    public static ImageStack normalize(ImageStack ims) {
        ims = ims.convertToFloat();
        for (int n=1; n<=ims.getSize(); n++) {
            ArrayMath.normalize((float[]) ims.getProcessor(n).getPixels(), 1);
        }
        return ims;
    }

    public static void normalizeIntegratedIntensity(FloatProcessor ip) {
        ArrayMath.normalizeIntegratedIntensity((float[]) ip.getPixels(), 1);
    }

    public static float maxToMeanRatio(ImageProcessor ip) {
        float v, vMax = -9999999, vMean = 0;
        for (int n=0; n<ip.getPixelCount(); n++) {
            v = ip.getf(n);
            vMean += v;
            vMax = max(v, vMax);
        }
        return vMax / (vMean / ip.getPixelCount());
    }

    public static float meanSquareError(ImageProcessor ipReference, ImageProcessor ipComparison) {
        double mse = 0;
        float[] pixelsReference = (float[]) ipReference.getPixels();
        float[] pixelsComparison = (float[]) ipComparison.getPixels();
        for (int p=0; p<pixelsReference.length; p++) {
            mse += pow(pixelsReference[p] - pixelsComparison[p], 2) / pixelsReference.length;
        }
        return (float) mse;
    }

    public static FloatProcessor meanSquareError(ImageProcessor ipReference, ImageStack ims) {
        FloatProcessor ipMSE = new FloatProcessor(ipReference.getWidth(), ipReference.getHeight());
        int nSlices = ims.getSize();

        for (int s=1; s<=nSlices; s++) {
            ImageProcessor ip = ims.getProcessor(s);
            for (int p=0; p<ipMSE.getPixelCount(); p++) {
                float mse = (float) pow(ip.getf(p) - ipReference.getf(p), 2) / nSlices;
                ipMSE.setf(p, ipMSE.getf(p) + mse);
            }
        }
        return ipMSE;
    }

    public static void meanSubtract(FloatProcessor fp) {
        float[] pixels = (float[]) fp.getPixels();
        addWithReplace(pixels, -getAverageValue(pixels));
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // ImageStack calculations
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~
    public static void sqrt(ImageStack ims) {
        for (int s=1; s<=ims.getSize(); s++)
            ims.getProcessor(s).sqrt();
    }

    public static void sqr(ImageStack ims) {
        for (int s=1; s<=ims.getSize(); s++)
            ims.getProcessor(s).sqr();
    }

    public static void blurGaussian(ImageStack ims, double sigma) {
        for (int s=1; s<=ims.getSize(); s++)
            ims.getProcessor(s).blurGaussian(sigma);
    }

    public static float[] fractionZeros(ImageStack ims) {

        float[] fZeros = new float[ims.getSize()];
        for (int n=1; n<=ims.getSize();n++){

            float z = 0;
            float[] pixels = (float[])ims.getProcessor(n).getPixels();
            for(int p=0;p<pixels.length;p++){
                if(pixels[p]==0) {z+=1;}
            }
            fZeros[n-1] = z/(float)(pixels.length);

        }
        return fZeros;
    }



    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // ImageProcessor calculations
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~
    public static ImageProcessor add(ImageProcessor ip1, ImageProcessor ip2){
        ImageProcessor ip = ip1.duplicate();
        for (int n=0; n<ip.getPixelCount();n++) ip.setf(n, ip1.getf(n) + ip2.getf(n));
        return ip;
    }

    public static ImageProcessor subtract(ImageProcessor ip1, ImageProcessor ip2){
        ImageProcessor ip = ip1.duplicate();
        for (int n=0; n<ip.getPixelCount();n++) ip.setf(n, ip1.getf(n) - ip2.getf(n));
        return ip;
    }

    public static ImageProcessor multiply(ImageProcessor ip1, ImageProcessor ip2){
        ImageProcessor ip = ip1.duplicate();
        for (int n=0; n<ip.getPixelCount();n++) ip.setf(n, ip1.getf(n) * ip2.getf(n));
        return ip;
    }

    public static ImageProcessor divide(ImageProcessor ip1, ImageProcessor ip2){
        ImageProcessor ip = ip1.duplicate();
        for (int n=0; n<ip.getPixelCount();n++) ip.setf(n, ip1.getf(n) / ip2.getf(n));
        return ip;
    }

    public static ImageProcessor flip(ImageProcessor ip) {
        ImageProcessor ipNew = ip.duplicate();
        int w = ip.getWidth()-1;
        int h = ip.getHeight()-1;
        for (int i=0; i<=0; i++) {
            for (int j=0; j<=0; j++) {
                ipNew.setf(i, j, ip.getf(w-i, h-j));
            }
        }
        return ipNew;
    }

    public static ImageProcessor subtractBlur(ImageProcessor ip, double sigma) {
        ImageProcessor ipBlur = ip.duplicate();
        ipBlur.blurGaussian(sigma);
        return subtract(ip, ipBlur);
    }

    public static Point getMaxPosition(ImageProcessor ip) {

        float[] pixels = (float[])((float[])ip.getPixels());
        float[] maxValues = getMaxValue(pixels);
        int pMax = (int) maxValues[0];

        int y = pMax / ip.getWidth();
        int x = pMax % ip.getWidth();
        return new Point(x, y);
    }


}
