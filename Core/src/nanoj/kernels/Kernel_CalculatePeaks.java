package nanoj.kernels;

import ij.ImageStack;
import ij.process.FloatProcessor;
import nanoj.core.java.aparapi.NJKernel;
import nanoj.core.java.tools.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeMap;

import static nanoj.core.java.array.ArrayMath.getBackgroundMeanAndStdDev;
import static nanoj.core.java.array.ArrayTypeConversion.ArrayListFloat2float;
import static nanoj.core.java.array.ImageStackToFromArray.ImageStackToFloatArray;

/**
 * Created by sculley on 11/04/2016.
 */

public class Kernel_CalculatePeaks extends NJKernel {
    private int width, height, widthM1, heightM1, timeM1, widthHeight, nTimePoints, do3D;
    private float[] pixels, minIntensity;
    private float[][][] peaks;
    public boolean[] peakMap;
    private Log log = new Log();

    public boolean[] calculate(FloatProcessor fp, float minSNR) {
        return this.calculate((float[]) fp.getPixels(), fp.getWidth(), fp.getHeight(), minSNR, false);
    }

    public boolean[] calculate(ImageStack ims, float minSNR, boolean do3D) {
        int width = ims.getWidth();
        int height = ims.getHeight();
        return this.calculate(ImageStackToFloatArray(ims), width, height, minSNR, true);
    }

    public boolean[] calculate(float[] pixels, int width, int height, float minSNR, boolean do3D) {

        // Input image properties
        this.width = width;
        this.height = height;
        this.widthHeight = width * height;
        this.nTimePoints = pixels.length / widthHeight;
        this.widthM1 = width-1;
        this.heightM1 = height-1;
        this.timeM1 = nTimePoints-1;

        // Initialise Arrays
        this.pixels = pixels;
        this.minIntensity = new float[nTimePoints];
        this.peakMap = new boolean[pixels.length];

        this.do3D = (do3D ? 1 : 0);


        // Calculate values for SNR
        for (int t=0; t<nTimePoints; t++) {

            int tp = t * widthHeight;
            float[] pixelsFrame = new float[widthHeight];
            System.arraycopy(pixels, tp, pixelsFrame, 0, widthHeight);

            double[] meanAndStdDev = getBackgroundMeanAndStdDev(pixelsFrame, 0.1f, true);// take only lowest 10%
            double mean = meanAndStdDev[0];
            double stdDev = meanAndStdDev[1];

            this.minIntensity[t] = (float) (minSNR * stdDev + mean);
        }

        // Upload arrays
        setExplicit(true);
        autoChooseDeviceForNanoJ();

        put(this.pixels);
        put(this.peakMap);
        put(this.minIntensity);

        execute(pixels.length);
        get(this.peakMap);

        // Convert peakMap into peak positions
        this.peaks = new float[nTimePoints][3][];

        for (int t=0; t<nTimePoints; t++) {

            ArrayList<Float> xpoints = new ArrayList<Float>();
            ArrayList<Float> ypoints = new ArrayList<Float>();
            int pStart = t*widthHeight;
            int pEnd = (t+1)*widthHeight;
            for (int p=pStart; p<pEnd; p++) {
                if (peakMap[p]) {
                    xpoints.add(p % (width) + 0.5f);
                    ypoints.add((p / width) % height + 0.5f);
                }
            }
            peaks[t][0] = ArrayListFloat2float(xpoints);
            peaks[t][1] = ArrayListFloat2float(ypoints);
        }
        return peakMap;
    }

    public float[] getXPoints(int t) {
        return peaks[t][0];
    }

    public float[] getYPoints(int t) {
        return peaks[t][1];
    }

    public float[][] getXYPoints(int t) {
        return new float[][] {peaks[t][0], peaks[t][1]};
    }

    public float[][] getXYPointsSortedByIntensity(int t, boolean lowToHigh) {
        float[] xPoints = getXPoints(t);
        float[] yPoints = getYPoints(t);

        TreeMap<Float, Integer> map;
        if (!lowToHigh) map = new TreeMap<Float, Integer>(Collections.reverseOrder());
        else map = new TreeMap<Float, Integer>();

        for (int p=0; p<xPoints.length; p++) {
            float v = pixels[t*widthHeight+((int) yPoints[p]*width)+((int) xPoints[p])];
            map.put(v, p);
        }

        float[] xPointsSorted = new float[xPoints.length];
        float[] yPointsSorted = new float[xPoints.length];

        int counter = 0;
        for (int i: map.values()) {
            xPointsSorted[counter] = xPoints[i];
            yPointsSorted[counter] = yPoints[i];
            counter++;
        }

        return new float[][]{xPointsSorted, yPointsSorted};
    }

    @Override
    public void run() {
        int p = getGlobalId(0);
        int x = p % (width);
        int y = (p / width) % height;
        int t = p / (widthHeight);

        if(do3D==0) {
            if (pixels[p] > this.minIntensity[t]) calculatePeakMap(p, x, y, t);
        }
        else{
            if (pixels[p] > this.minIntensity[t]) calculatePeakMap3D(p, x, y, t);
        }
    }

    private void calculatePeakMap(int p, int x, int y, int t) {

        float v = pixels[p];

        if (v==0) return;
        if (pixels[getIdx3BC(x-1, y, t)] > v) return;
        if (pixels[getIdx3BC(x+1, y, t)] > v) return;

        if (pixels[getIdx3BC(x-1, y-1, t)] > v) return;
        if (pixels[getIdx3BC(x  , y-1, t)] > v) return;
        if (pixels[getIdx3BC(x+1, y-1, t)] > v) return;
        if (pixels[getIdx3BC(x-1, y+1, t)] > v) return;
        if (pixels[getIdx3BC(x  , y+1, t)] > v) return;
        if (pixels[getIdx3BC(x+1, y+1, t)] > v) return;

        peakMap[p] = true;
    }

    private void calculatePeakMap3D(int p, int x, int y, int t) {

        float v = pixels[p];

        if (v==0) return;

        for(int t_=t-1; t_<=t+1; t_++) {
            if (pixels[getIdx3BC(x - 1, y, t_)] > v) return;
            if (pixels[getIdx3BC(x + 1, y, t_)] > v) return;

            if (pixels[getIdx3BC(x - 1, y - 1, t_)] > v) return;
            if (pixels[getIdx3BC(x,     y - 1, t_)] > v) return;
            if (pixels[getIdx3BC(x + 1, y - 1, t_)] > v) return;
            if (pixels[getIdx3BC(x - 1, y + 1, t_)] > v) return;
            if (pixels[getIdx3BC(x,     y + 1, t_)] > v) return;
            if (pixels[getIdx3BC(x + 1, y + 1, t_)] > v) return;
        }

        if (pixels[getIdx3BCz(x  , y  , t-1)] > v) return;
        if (pixels[getIdx3BCz(x  , y  , t+1)] > v) return;

        peakMap[p] = true;
    }

    private int getIdx3(int x, int y, int t) {
        int pt = t * widthHeight;
        int pf = y * width + x; // position within a frame
        return pt + pf;
    }

    // version with boundary check
    private int getIdx3BC(int x, int y, int t) {
        x = min(max(0, x), widthM1);
        y = min(max(0, y), heightM1);
        int pt = t * widthHeight;
        int pf = y * width + x; // position within a frame
        return pt + pf;
    }

    private int getIdx3BCz(int x, int y, int t) {
        x = min(max(0, x), widthM1);
        y = min(max(0, y), heightM1);
        t = min(max(0, t), timeM1);
        int pt = t * widthHeight;
        int pf = y * width + x; // position within a frame
        return pt + pf;
    }
}