package nanoj.core.java.optimizers.particleSwarm;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Plot;
import ij.process.ByteProcessor;
import nanoj.core.java.threading.NanoJThreadExecutor;
import nanoj.core.java.tools.Log;

import java.awt.*;
import java.util.*;

import static java.lang.Math.*;
import static nanoj.core.java.array.ArrayInitialization.initializeAndValueFill;
import static nanoj.core.java.array.ArrayInitialization.initializeIntAndGrowthFill;

/**
 * Created by paxcalpt on 06/06/2017.
 */
public abstract class PSOOptimizer implements PSOConstants {

    private int swarmSize;
    private int maxIterations;
    private double tolerance = 0;
    public int nVariables;
    private Random generator = new Random();

    private Vector<PSOParticle> swarm = new Vector<PSOParticle>();
    private double globalBestError = Double.MAX_VALUE;
    private double globalWorstError = -Double.MAX_VALUE;
    public double[] globalBestPosition;

    private int iteration = 0;

    private LinkedHashMap<Double, Double> globalBestError_history = new LinkedHashMap<Double, Double>();
    private LinkedHashMap<Double, double[]> globalBestPosition_history = new LinkedHashMap<Double, double[]>();

    private Log log = new Log();

    private double[] position_lowBoundary;
    private double[] position_highBoundary;
    private double[] position_max; // note that in the unconstrained version the max may be higher than boundary
    private double[] position_min; // note that in the unconstrained version the min may be lower than boundary
    private double[] velocity_lowBoundary;
    private double[] velocity_highBoundary;

    private String statusString = "Time to finish";
    private long optimiserTime;

    public void setup(int swarmSize, int maxIterations, boolean obeyBoundaries, double[] position_lowBoundary, double[] position_highBoundary) {
        nVariables = position_lowBoundary.length;
        double[] velocity_lowBoundary = new double[nVariables];
        double[] velocity_highBoundary = new double[nVariables];

        for (int n=0; n<nVariables; n++) {
            double vRange = 0.2*abs(position_highBoundary[n]-position_lowBoundary[n]); // suggestion from Nobile et al, 2015
            velocity_lowBoundary[n] = -vRange;
            velocity_highBoundary[n] = +vRange;
        }

        this.setup(swarmSize, maxIterations, obeyBoundaries,
                   position_lowBoundary, position_highBoundary,
                   velocity_lowBoundary, velocity_highBoundary);
    }

    public void setup(int swarmSize, int maxIterations, boolean obeyBoundaries,
                      double[] position_lowBoundary, double[] position_highBoundary,
                      double[] velocity_lowBoundary, double[] velocity_highBoundary) {
        this.position_lowBoundary = position_lowBoundary;
        this.position_highBoundary = position_highBoundary;
        this.velocity_lowBoundary = velocity_lowBoundary;
        this.velocity_highBoundary = velocity_highBoundary;

        assert (position_lowBoundary.length == position_highBoundary.length);
        assert (velocity_lowBoundary.length == velocity_highBoundary.length);
        assert (position_lowBoundary.length == velocity_lowBoundary.length);

        this.nVariables = position_lowBoundary.length;
        this.swarmSize = swarmSize;
        this.maxIterations = maxIterations;
        this.globalBestPosition = new double[this.nVariables];
        this.position_max = initializeAndValueFill(nVariables, -Double.MAX_VALUE);
        this.position_min = initializeAndValueFill(nVariables, Double.MAX_VALUE);

        NanoJThreadExecutor NTE = new NanoJThreadExecutor(false);
        NTE.threadBufferSize = max(NTE.nCPUs-1, 1);
        NTE.showProgress = false;

        for(int i=0; i<swarmSize; i++){
            PSOParticle p = new PSOParticle(i,
                    position_lowBoundary, position_highBoundary,
                    velocity_lowBoundary, velocity_highBoundary, maxIterations, obeyBoundaries);
            swarm.add(p);

            Thread t = new ThreadedProcessParticle(p);
            NTE.execute(t);
            //t.run();
        }
        NTE.finish();
    }

    // This is the function you want to overwrite
    public abstract double calculateError(double[] params);

