package nanoj.kernels;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Plot;
import ij.process.Blitter;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij3d.Content;
import ij3d.Image3DUniverse;
import nanoj.core.java.aparapi.NJKernel;
import nanoj.core.java.tools.Log;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Point3f;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import static ij.plugin.HyperStackConverter.toHyperStack;
import static nanoj.core.java.array.ArrayInitialization.initializeAndValueFill;

/**
 * Created by Henriques-lab on 23/07/2017.
 */
public class Kernel_BasePSO extends NJKernel {

    protected Log log = new Log();
    public boolean showMessages = true;

    // Velocity generation weighting factors, suggest leaving both as 1
    public double Ccog = 1.9; // suggestion from Nobile et al, 2015
    public double Csoc = 1.9; // suggestion from Nobile et al, 2015
    // Inertia weighting factors - currently unused
    public double W_UP = 0.9; // suggestion from Nobile et al, 2015
    public double W_LO = 0.4; // suggestion from Nobile et al, 2015

    public static final int DONT_OBEY_BOUNDARY = 0;
    public static final int OBEY_BOUNDARY = 1;
    public static final int OBEY_HIGH_BOUNDARY = 2;
    public static final int OBEY_LOW_BOUNDARY = 3;
    public static final int CONSTANT = 4;

    protected Random generator = new Random(327636000); // always use same seed for repeatability;

    protected int swarmSize, nIterations, nVariables;
    protected float[] position_particles;
    protected float[] velocity_particles;
    protected float[] position_particleBest;
    protected double[] position_lowBoundary;
    protected double[] position_highBoundary;
    protected double[] position_max;
    protected double[] position_min;
    private int[] position_constrainToBoundaries;
    private double minimalImprovement;

    protected double[] globalBestPosition;
    protected double globalBestError;
    protected double globalWorstError;
    protected double globalDeltaError;
    protected double[] localBestError;

    private double bestImprovement;
    private double averageImprovement;
    protected boolean stop;

    protected LinkedHashMap<Double, Double> globalBestError_history;
    protected LinkedHashMap<Double, Double>[] localBestError_history;

    protected float[][] position_particles_history;

    public void initializePSO(int swarmSize, int nIterations,
                              double[] position_lowBoundary, double[] position_highBoundary,
                              double[] position_bestGuess, int[] position_constrainToBoundaries,
                              double minimalImprovement) {

        this.swarmSize = swarmSize;
        this.nIterations = nIterations;
        this.nVariables = position_lowBoundary.length;
        this.minimalImprovement = minimalImprovement;

        this.position_lowBoundary = position_lowBoundary;
        this.position_highBoundary = position_highBoundary;

        this.position_particles = new float[swarmSize * nVariables]; // [p0_v0, p0_v1, p0_v2, ..., pN_v0, pN_v1, pN_v2]
        this.velocity_particles = new float[swarmSize * nVariables];
        this.position_particleBest = new float[swarmSize * nVariables];
        this.position_particles_history = new float[nIterations][];
        this.position_max = position_highBoundary.clone();
        this.position_min = position_lowBoundary.clone();
        this.position_constrainToBoundaries = position_constrainToBoundaries;

        this.globalBestError_history = new LinkedHashMap<Double, Double>();
        this.localBestError_history = new LinkedHashMap[swarmSize];
        for (int s=0; s<swarmSize; s++)
            this.localBestError_history[s] = new LinkedHashMap<Double, Double>();

        globalBestError = Double.MAX_VALUE;
        globalWorstError = -Double.MAX_VALUE;
        globalDeltaError = Double.NaN;
        globalBestPosition = new double[nVariables];
        localBestError = initializeAndValueFill(swarmSize, Double.MAX_VALUE);

        bestImprovement = -Double.MAX_VALUE;
        averageImprovement = 0;
        stop = false;

        // Initialise particles
        for (int n = 0; n < nVariables; n++) {
            double position_deltaBoundary = position_highBoundary[n] - position_lowBoundary[n];
            double velocity_deltaBoundary = 0.2f * position_deltaBoundary;

            // to deal with best guess, we put the first particle in that position to make sure it gets calculated
            position_particles[n] = (float) position_bestGuess[n];

            // initialise the rest of the particles
            if (position_constrainToBoundaries[n] == CONSTANT) {
                for(int pId=1; pId<swarmSize; pId++) {
                    position_particles[pId * nVariables + n] = (float) position_bestGuess[n];
                    velocity_particles[pId * nVariables + n] = 0;
                }
            }
            else {
                for (int pId = 1; pId < swarmSize; pId++) {
                    position_particles[pId * nVariables + n] = (float) (position_lowBoundary[n] + generator.nextFloat() * position_deltaBoundary);
                    velocity_particles[pId * nVariables + n] = (float) ((generator.nextFloat() * 2 - 1) * velocity_deltaBoundary);
                }
            }
        }
    }

