package nanoj.kernels;

import ij.process.FloatProcessor;
import nanoj.core.java.aparapi.NJKernel;
import nanoj.core.java.array.ArrayMath;
import nanoj.core.java.tools.Log;

/**
 * Created by Henriques-lab on 10/05/2016.
 */
public class Kernel_LocalSimilarity extends NJKernel {

    private int width, height, widthM1, heightM1, widthHeight;
    private float globalMean1 = 0, globalMean2 = 0;
    private float[] pixels1, pixels2, pixelsSimilarity;
    private int spatialRadius;
    public int doZeroMin = 1; // if 1 - truncate minimum to 0
    private int doGlobalMean = 1; // if 1 - use global mean instead of local mean
    private Log log = new Log();

    public FloatProcessor calculate(FloatProcessor fp1, FloatProcessor fp2, int spatialRadius) {
        int w = fp1.getWidth();
        int h = fp1.getHeight();
        assert (w == fp2.getWidth() && h == fp2.getHeight());

        float[] pixels = calculate((float[]) fp1.getPixels(), (float[]) fp2.getPixels(), w, h, spatialRadius);
        return new FloatProcessor(w, h, pixels);
    }

    public void setGlobalMean(boolean flag) {
        if (flag) doGlobalMean = 1;
        else doGlobalMean = 0;
    }

    public float[] calculate(float[] pixels1, float[] pixels2, int width, int height, int spatialRadius) {
        // Input image properties
        this.width =  width;
        this.height = height;
        this.widthHeight = width * height;
        this.widthM1 = width-1;
        this.heightM1 = height-1;
        this.spatialRadius = spatialRadius;

        this.pixels1 = pixels1;
        this.pixels2 = pixels2;
        this.pixelsSimilarity = new float[pixels1.length];

        if (doGlobalMean == 1) {
            globalMean1 = ArrayMath.getAverageValue(pixels1);
            globalMean2 = ArrayMath.getAverageValue(pixels2);
        }

        // Upload arrays
        setExplicit(true);
        autoChooseDeviceForNanoJ();
        //setExecutionMode(EXECUTION_MODE.JTP);

        put(this.pixels1);
        put(this.pixels2);
        put(this.pixelsSimilarity);

        execute(this.pixelsSimilarity.length);

        get(this.pixelsSimilarity);
        return this.pixelsSimilarity;
    }

    @Override
    public void run() {
        int pixelIdx = getGlobalId();
        int x = pixelIdx % width;
        int y = (pixelIdx / width) % height;

        // calculate means
        float localMean1 = 0;
        float localMean2 = 0;

        if (doGlobalMean != 1) {
            int counter = 1;
            for (int j=y-spatialRadius; j<=y+spatialRadius; j++) {
                for (int i=x-spatialRadius; i<=x+spatialRadius; i++) {
                    int p = getIdx2BC(i, j);
                    localMean1 += (pixels1[p] - localMean1) / counter;
                    localMean2 += (pixels2[p] - localMean2) / counter;
                    counter++;
                }
            }
        }
        else {
            localMean1 = globalMean1;
            localMean2 = globalMean2;
        }

        // calculate similarity
        float covariance = 0;
        float squareSum1  = 0;
        float squareSum2  = 0;
        for (int j=y-spatialRadius; j<=y+spatialRadius; j++) {
            for (int i=x-spatialRadius; i<=x+spatialRadius; i++) {
                int p = getIdx2BC(i, j);
                float v1 = pixels1[p] - localMean1;
                float v2 = pixels2[p] - localMean2;
                covariance += v1*v2;
                squareSum1 += v1*v1;
                squareSum2 += v2*v2;
            }
        }
        float similarity = 0;
        if (squareSum1 > 0.01f && squareSum2 > 0.01f) similarity = covariance / sqrt(squareSum1 * squareSum2);
        if (doZeroMin == 1) similarity = max(0, similarity);
        pixelsSimilarity[pixelIdx] = similarity;
    }

    // version with boundary check
    private int getIdx2BC(int x, int y) {
        x = min(max(0, x), widthM1);
        y = min(max(0, y), heightM1);
        return y * width + x; // position within a frame
    }
}