    public void setBestGuess(double[] params) {
        assert (nVariables == params.length);
        double error = calculateError(params);
        checkIfBestGlobalErrorAndUpdate(error, params);
    }

    synchronized public boolean checkIfBestGlobalErrorAndUpdate(double error, double[] position) {
        if (error > globalWorstError) globalWorstError = error; // keep track of worst error

        if (error < globalBestError) {
            globalBestError = error;
            globalBestError_history.put((double) iteration, error);

            globalBestPosition = position.clone();
            globalBestPosition_history.put((double) iteration, globalBestPosition);

            return true;
        }
        return false;
    }

    public void execute() {
        NanoJThreadExecutor NTE = new NanoJThreadExecutor(false);
        NTE.threadBufferSize = max(NTE.nCPUs-1, 1);
        NTE.showProgress = false;

        long timeStart = System.nanoTime();
        double remainingTime;

        for (iteration=0; iteration<maxIterations; iteration++) {

            for (PSOParticle p: swarm) {
                // process the particle in a thread
                Thread t = new ThreadedProcessParticle(p);
                NTE.execute(t);
            }

            double iterationTime=((System.nanoTime()-timeStart)/(iteration+1))/1e9;
            remainingTime = iterationTime*(maxIterations-iteration+1);
            int _h = (int) (remainingTime / 3600);
            int _m = (int) (((remainingTime % 86400) % 3600) / 60);
            int _s = (int) (((remainingTime % 86400) % 3600) % 60);
            log.status(statusString +" = "+String.format("%02d:%02d:%02d", _h, _m, _s));


            if(iteration%50==0){
                log.msg("\t Iteration "+iteration+", minimum error is "+globalBestError);
            }
        }
        NTE.finish();
        long timeFinish = System.nanoTime();
        optimiserTime = timeFinish-timeStart;

    }

    public void plotOptimizationEvolution(String[] variableNames, double percentageOfParticles) {
        assert (variableNames.length == nVariables);
        assert (percentageOfParticles > 0 && percentageOfParticles <= 100);

        Plot plotGlobalError = new Plot("Global error evolution", "Iteration number", "Error");
        Plot plotParticleError = new Plot("Particle error evolution", "Iteration number", "Error");
        Plot plotParticleImprovement = new Plot("Particle improvement evolution", "Iteration number", "Improvement");

        Plot[] plotsVariablesPosition = new Plot[nVariables];
        Plot[] plotsVariablesVelocity = new Plot[nVariables];

        for(int n=0; n<nVariables; n++) {
            plotsVariablesPosition[n] = new Plot("Position of variable "+variableNames[n],
                    "Value of variable "+variableNames[n], "Iteration number");
            plotsVariablesVelocity[n] = new Plot("Velocity of variable "+variableNames[n],
                    "Velocity of variable "+variableNames[n], "Iteration number");
        }

        double[] xVals, yVals;

        for (PSOParticle p: swarm) {
            if (generator.nextDouble()*100 > percentageOfParticles) continue; // randomly skip some particles

            int nIterations = p.getNumberOfIterations();
            yVals = initializeIntAndGrowthFill(nIterations, 1d, 1d);
            xVals = new double[nIterations];
            Color color = new Color(generator.nextInt(255),generator.nextInt(255), generator.nextInt(255));

            // Plot the variable values
            for(int n=0; n<nVariables; n++) {
                System.arraycopy(p.position_history[n], 0, xVals, 0, nIterations);
                plotsVariablesPosition[n].setColor(color);
                plotsVariablesPosition[n].setLineWidth(1);
                plotsVariablesPosition[n].addPoints(xVals, yVals, Plot.LINE);

                System.arraycopy(p.velocity_history[n], 0, xVals, 0, nIterations);
                plotsVariablesVelocity[n].setColor(color);
                plotsVariablesVelocity[n].setLineWidth(1);
                plotsVariablesVelocity[n].addPoints(xVals, yVals, Plot.LINE);
            }

            // Plot the error values
            xVals = yVals.clone();
            System.arraycopy(p.error_history, 0, yVals, 0, nIterations);
            plotParticleError.setColor(color);
            plotParticleError.setLineWidth(1);
            plotParticleError.addPoints(xVals, yVals, Plot.LINE);

            // Plot the improvement values
            System.arraycopy(p.improvement_history, 0, yVals, 0, nIterations);
            plotParticleImprovement.setColor(color);
            plotParticleImprovement.setLineWidth(1);
            plotParticleImprovement.addPoints(xVals, yVals, Plot.LINE);
        }

        // Plot the global values
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

        plotGlobalError.show();
        plotParticleError.show();
        plotParticleImprovement.show();
        for(int n=0; n<plotsVariablesPosition.length; n++) {
            plotsVariablesPosition[n].show();
//            plotsVariablesVelocity[n].show();
        }
    }