    public void updateError(double error, int iteration, int pId) {
        // Get Global Worst
        globalWorstError = max(error, globalWorstError); // keep track of the worst

        // Get Global Best and Global Delta
        if (error < globalBestError) { // check if global best
            if (globalBestError != Double.MAX_VALUE) globalDeltaError = globalBestError - error;

            globalBestError = error;
            for (int n=0; n<nVariables; n++) globalBestPosition[n] = position_particles[pId * nVariables + n];
            globalBestError_history.put((double) iteration, error);
        }

        // Get Local Best
        if (error < localBestError[pId]) { // check if local best
            localBestError[pId] = error;
            for (int n=0; n<nVariables; n++) {
                int vp = pId * nVariables + n;
                position_particleBest[vp] = position_particles[vp];
            }
            localBestError_history[pId].put((double) iteration, error);
        }
    }

    public void updateParticlePosition(int iteration) {

        // Keep an history log of the particles position
        this.position_particles_history[iteration-1] = this.position_particles.clone();

        // Start the update
        double progress = ((double) iteration)/ nIterations;

        // calculate the inertia for current iteration
        double w = W_UP - (W_UP-W_LO) * progress;

        double averageParticleDistanceTraveled = 0;

        for(int pId=0; pId<swarmSize; pId++) {

            // now lets update the particle velocity and position
            double Rcog = generator.nextDouble();
            double Rsoc = generator.nextDouble();

            double distanceTraveled = 0;

            for (int n = 0; n < nVariables; n++) {

                int vp = pId * nVariables + n;
                double velocity = velocity_particles[vp];
                double position = position_particles[vp];
                double localBestPosition = position_particleBest[vp];

                if (position_constrainToBoundaries[n] != CONSTANT) {
                    // note that we are mostly unconstraining.... this actually seems to work better for our case
                    double velocityProposal = w * velocity + (Rcog * Ccog) * (localBestPosition - position) + (Rsoc * Csoc) * (globalBestPosition[n] - position);
                    double positionProposal = position_particles[vp] + velocityProposal;

                    if (position_constrainToBoundaries[n] > 0) { // impose rigid boundary !!
                        if ((position_constrainToBoundaries[n] == OBEY_BOUNDARY || position_constrainToBoundaries[n] == OBEY_LOW_BOUNDARY) && positionProposal < position_lowBoundary[n]) {
                            velocityProposal *= -generator.nextFloat();
                            position_particles[vp] = (float) position_lowBoundary[n];
                        } else if ((position_constrainToBoundaries[n] == OBEY_BOUNDARY || position_constrainToBoundaries[n] == OBEY_HIGH_BOUNDARY) && positionProposal > position_highBoundary[n]) {
                            velocityProposal *= -generator.nextFloat();
                            position_particles[vp] = (float) position_highBoundary[n];
                        }
                    }
                    if (abs(velocityProposal) > (position_highBoundary[n] - position_lowBoundary[n])) { // impose speed limit !!
                        velocityProposal *= generator.nextFloat(); // dampen speed
                    }

                    velocity_particles[vp] = (float) velocityProposal;
                    position_particles[vp] += velocity_particles[vp];
                }

                // calculate distance
                position_max[n] = max(position_particles[vp], position_max[n]);
                position_min[n] = min(position_particles[vp], position_min[n]);
                double newPositionNormalised = (position_particles[vp] - position_min[n]) / (position_max[n] - position_min[n]);
                double oldPositionNormalised = (position - position_min[n]) / (position_max[n] - position_min[n]);
                distanceTraveled += pow(newPositionNormalised - oldPositionNormalised, 2);
            }

            averageParticleDistanceTraveled += sqrt(distanceTraveled/nVariables) / swarmSize;
        }

        double improvement = (globalDeltaError/globalWorstError) * averageParticleDistanceTraveled;

        if (!Double.isNaN(improvement)) {
            bestImprovement = max(bestImprovement, improvement);
            if (averageImprovement == 0) averageImprovement = improvement;
            else averageImprovement = averageImprovement * 0.9 + improvement * 0.1;
            if (!Double.isNaN(minimalImprovement) && averageImprovement/bestImprovement < minimalImprovement) {
                if (showMessages) log.msg(String.format("Improvement below %2.2E, stopping PSO at iteration %d", minimalImprovement, iteration));
                stop = true;
            }
        }
    }

