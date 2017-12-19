package nanoj.core.java.localisation.peakDetection;

import ij.ImageStack;
import ij.process.FloatProcessor;
import nanoj.core.java.aparapi.NJKernel;
import nanoj.core.java.array.ImageStackToFromArray;

import static nanoj.core.java.array.ImageStackToFromArray.ImageStackFromFloatArray;

/**
 * Author: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 4/5/13
 * Time: 10:34 AM
 */
public class LocalMaximaDetector {

    private static final Kernel_LocalMaximaDetector kernel = new Kernel_LocalMaximaDetector();

    public String getExecutionMode(){
        return kernel.getExecutionMode().toString();
    }

    public ImageStack calculate(ImageStack ims, float radius, boolean keepOriginalPeakValue){
        float [] pixels = ImageStackToFromArray.ImageStackToFloatArray(ims);

        int width = ims.getWidth();
        int height = ims.getHeight();

        return ImageStackFromFloatArray(calculate(pixels, width, height, radius, keepOriginalPeakValue), width, height);
    }

    public FloatProcessor calculate(FloatProcessor ip, float radius, boolean keepOriginalPeakValue){
        float [] pixels = (float[]) ip.getPixels();

        int width = ip.getWidth();
        int height = ip.getHeight();

        return new FloatProcessor(width, height, calculate(pixels, width, height, radius, keepOriginalPeakValue));
    }

    public float[] calculate(float [] pixels, int width, int height, float radius, boolean keepOriginalPeakValue){
        return kernel.calculate(pixels, width, height, radius, keepOriginalPeakValue);
    }
}

class Kernel_LocalMaximaDetector extends NJKernel {
    private int width, height, widthHeight, iRadius;
    private float radius;
    private float[] pixels;
    private float[] impeaks;
    public int useOriginalValue = 1;

    public float[] calculate(float [] pixels, int width, int height, float radius, boolean keepOriginalPeakValue) {
        assert (radius>=1);

        this.width = width;
        this.height = height;
        this.widthHeight = width*height;
        this.radius = radius;
        this.iRadius = (int) floor(radius);
        this.useOriginalValue = keepOriginalPeakValue? 1: 0;
        this.pixels = pixels;
        this.impeaks = new float[this.pixels.length];

        setExplicit(true);
        autoChooseDeviceForNanoJ();
        put(this.pixels);
        put(this.impeaks);
        execute(this.pixels.length);
        get(this.impeaks);

        return impeaks;
    }

    @Override public void run() {
        int p, x, y, t;

        p = getGlobalId(0);
        x = p % (width);
        y = (p / width) % height;
        t = p / (widthHeight);

        int max_box_size = iRadius + 1;
        if (x >= max_box_size && y >= max_box_size && // check for boundary collisions
            x < width-max_box_size && y < height-max_box_size) {
            checkMax(x, y, t);
        }
    }

    public float getValue(int x, int y, int t) {
        return pixels[t * widthHeight + y * width + x];
    }

    public void checkMax(int x, int y, int t){

        int pt = t * widthHeight; // position in time * pixels in a frame
        int pf = y * width + x; // position within a frame
        int p = pt + pf; // linear coordinate

        float localValue;
        float comparisonValue = getValue(x, y, t);

        for (int j = (y-iRadius); j <= (y+iRadius); j++){
            for (int i = (x-iRadius); i <= (x+iRadius); i++){
                if (sqrt(pow(i-x, 2) + pow(j-y, 2)) <= radius) {
                    localValue = getValue(i, j, t);
                    if (!(x == i && y == j) && localValue >= comparisonValue) {
                        return;
                    }
                }
            }
        }
        if (useOriginalValue == 1) impeaks[p] = pixels[p];
        else impeaks[p] = 1;
    }
}
