package nanoj.core2;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

import java.util.Random;

import static nanoj.core.java.tools.math.LogFactorial.logFactorial;

/**
 * Created by sculley on 19/04/2018.
 */
public class NanoJRandomNoise {

    public static Random random = new Random();

    public static int poissonValue(double mean) {
        if (mean<100){
            return poissonSmall(mean);
        }
        else{
            return poissonLarge(mean);
        }
    }

    private static int poissonLarge(double mean) {
        // "Rejection method PA" from "The Computer Generation of
        // Poisson Random Variables" by A. C. Atkinson,
        // Journal of the Royal Statistical Society Series C
        // (Applied Statistics) Vol. 28, No. 1. (1979)
        // The article is on pages 29-35.
        // The algorithm given here is on page 32.

        double c = 0.767 - 3.36/mean;
        double beta = Math.PI/Math.sqrt(3.0 * mean);
        double alpha = beta*mean;
        double k = Math.log(c) - mean - Math.log(beta);

        while (true) {
            double u = random.nextDouble();
            double x = (alpha - Math.log((1.0 - u) / u))/beta;
            int n = (int) Math.floor(x + 0.5);
            if (n < 0)
                continue;
            double v = random.nextDouble();
            double y = alpha - beta*x;
            double temp = 1.0 + Math.exp(y);
            double lhs = y + Math.log(v / (temp * temp));
            double rhs = k + n*Math.log(mean) - logFactorial(n);
            if (lhs <= rhs)
                return n;
        }
    }

    private static int poissonSmall(double mean) {
        double L = Math.exp(-mean);
        double p = 1.0;
        int k = 0;

        do {
            k++;
            p *= Math.random();
        } while (p > L);

        return k - 1;
    }

    public static double gaussianValue(double mean, double sigma)
    {
        return random.nextGaussian()*sigma+mean;
    }

    public static void addMixedGaussianPoissonNoise(String gaussSigma, String gaussMean) {
        addMixedGaussianPoissonNoise(Double.parseDouble(gaussSigma), Double.parseDouble(gaussMean));
    }

    public static void addMixedGaussianPoissonNoise(double gaussSigma, double gaussMean){
        ImagePlus imp = IJ.getImage();
        addMixedGaussianPoissonNoise(imp, gaussSigma, gaussMean);
    }

    public static void addMixedGaussianPoissonNoise(ImagePlus imp, double gaussSigma, double gaussMean)
    {
        ImageProcessor ip;
        for (int n=0; n<imp.getNSlices(); n++)
        {
            imp.setSlice(n+1);
            ip = imp.getProcessor();
            addMixedGaussianPoissonNoise(ip, gaussSigma, gaussMean);
            imp.setProcessor(ip);
        }
    }

    public static void addMixedGaussianPoissonNoise(ImageProcessor ip, double gaussSigma, double gaussMean)
    {
        int npixels = ip.getHeight()*ip.getWidth();
        float v;
        for (int p=0; p<npixels; p++)
        {
            v = ip.getf(p);
            v = (float) gaussianValue(gaussMean, gaussSigma) + poissonValue(v);
            v = Math.max(v, 0);
            v = Math.min(v, 65535);
            ip.setf(p, v);
        }
    }

    public static void addGaussianNoise(ImagePlus imp, double gaussSigma, double gaussMean)
    {
        ImageProcessor ip;
        for (int n=0; n<imp.getNSlices(); n++)
        {
            imp.setSlice(n+1);
            ip = imp.getProcessor();
            addGaussianNoise(ip, gaussSigma, gaussMean);
            imp.setProcessor(ip);
        }
    }

    public static void addGaussianNoise(ImageProcessor ip, double gaussSigma, double gaussMean)
    {
        int npixels = ip.getHeight()*ip.getWidth();
        float v;
        for (int p=0; p<npixels; p++)
        {
            v = ip.getf(p);
            v = (float) gaussianValue(gaussMean, gaussSigma) + v;
            v = Math.max(v, 0);
            v = Math.min(v, 65535);
            ip.setf(p, v);
        }
    }

    public static void addsCMOSNoise(ImagePlus imp, ImagePlus sCMOSFPN){
        /**
         * Takes a given sCMOS Fixed Pattern Noise (FPN) and adds Possion Shot noise and sCMOS FPN
         * to the ImagePlus provided which contains the detected photon number per pixel
         *
         * ImagePlus sCMOSFPN must be a 3 frame stack - Frame 1: Offset. Frame2: Variance. Frame3: Gain.
         * as provided by CharacterisePixels_
         *
         * Nils Gustafsson
         *
         */
        ImageProcessor ip;
        //loop over frames in imp
        for (int n=0; n<imp.getNSlices(); n++){
            //get frame Processor
            imp.setSlice(n+1);
            ip = imp.getProcessor();
            //add noise
            addPoissonNoise(ip);
            FPNGainOffset(ip, sCMOSFPN);
            //update imp
            imp.setProcessor(ip);
        }
    }

    public static void addPoissonNoise(ImageProcessor ip){
        /**
         * Adds Poisson Noise to an image processor provided which has a mean photons detected per pixel map
         *
         * Nils Gustafsson
         */
        int nPixels = ip.getHeight()*ip.getWidth();
        float v;
        //loop over pixels
        for (int p=0; p<nPixels; p++){
            //sample from poisson distribution
            v = ip.getf(p);
            v = (float) poissonValue((double) v);
            v = Math.max(v, 0);
            v = Math.min(v, 65535);
            //update ip
            ip.setf(p, v);
        }
    }

    public static void FPNGainOffset(ImageProcessor ip, ImagePlus sCMOSFPN){
        /**
         * Adds sCMOS fixed pattern noise to an image processor with true detected
         * photon count per pixel (post shot noise addition) using sCMOS FPN characteristics
         * provided as an ImagePlus
         * ImagePlus sCMOSFPN must be a 3 frame stack - Frame 1: Offset. Frame2: Variance. Frame3: Gain.
         * as provided by CharacterisePixels_
         *
         * Nils Gustafsson
         */
        int width = ip.getWidth();
        int height = ip.getHeight();
        int nPixels = height*width;
        //get pixel specific Offset, Variance and Gain
        sCMOSFPN.setSlice(1);
        ImageProcessor offset = sCMOSFPN.getProcessor();
        float[][] off = offset.getFloatArray();
        sCMOSFPN.setSlice(2);
        ImageProcessor variance = sCMOSFPN.getProcessor();
        float[][] var = variance.getFloatArray();
        sCMOSFPN.setSlice(3);
        ImageProcessor gain = sCMOSFPN.getProcessor();
        float[][] g = gain.getFloatArray();
        float v;
        float readNoise;
        for (int p=0; p<nPixels; p++){
            //calculate pixel specific random read noise
            readNoise = (float) gaussianValue(off[p%width][p/width], Math.sqrt((double)var[p%width][p/width]));
            //add noise to photon number and apply pixel specific gain
            v = ip.getf(p);
            v = v*g[p%width][p/width]+readNoise;
            v = Math.max(v, 0);
            v = Math.min(v, 65535);
            //update ip
            ip.setf(p, v);
        }
    }
}