    public void plotOptimizationEvolution(String[] variableNames) {
        assert (variableNames.length == nVariables);

        double[] xVals, yVals;
        ImageStack imsPlots = null;

        // Plot the global values
        Plot plotGlobalError = new Plot("Global error evolution", "Iteration number", "Error");
        int nValues = globalBestError_history.size();
        int counter = 0;
        xVals = new double[nValues];
        yVals = new double[nValues];
        for (Map.Entry<Double, Double> e : globalBestError_history.entrySet()) {
            xVals[counter] = e.getKey();
            yVals[counter] = e.getValue();
            counter++;
        }
        plotGlobalError.addPoints(xVals, yVals, Plot.LINE);
        ImageProcessor ip = plotGlobalError.getProcessor().convertToColorProcessor();
        imsPlots = new ImageStack(ip.getWidth(), ip.getHeight());
        imsPlots.addSlice(ip);

        // Plot the local values
        Plot plotLocalError = new Plot("Local error evolution", "Iteration number", "Error");
        for (int pId=0; pId<swarmSize; pId++) {
            Color color = new Color(generator.nextInt(255),generator.nextInt(255), generator.nextInt(255));
            nValues = localBestError_history[pId].size();
            counter = 0;
            xVals = new double[nValues];
            yVals = new double[nValues];
            for (Map.Entry<Double, Double> e :localBestError_history[pId].entrySet()) {
                xVals[counter] = e.getKey();
                yVals[counter] = e.getValue();
                counter++;
            }
            plotLocalError.setColor(color);
            plotLocalError.setLineWidth(1);
            plotLocalError.addPoints(xVals, yVals, Plot.LINE);
        }
        imsPlots.addSlice(plotLocalError.getProcessor());

        // Plot particles position
        ImageProcessor ipVariables = null;
        for (int v=0; v<nVariables; v++) {

            Plot plotParticlesPosition = new Plot("Position of variable "+variableNames[v],
                    "Iteration number", "Value of variable "+variableNames[v]);

            for (int pId=0; pId<swarmSize; pId++) {

                Color color = new Color(generator.nextInt(255),generator.nextInt(255), generator.nextInt(255));

                xVals = new double[nIterations];
                yVals = new double[nIterations];

                for (int i=0; i<nIterations; i++) {
                    xVals[i] = i+1;
                    yVals[i] = this.position_particles_history[i][pId * nVariables + v];
                }

                plotParticlesPosition.setColor(color);
                plotParticlesPosition.setLineWidth(1);
                plotParticlesPosition.addPoints(xVals, yVals, Plot.LINE);
            }

            imsPlots.addSlice(plotParticlesPosition.getProcessor());
        }

        ip = new ColorProcessor(imsPlots.getWidth(), imsPlots.getHeight() * imsPlots.getSize());
        for (int s=1; s<=imsPlots.getSize(); s++) {
            ip.copyBits(imsPlots.getProcessor(s), 0, imsPlots.getHeight() * (s-1), Blitter.COPY);
        }
        new ImagePlus("Optimisation Evolution", ip).show();

        //plotOptimizationEvolution3D();
    }

