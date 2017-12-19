package nanoj.kernels;

import ij.process.FloatProcessor;
import nanoj.core.java.aparapi.NJKernel;
import nanoj.core.java.tools.Log;

import static nanoj.core.java.array.ArrayMath.addWithReplace;

/**
 * Created by paxcalpt on 23/03/2017.
 */
public class Kernel_PointsToMap  extends NJKernel {
    private Log log = new Log();

    private float[] xPositions;
    private float[] yPositions;
    private float[] values;
    public float[] pixels;
    float maxDistance;
    private int width, height, nValues;

    public FloatProcessor calculateImage(int width, int height, float[] xPositions, float[] yPositions, float[] values) {
        float[] pixels = calculate(width, height, xPositions, yPositions, values);
        return new FloatProcessor(width, height, pixels);
    }

    public float[] calculate(int width, int height, float[] xPositions, float[] yPositions, float[] values) {
        assert xPositions.length == yPositions.length;
        assert xPositions.length == values.length;

        this.maxDistance = sqrt(width*width + height*height);
        this.width = width;
        this.height = height;
        this.xPositions = xPositions;
        this.yPositions = yPositions;
        this.values = values;
        this.nValues = values.length;
        this.pixels = new float[width*height];

        if (nValues == 0) return this.pixels;
        if (nValues == 1) {
            addWithReplace(this.pixels, 1);
            return this.pixels;
        }

        // Upload arrays
        setExplicit(true);
        //setExecutionMode(EXECUTION_MODE.JTP); // seems that running on JTP is faster than GPU
        autoChooseDeviceForNanoJ();

        put(this.xPositions);
        put(this.yPositions);
        put(this.values);
        put(this.pixels);

        executeByBlocks(this.pixels.length);

        // Download arrays
        get(this.pixels);
        return this.pixels;
    }

    // called inside CL
    @Override
    public void run() {
        int p = getGlobalId()+blockOffset;
        int x = p % width;
        int y = p / width;

        float value = 0;
        float wSum = 0;

        for (int n=0; n<nValues; n++) {
            float d = sqrt(pow(xPositions[n] - x, 2) + pow(yPositions[n] - y, 2)) + 1;
            float weight = pow(((maxDistance - d) / (maxDistance * d)), 2) / maxDistance;
            value += values[n] * weight;
            wSum += weight;
        }

        value /= wSum;
        pixels[p] = value;
    }
}
