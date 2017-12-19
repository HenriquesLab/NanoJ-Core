package nanoj.core.java.localisation.peakDetection;

import ij.ImageStack;
import nanoj.core.java.aparapi.NJKernel;
import nanoj.core.java.array.ImageStackToFromArray;
import nanoj.core.java.image.analysis.CalculateNoiseAndBackground;
import nanoj.core.java.image.filtering.ConvolutionKernels;
import nanoj.core.java.image.filtering.Convolve;

import java.util.Random;

/**
 * Author: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 4/5/13
 * Time: 10:34 AM
 */
public class SmoothPeakFinder {

    private Kernel_smoothFindPeaks2DStack kernel = new Kernel_smoothFindPeaks2DStack();
    private CalculateNoiseAndBackground cnb = new CalculateNoiseAndBackground();

    public String getExecutionMode(){
        return kernel.getExecutionMode().toString();
    }

    public nanoj.core.java.localisation.particlesHandling.ParticlesHolder findPeaks(ImageStack ims, int radius, float snr){
        float [] pixels = ImageStackToFromArray.ImageStackToFloatArray(ims);

        int width = ims.getWidth();
        int height = ims.getHeight();

        return findPeaks(pixels, width, height, radius, snr);
    }

    public nanoj.core.java.localisation.particlesHandling.ParticlesHolder findPeaks(float [] pixels, int width, int height, int radius, float snr){

        kernel.useSmoothing = true;
        return kernel.findPeaks(pixels, width, height, radius, snr);
    }

    public nanoj.core.java.localisation.particlesHandling.ParticlesHolder findPeaksWithDOG(ImageStack ims, int radius, float snr,
                                            float smallSigma, float bigSigma){
        float [] pixels = ImageStackToFromArray.ImageStackToFloatArray(ims);

        int width = ims.getWidth();
        int height = ims.getHeight();

        return findPeaksWithDOG(pixels, width, height, radius, snr, smallSigma, bigSigma);
    }

    public nanoj.core.java.localisation.particlesHandling.ParticlesHolder findPeaksWithDOG(float [] pixels, int width, int height, int radius, float snr,
                                            float smallSigma, float bigSigma){
        kernel.useSmoothing = false;
        Convolve cv = new Convolve();
        float[] pixels_DOG = cv.convolve2DStack(pixels, width, height,
                ConvolutionKernels.genDifferenceOfGaussiansKernel(smallSigma, bigSigma));

        return kernel.findPeaks(pixels_DOG, width, height, radius, snr);
    }

    public nanoj.core.java.localisation.particlesHandling.ParticlesHolder findPeaksWithExtendedLaplacian(ImageStack ims, int radius, float snr){
        float [] pixels = ImageStackToFromArray.ImageStackToFloatArray(ims);

        int width = ims.getWidth();
        int height = ims.getHeight();

        return findPeaksWithExtendedLaplacian(pixels, width, height, radius, snr);
    }

    public nanoj.core.java.localisation.particlesHandling.ParticlesHolder findPeaksWithExtendedLaplacian(float [] pixels, int width, int height, int radius, float snr){
        kernel.useSmoothing = false;
        kernel.usingLaplacian = true;
        Convolve cv = new Convolve();
        float[] pixels_EL = cv.convolve2DStack(pixels, width, height,
                ConvolutionKernels.genExtendedLaplacian());
        return kernel.findPeaks(pixels_EL, width, height, radius, snr);
    }


}

class Kernel_smoothFindPeaks2DStack extends NJKernel {
    private int width, height, widthHeight, radius;
    private float snr;
    private float[] pixels_$constant$;
    private float[] xCenter;
    private float[] yCenter;
    private boolean[] impeaks;
    public boolean useSmoothing = true;
    public boolean usingLaplacian = false;
    private float mean = 0;
    private float std = 0;