    public ImagePlus renderOptimizationEvolution2D(int w, int h, int varId0, int varId1) {

        byte[][][] volume = new byte[nIterations][3][w*h];
        ImageStack ims = new ImageStack(w, h);
        byte[] c = new byte[3];

        for (int pId = 0; pId < swarmSize; pId++) {
            c[0] = (byte) (generator.nextDouble() * 155 + 100);
            c[1] = (byte) (generator.nextDouble() * 155 + 100);
            c[2] = (byte) (generator.nextDouble() * 155 + 100);

            for (int i = 0; i < nIterations; i++) {

                double p0 = (position_particles_history[i][pId * nVariables + varId0] - position_min[varId0]) / (position_max[varId0] - position_min[varId0]);
                double p1 = (position_particles_history[i][pId * nVariables + varId1] - position_min[varId1]) / (position_max[varId1] - position_min[varId1]);

                int _p0 = (int) round(p0 * (w - 1));
                int _p1 = (int) round(p1 * (h - 1));

                for (int ch=0; ch<3; ch++) {
                    volume[i][ch][w * _p1 + _p0] = c[ch];
                    if (_p0 > 0) volume[i][ch][w * _p1 + _p0 - 1] = c[ch];
                    if (_p0 < w - 1) volume[i][ch][w * _p1 + _p0 + 1] = c[ch];
                    if (_p1 > 0) volume[i][ch][w * (_p1 - 1) + _p0] = c[ch];
                    if (_p1 < h - 1) volume[i][ch][w * (_p1 + 1) + _p0] = c[ch];
                }
            }
        }

        for (int i = 0; i < nIterations; i++) {
            ColorProcessor ip = new ColorProcessor(w, h);
            ip.setRGB(volume[i][0], volume[i][1], volume[i][2]);
            ims.addSlice(ip);

        }

        ImagePlus imp = new ImagePlus("Particle Evolution", ims);
        IJ.run(imp, "8-bit Color", "number=256");

        return imp;

    }

