package nanoj.kernels;

import nanoj.core.java.aparapi.NJKernel;
import nanoj.core.java.tools.Log;

/**
 * Created by paxcalpt on 23/03/2017.
 */
public class Kernel_NearestNeighbour extends NJKernel {
    private int MAX_PARTICLES_PER_RUN = 1000;

    private float[] xPositions0;
    private float[] yPositions0;
    private float[] zPositions0;
    private float[] xPositions1;
    private float[] yPositions1;
    private float[] zPositions1;
    private int allowStationary;
    public float[] nearestDistance;
    public float[] nearestNeighbour;
    int nParticles0, nParticles1, nStart, nEnd;

    private Log log = new Log();
    private boolean showProgress = false;

    public float[][] calculate(float[] xPositions, float[] yPositions){
        return calculate(
                xPositions, yPositions, new float[xPositions.length],
                xPositions, yPositions, new float[xPositions.length],
                false);
    }

    public float[][] calculate(float[] xPositions, float[] yPositions, float[] zPositions){
        return calculate(
                xPositions, yPositions, zPositions,
                xPositions, yPositions, zPositions,
                false);
    }

    public float[][] calculate(float[] xPositions0, float[] yPositions0,
                               float[] xPositions1, float[] yPositions1,
                               boolean allowStationary){
        return calculate(
                xPositions0, yPositions0, new float[xPositions0.length],
                xPositions1, yPositions1, new float[xPositions1.length],
                allowStationary);
    }

    public float[][] calculate(float[] xPositions0, float[] yPositions0, float[] zPositions0,
                               float[] xPositions1, float[] yPositions1, float[] zPositions1,
                               boolean allowStationary) {
        this.nParticles0 = xPositions0.length;
        this.nParticles1 = xPositions1.length;
        this.xPositions0 = xPositions0;
        this.yPositions0 = yPositions0;
        this.zPositions0 = zPositions0;
        this.xPositions1 = xPositions1;
        this.yPositions1 = yPositions1;
        this.zPositions1 = zPositions1;
        if(allowStationary)this.allowStationary = 1;
        else this.allowStationary = 0;
        this.nearestNeighbour = new float[xPositions0.length];
        this.nearestDistance = new float[xPositions0.length];
        for (int n=0; n<nearestDistance.length; n++) this.nearestDistance[n] = Float.MAX_VALUE;

        // Upload arrays
        setExplicit(true);
        //setExecutionMode(EXECUTION_MODE.JTP);
        autoChooseDeviceForNanoJ();

        put(this.xPositions0);
        put(this.yPositions0);
        put(this.zPositions0);
        put(this.xPositions1);
        put(this.yPositions1);
        put(this.zPositions1);
        put(this.nearestDistance);
        put(this.nearestNeighbour);

        int groups = nParticles1 / MAX_PARTICLES_PER_RUN;
        if (nParticles1 % MAX_PARTICLES_PER_RUN != 0) groups++;

        for (int pG = 0; pG<groups; pG++) {
            if (showProgress) log.msg("Nearest-Neighbour: Analysing particles group "+pG+"/"+groups);
            nStart = pG * MAX_PARTICLES_PER_RUN;
            nEnd = (pG + 1) * MAX_PARTICLES_PER_RUN;
            nEnd = min(nEnd, nParticles1);
            execute(this.xPositions0.length);
        }

        // Download arrays
        get(this.nearestDistance);
        get(this.nearestNeighbour);

        return new float[][]{this.nearestDistance, this.nearestNeighbour};
    }

    // called inside CL
    @Override
    public void run() {
        int p0 = getGlobalId();
        float x0 = xPositions0[p0];
        float y0 = yPositions0[p0];
        float z0 = zPositions0[p0];

        float smallestDistance = nearestDistance[p0];
        int closestNeighbour = (int) nearestNeighbour[p0];

        for (int p1=nStart; p1<nEnd; p1++) {
            float x1 = xPositions1[p1];
            float y1 = yPositions1[p1];
            float z1 = zPositions1[p1];

            if(allowStationary==1){
                float dx = x0-x1;
                float dy = y0-y1;
                float dz = z0-z1;

                if (abs(dx) < smallestDistance && abs(dy) < smallestDistance && abs(dz) < smallestDistance) {
                    float d = sqrt(pow(dx,2)+pow(dy,2)+pow(dz,2));
                    if (d < smallestDistance) {
                        smallestDistance = d;
                        closestNeighbour = p1;
                    }
                }
            }
            else {
                if (x0 != x1 && y0 != y1 && y0 != y1) {
                    float dx = x0 - x1;
                    float dy = y0 - y1;
                    float dz = z0 - z1;

                    if (abs(dx) < smallestDistance && abs(dy) < smallestDistance && abs(dz) < smallestDistance) {
                        float d = sqrt(pow(dx, 2) + pow(dy, 2) + pow(dz, 2));
                        if (d < smallestDistance) {
                            smallestDistance = d;
                            closestNeighbour = p1;
                        }
                    }
                }
            }
        }
        this.nearestDistance[p0] = smallestDistance;
        this.nearestNeighbour[p0] = closestNeighbour;
    }
}