    public nanoj.core.java.localisation.particlesHandling.ParticlesHolder findPeaks(float [] pixels, int width, int height, int radius, float snr) {
        // TODO: make sure dialog does not allow radius <1
        assert (radius>=1);

        this.snr = snr;
        this.width = width;
        this.height = height;
        this.widthHeight = width*height;
        this.radius = radius;
        this.pixels_$constant$ = pixels;
        this.impeaks = new boolean[pixels_$constant$.length];
        this.xCenter = new float[pixels_$constant$.length];
        this.yCenter = new float[pixels_$constant$.length];

        // get mean and std for the image
        float delta;
        int limit = min(pixels_$constant$.length, 1000);
        Random r = new Random();
        for (int n=0;n<limit;n++)
        {
            delta = pixels_$constant$[r.nextInt(pixels_$constant$.length)] - mean;
            if (!usingLaplacian) mean += delta / (n+1);
            std += pow(delta, 2);
        }
        std = sqrt(std)/limit;

        setExplicit(true);
        autoChooseDeviceForNanoJ();
        put(pixels_$constant$);
        put(xCenter);
        put(yCenter);
        put(impeaks);
        execute(pixels_$constant$.length); // run kernel on GPU or CPU
        get(impeaks);
        get(xCenter);
        get(yCenter);

        // count the number of peaks
        int npeaks = 0;
        for (int p = 0; p<impeaks.length; p++) npeaks += impeaks[p]? 1 : 0;

        // initialize particles holder for npeaks
        nanoj.core.java.localisation.particlesHandling.ParticlesHolder peaks = new nanoj.core.java.localisation.particlesHandling.ParticlesHolder(npeaks);

        // tell particles holder how I'm going to feed it data
        String [] entrySequence = {"signal", "x", "y", "z", "t"}; // sequence of elements to be given
        float [] entryValues = new float[entrySequence.length]; // 5 element array I'm using to feed the holder

        for (int p = 0; p<impeaks.length; p++)
        {
            if (impeaks[p]) // if it is true (it's a peak)
            {
                entryValues[0] = pixels_$constant$[p]; // intensity
                entryValues[1] = this.xCenter[p]+0.5f; // center x
                entryValues[2] = this.yCenter[p]+0.5f; // center y
                entryValues[4] = p / (widthHeight); // position in time
                peaks.addPoint(entrySequence, entryValues);
            }
        }
        return peaks;
    }

    @Override public void run() {
        int p, x, y, t;

        p = getGlobalId(0);
        x = p % (width);
        y = (p / width) % height;
        t = p / (widthHeight);

        int max_box_size = radius+1;

        if (x >= max_box_size && y >= max_box_size && // check for boundary collisions
            x < width-max_box_size && y < height-max_box_size) {

            checkMax(x, y, t);
        }
    }

    public float getSmoothValue(int x, int y, int t) {
        int pt, pf, p;

        float value = 0;
        pt = t * widthHeight; // position in time * pixels in a frame

        if (!useSmoothing) { // if smoothing is disabled
            pf = y * width + x; // position within a frame
            p = pt + pf;
            value = pixels_$constant$[p];
            return value;
        }

        for (int j=(y-1);j<=(y+1);j++){
            for (int i=(x-1);i<=(x+1);i++){

                pf = j * width + i; // position within a frame
                p = pt + pf;

                value += pixels_$constant$[p];
            }
        }
        return value/9f;
    }

    public void checkMax(int x, int y, int t){

        int pt = t * widthHeight; // position in time * pixels in a frame
        int pf = y * width + x; // position within a frame
        int p = pt + pf; // linear coordinate

        float localValue;
        float comparisonValue = getSmoothValue(x, y, t);

        if (comparisonValue < snr*std+mean)
        {
            impeaks[p] = false;
            return;
        }

        float xc = 0; // used for center of mass
        float yc = 0; // used for center of mass
        float summed_intensities = 0;  // used for center of mass

        for (int j = (y-radius); j <= (y+radius); j++){
            for (int i = (x-radius); i <= (x+radius); i++){

                localValue = getSmoothValue(i, j, t);
                if (!(x == i && y == j) // should not compare against center
                        && localValue >= comparisonValue) {
                    impeaks[p] = false;
                    return;
                }

                // since we are looping through windows, we can calculate the center of mass

                xc += i * localValue;
                yc += j * localValue;
                summed_intensities += localValue;
            }
        }

        // if we got to here, it's local maximum, lets normalize the center of mass
        xc = xc / summed_intensities;
        yc = yc / summed_intensities;

        if (abs(xc-x)>2 && abs(yc-y)>2){
            impeaks[p] = false;
            return;
        }

        // update arrays with these values
        impeaks[p] = true;
        xCenter[p] = xc;
        yCenter[p] = yc;
    }
}