    public ImagePlus renderOptimizationEvolution3D(int w, int h, int d, int varId0, int varId1, int varId2) {

        byte[][][][] volume = new byte[nIterations][d][3][w*h];
        ImageStack ims = new ImageStack(w, h);
        byte[] c = new byte[3];

        for (int pId = 0; pId < swarmSize; pId++) {
            c[0] = (byte) (generator.nextDouble() * 155 + 100);
            c[1] = (byte) (generator.nextDouble() * 155 + 100);
            c[2] = (byte) (generator.nextDouble() * 155 + 100);

            for (int i = 0; i < nIterations; i++) {

                double p0 = (position_particles_history[i][pId * nVariables + varId0] - position_min[varId0]) / (position_max[varId0] - position_min[varId0]);
                double p1 = (position_particles_history[i][pId * nVariables + varId1] - position_min[varId1]) / (position_max[varId1] - position_min[varId1]);
                double p2 = (position_particles_history[i][pId * nVariables + varId2] - position_min[varId2]) / (position_max[varId2] - position_min[varId2]);

                int _p0 = (int) round(p0 * (w - 1));
                int _p1 = (int) round(p1 * (h - 1));
                int _p2 = (int) round(p2 * (d - 1));

                for (int ch=0; ch<3; ch++) {
                    volume[i][_p2][ch][w * _p1 + _p0] = c[ch];
                    if (_p0 > 0) volume[i][_p2][ch][w * _p1 + _p0 - 1] = c[ch];
                    if (_p0 < w - 1) volume[i][_p2][ch][w * _p1 + _p0 + 1] = c[ch];
                    if (_p1 > 0) volume[i][_p2][ch][w * (_p1 - 1) + _p0] = c[ch];
                    if (_p1 < h - 1) volume[i][_p2][ch][w * (_p1 + 1) + _p0] = c[ch];
                    if (_p2 > 0) volume[i][_p2 - 1][ch][w * _p1 + _p0] = c[ch];
                    if (_p2 < d - 1) volume[i][_p2 + 1][ch][w * _p1 + _p0] = c[ch];
                }
            }
        }

        for (int i = 0; i < nIterations; i++) {
            for (int z=0; z<d; z++) {
                ColorProcessor ip = new ColorProcessor(w, h);
                ip.setRGB(volume[i][z][0], volume[i][z][1], volume[i][z][2]);
                ims.addSlice(ip);
            }
        }

        ImagePlus imp = new ImagePlus("Particle Evolution", ims);
        IJ.run(imp, "8-bit Color", "number=256");
        imp = toHyperStack(imp, 1, d, nIterations);

        return imp;
    }

    public ImagePlus plotOptimizationEvolution3D(int w, int h, int varId0, int varId1, int varId2) {

        Image3DUniverse universe = new Image3DUniverse();

        for (int pId = 0; pId < swarmSize; pId++) {
            Color3f color = new Color3f(generator.nextFloat(), generator.nextFloat(), generator.nextFloat());

            ArrayList<Point3f> pointList = new ArrayList<Point3f>();
            for (int i = 0; i < nIterations; i++) {
                float p0 = (float) ((position_particles_history[i][pId * nVariables + varId0] - position_min[varId0]) / (position_max[varId0] - position_min[varId0]));
                float p1 = (float) ((position_particles_history[i][pId * nVariables + varId1] - position_min[varId1]) / (position_max[varId1] - position_min[varId1]));
                float p2 = (float) ((position_particles_history[i][pId * nVariables + varId2] - position_min[varId2]) / (position_max[varId2] - position_min[varId2]));

                pointList.add(new Point3f(p0, p1, p2));
            }

            universe.addPointMesh(pointList, color, 3, "Points Id" + pId);
            Content lines = universe.addLineMesh(pointList, color, "Lines Id" + pId, true);
            lines.setTransparency(0.7f);
        }

        float p0c = (float) ((globalBestPosition[varId0] - position_min[varId0]) / (position_max[varId0] - position_min[varId0]));
        float p1c = (float) ((globalBestPosition[varId1] - position_min[varId1]) / (position_max[varId1] - position_min[varId1]));
        float p2c = (float) ((globalBestPosition[varId2] - position_min[varId2]) / (position_max[varId2] - position_min[varId2]));

        universe.centerAt(new Point3d(p0c, p1c, p2c));

        ImageStack ims = new ImageStack(w, h);
        for (int s=0; s<300; s++) {
            log.progress(s+1, 300);
            if (s<100) {
                universe.rotateY(0.05);
                universe.getViewPlatformTransformer().zoomTo(max(1d - (s/100d), 0.01));
            }
            else if (s<200) {
                universe.rotateZ(0.02);
                universe.getViewPlatformTransformer().zoomTo(0.001 / (s-99));
            }
            else {
                universe.rotateX(0.01);
                universe.getViewPlatformTransformer().zoomTo(0.0005 / (s-99));
            }

            ims.addSlice(universe.takeSnapshot(w, h).getProcessor());
        }
        //universe.show();

        return new ImagePlus("Particle Evolution", ims);
    }
}
