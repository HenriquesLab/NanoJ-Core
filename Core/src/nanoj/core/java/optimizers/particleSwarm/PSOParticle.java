package nanoj.core.java.optimizers.particleSwarm;

import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Math.*;


/**
 * Created by paxcalpt on 06/06/2017.
 */
public class PSOParticle implements PSOConstants {

    private Random generator = new Random();
    public ReentrantLock lock;

    public final double[] position_highBoundary;
    public final double[] position_lowBoundary;
    private final double[] position_deltaBoundary;
    public final double[] velocity_highBoundary;
    public final double[] velocity_lowBoundary;
    private final double[] velocity_deltaBoundary;
    public final int id;
    public final int nVariables;
    public final boolean obeyBoundaries;

    public double[] position;
    public double[] velocity;
    public double[] bestPosition;
    public double error = Double.MAX_VALUE;
    public double bestError = Double.MAX_VALUE;

    private int iterationCounter = 0;
    private int maxIterations;

    public double[][] position_history;
    public double[][] velocity_history;
    //public double[][] bestPosition_history;
    public double[] error_history;
    public double[] improvement_history;
    //public double[] bestError_history;

    public PSOParticle(int id,
                       double[] position_lowBoundary, double[] position_highBoundary,
                       double[] velocity_lowBoundary, double[] velocity_highBoundary,
                       int maxIterations, boolean obeyBoundaries) {

        this.position_lowBoundary = position_lowBoundary;
        this.position_highBoundary = position_highBoundary;
        this.velocity_lowBoundary = velocity_lowBoundary;
        this.velocity_highBoundary = velocity_highBoundary;
        this.nVariables = position_lowBoundary.length;
        this.id = id;
        this.maxIterations = maxIterations;
        this.obeyBoundaries = obeyBoundaries;
        this.lock = new ReentrantLock();

        this.position = new double[nVariables];
        this.velocity = new double[nVariables];
        this.position_deltaBoundary = new double[nVariables];
        this.velocity_deltaBoundary = new double[nVariables];

        this.position_history = new double[nVariables][maxIterations];
        this.velocity_history = new double[nVariables][maxIterations];
        this.error_history = new double[maxIterations];
        this.improvement_history = new double[maxIterations];

        for (int n=0; n<nVariables; n++) {
            position_deltaBoundary[n] = position_highBoundary[n]-position_lowBoundary[n];
            velocity_deltaBoundary[n] = velocity_highBoundary[n]-velocity_lowBoundary[n];

            position[n] = position_lowBoundary[n] + generator.nextDouble() * position_deltaBoundary[n];
            velocity[n] = velocity_lowBoundary[n] + generator.nextDouble() * velocity_deltaBoundary[n];
            //System.out.println(""+velocity[n]);
        }
    }

    public void setError(double error) {
        if (error < this.bestError) {
            this.bestError = error;
            this.bestPosition = position.clone();
        }

        this.error = error;
        this.error_history[iterationCounter] = error;
    }

    public void update(double[] globalBestPosition, double globalWorstError, double[] position_min, double[] position_max) {
        if (iterationCounter >= maxIterations-1) return;

        // append history
        for (int n = 0; n < nVariables; n++) {
            position_history[n][iterationCounter] = position[n];
            velocity_history[n][iterationCounter] = velocity[n];
        }

        // calculate improvement
        double distance = 0;
        double distanceMaxHyperRectangle = 0;
        double improvement = 0;
        if (iterationCounter != 0) {
            double[] positionCurrent = getPositionNormalised(iterationCounter);
            double[] positionPrevious = getPositionNormalised(iterationCounter-1);

            for (int n = 0; n < nVariables; n++) {
                distance += pow(positionCurrent[n]-positionPrevious[n], 2);
                distanceMaxHyperRectangle += pow(position_max[n]-position_min[n], 2);
            }
            distance = sqrt(distance);
            distanceMaxHyperRectangle = sqrt(distanceMaxHyperRectangle);

            // Calculate the normalised improvement (fitness) factor
            // From Nobile et al, IEEE, 2015, Proactive Particles in Swarm Optimization: A self-tuning algorithm based on Fuzzy Logic
            double errorCurrent = error_history[iterationCounter];
            double errorPrevious = error_history[iterationCounter-1];
            improvement = ((errorPrevious-errorCurrent)/abs(globalWorstError))*(distance/distanceMaxHyperRectangle);
        }
        improvement_history[iterationCounter] = improvement;

        double Rcog = generator.nextDouble();
        double Rsoc = generator.nextDouble();

        // Inertia weight, bit weird.
        double w = W_UP - (W_UP-W_LO)*(((double) iterationCounter)/ maxIterations);
        //double w = 1;

        for (int n=0; n<nVariables; n++) {

            // note that we are completely unconstraining.... this actually seems to work better for our case
            double velocityProposal = w*velocity[n]+(Rcog*Ccog)*(bestPosition[n]-position[n])+(Rsoc*Csoc)*(globalBestPosition[n]-position[n]);

            if (obeyBoundaries) {
                velocityProposal = min(max(velocityProposal, velocity_lowBoundary[n]), velocity_highBoundary[n]);

                // From: Nobile et al, 2015, Proactive Particles in Swarm Optimization: A self-tuning algorithm based on Fuzzy Logic
                // "In this work, we consider the damping boundary conditions proposed by [12], whereby a random bounce on
                // the limit of the search space is simulated, letting the particle go back to the feasible region."
                // Here's the Huang paper: A hybrid boundary condition for robust particle swarm optimization
                double positionProposal = velocityProposal + position[n];
                if (positionProposal < position_lowBoundary[n] || positionProposal > position_highBoundary[n]) {
                    velocityProposal *= -generator.nextDouble();
                }
            }
            // dampen extreme velocity
            else if (velocityProposal < position_lowBoundary[n] || velocityProposal > position_highBoundary[n]) {
                velocityProposal *= generator.nextDouble();
            }

            velocity[n] = velocityProposal;
            position[n] += velocity[n];
        }

        this.iterationCounter++;
    }

    public int getNumberOfIterations() {
        return this.iterationCounter -1;
        //return iterationCounter;
    }

    public double[] getPositionNormalised(int iteration) {
        double [] position = new double[nVariables];
        for (int v=0; v<nVariables; v++) {
            position[v] = (this.position_history[v][iteration] - this.position_lowBoundary[v]) / position_deltaBoundary[v];
        }
        return position;
    }

    public double[] getPositionNormalised() {
        double [] position = new double[nVariables];
        for (int v=0; v<nVariables; v++) {
            position[v] = (this.position[v] - this.position_lowBoundary[v]) / position_deltaBoundary[v];
        }
        return position;
    }
}