    public ImagePlus renderParticleEvolution(int width, int height, int varX, int varY, String varXName, String varYName) {
        ImageStack ims = new ImageStack(width, height, maxIterations);

        for (int i=1; i<=maxIterations; i++) {
            ims.setProcessor(new ByteProcessor(width, height), i);
        }

        for (PSOParticle p: swarm) {
            byte colorId = (byte) round(generator.nextDouble() * 254 + 1);

            for (int n = 0; n < p.getNumberOfIterations(); n++) {
                double xDouble = p.position_history[varX][n];
                xDouble = (xDouble - position_lowBoundary[varX]) / (position_highBoundary[varX] - position_lowBoundary[varX]);
                xDouble *= width;

                double yDouble = p.position_history[varY][n];
                yDouble = (yDouble - position_lowBoundary[varY]) / (position_highBoundary[varY] - position_lowBoundary[varY]);
                yDouble *= height;

                int x = min(max((int) round(xDouble), 0), width-1);
                int y = min(max((int) round(yDouble), 0), height-1);

                ((byte[]) ims.getPixels(n + 1))[y * width + x] = colorId;
                if (x>0 && y>0)               ((byte[]) ims.getPixels(n + 1))[(y-1) * width + (x-1)] = colorId;
                if (x<width-1 && y>0)         ((byte[]) ims.getPixels(n + 1))[(y-1) * width + (x+1)] = colorId;
                if (x>0 && y<height-1)        ((byte[]) ims.getPixels(n + 1))[(y+1) * width + (x-1)] = colorId;
                if (x<width-1 && y<height-1)  ((byte[]) ims.getPixels(n + 1))[(y+1) * width + (x+1)] = colorId;
            }
        }

        ImagePlus imp = new ImagePlus("Particle Evolution - "+varXName+" vs "+varYName, ims);
        IJ.run(imp, "Rainbow RGB", "");
        return imp;
    }

    public ImagePlus renderParticleEvolution(int width) {
        ImageStack ims = new ImageStack(width, nVariables, maxIterations);

        for (int i=1; i<=maxIterations; i++) {
            ims.setProcessor(new ByteProcessor(width, nVariables), i);
        }

        for (PSOParticle p: swarm) {
            byte colorId = (byte) round(generator.nextDouble() * 254 + 1);

            for (int v = 0; v < nVariables; v++) {
                for (int n = 0; n < p.getNumberOfIterations(); n++) {

                    double position = p.position_history[v][n];
                    position = (position - position_min[v]) / (position_max[v] - position_min[v]);
                    position *= (width - 1);

                    ((byte[]) ims.getPixels(n + 1))[(int) (v * width + min(max(round(position), 0), width - 1))] = colorId;
                }
            }
        }
        ImagePlus imp = new ImagePlus("Particle Evolution", ims);
        IJ.run(imp, "Rainbow RGB", "");
        return imp;
    }

    private class ThreadedProcessParticle extends Thread {
        private PSOParticle p;

        public ThreadedProcessParticle(PSOParticle p) {
            this.p = p;
        }

        @Override
        public void run() {

            p.lock.lock();
            double[] position = p.position.clone();

            for (int n=0; n<nVariables; n++) {
                position_max[n] = max(position_max[n], position[n]);
                position_min[n] = min(position_min[n], position[n]);
            }

            double error = calculateError(position);

            p.setError(error);
            checkIfBestGlobalErrorAndUpdate(error, position);
            p.update(globalBestPosition, globalWorstError, position_min, position_max);

            p.lock.unlock();
        }
    }

    public long getOptimiserTime(){return optimiserTime;}

    public void setStatusString(String string){statusString = string;}
}

