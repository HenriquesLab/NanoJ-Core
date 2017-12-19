package nanoj.core.java.image.transform;

import ij.process.FloatProcessor;
import nanoj.core.java.aparapi.NJKernel;
import nanoj.core.java.tools.Log;

import java.util.ArrayList;

import static nanoj.core.java.array.ArrayCasting.toArray;

/**
 * Created by robertgray on 14/03/2016.
 */
public class ZeroFillWithInterpolation {
    private static Kernel_NearestNeighbour kernel = new Kernel_NearestNeighbour();
    private Log log = new Log();

    public String getExecutionMode(){
        return kernel.getExecutionMode().toString();
    }

    public FloatProcessor calculate(FloatProcessor ip, int iterationsPerCycle) {
        float[] pixels = (float[]) ip.getPixels();

        while (hasZeros(pixels)) {
            pixels = kernel.calculate(pixels, ip.getWidth(), ip.getHeight(), iterationsPerCycle);
        }

        return new FloatProcessor(ip.getWidth(), ip.getHeight(), pixels);
    }

    private boolean hasZeros(float[] pixels) {
        for (int n=0; n<pixels.length; n++) {
            if (pixels[n] == 0) return true;
        }
        return false;
    }
}

class Kernel_NearestNeighbour extends NJKernel {

    private float[] pixels;
    private int[] zeroPixelIndex;
    private int width;
    private int height;
    private int nPixels;

    public float[] calculate(float[] pixels, int width, int height, int nInterations) {
        this.pixels = pixels;
        this.width = width;
        this.height = height;
        this.nPixels = pixels.length;

        ArrayList<Integer> zeroPixelList = new ArrayList<Integer>();
        for (int n=0; n<nPixels; n++) if (pixels[n]==0) zeroPixelList.add(n);
        this.zeroPixelIndex = toArray(zeroPixelList, 0);

        // Upload arrays
        setExplicit(true);
        //setExecutionMode(EXECUTION_MODE.JTP);
        autoChooseDeviceForNanoJ();

        put(this.pixels);
        put(this.zeroPixelIndex);

        for (int i=0; i<nInterations; i++)
            execute(this.zeroPixelIndex.length);

        // Download arrays
        get(this.pixels);

        return this.pixels;
    }

    // called inside CL
    @Override
    public void run() {
        int p = zeroPixelIndex[getGlobalId()];
        int x = p % width;
        int y = p / width;

        int vMean = 0;
        int counter = 0;

        for (int j=y-1; j<=y+1; j++) {
            if (j>=0 && j<height) {
                for (int i=x-1; i<=x+1; i++) {
                    if (i>=0 && i<width) {
                        float v = pixels[j*width+i];
                        if (v != 0) {
                            counter++;
                            vMean += (v - vMean) / (counter);
                        }
                    }
                }
            }
        }

        pixels[p] = vMean;
    }
}