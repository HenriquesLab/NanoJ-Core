package nanoj.kernels;

import ij.process.FloatProcessor;
import nanoj.core.java.aparapi.NJKernel;
import nanoj.core.java.tools.Log;

import static nanoj.core.java.array.ArrayCasting.intToFloat;

/**
 * Created by paxcalpt on 23/03/2017.
 */
public class Kernel_VoronoiImage extends NJKernel {
    private int MAX_PARTICLES_PER_RUN = 500;
    private Log log = new Log();

    private float[] xPositions;
    private float[] yPositions;
    private float[] values;
    public float[] pixels;
    public float[] nearestDistance;
    public int[] nearestNeighbour;
    int nParticles, nStart, nEnd, width, height;

    public FloatProcessor calculateImage(int width, int height, int[] xPositions, int[] yPositions, float[] values) {
        float[] pixels = calculate(width, height, intToFloat(xPositions), intToFloat(yPositions), values);
        return new FloatProcessor(width, height, pixels);
    }

    public FloatProcessor calculateImage(int width, int height, float[] xPositions, float[] yPositions, float[] values) {
        float[] pixels = calculate(width, height, xPositions, yPositions, values);
        return new FloatProcessor(width, height, pixels);
    }

    public float[] calculate(int width, int height, float[] xPositions, float[] yPositions, float[] values) {
        this.nParticles = xPositions.length;
        this.width = width;
        this.height = height;
        this.xPositions = xPositions;
        this.yPositions = yPositions;
        this.values = values;
        this.pixels = new float[width*height];
        this.nearestNeighbour = new int[width*height];
        this.nearestDistance = new float[width*height];
        for (int n=0; n<nearestDistance.length; n++) this.nearestDistance[n] = Float.MAX_VALUE;

        // Upload arrays
        setExplicit(true);
        //setExecutionMode(EXECUTION_MODE.JTP); // seems that running on JTP is faster than GPU
        autoChooseDeviceForNanoJ();

        put(this.xPositions);
        put(this.yPositions);
        put(this.values);
        put(this.pixels);
        put(this.nearestDistance);
        put(this.nearestNeighbour);

        int groups = nParticles / MAX_PARTICLES_PER_RUN;
        if (nParticles % MAX_PARTICLES_PER_RUN != 0) groups++;

        for (int pG = 0; pG<groups; pG++) {
            log.progress(pG, groups);
            nStart = pG * MAX_PARTICLES_PER_RUN;
            nEnd = (pG + 1) * MAX_PARTICLES_PER_RUN;
            nEnd = min(nEnd, nParticles);
            execute(this.pixels.length);
        }
        log.progress(1);

        // Download arrays
        get(this.pixels);
        return this.pixels;
    }

    // called inside CL
    @Override
    public void run() {
        int p = getGlobalId();
        int x = p % width;
        int y = p / width;

        float smallestDistance = nearestDistance[p];
        int closestNeighbour = nearestNeighbour[p];

        for (int n=nStart; n<nEnd; n++) {
            float xp = xPositions[n];
            float yp = yPositions[n];
            float dx = abs(xp-x);
            float dy = abs(yp-y);

            if (dx < smallestDistance && dy < smallestDistance) {
                float d = sqrt(dx*dx+dy*dy);
                if (d < smallestDistance) {
                    smallestDistance = d;
                    closestNeighbour = n;
                }
            }
        }
        nearestDistance[p] = smallestDistance;
        nearestNeighbour[p] = closestNeighbour;
        pixels[p] = values[closestNeighbour];
    }
}
