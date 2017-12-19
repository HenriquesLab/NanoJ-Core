package nanoj.core.java.localisation.particlesHandling;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import nanoj.core.java.image.rendering.SubPixelGaussianRendering;
import nanoj.core.java.tools.math.Randomizer;

import java.util.Random;

/**
 * Author: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 4/4/13
 * Time: 10:37 AM
 */
public class SimulateStackWithParticles {

    Random random = new Random();
    float[][] xpositions;
    float[][] ypositions;
    float[][] signals;
    float[][] sigmasX;
    float[][] sigmasY;

//    public ImagePlus generateStackWithApproachingParticles(int width, int height, int frames, int nParticles,
//                                                            int signal,
//                                                            double sigma, double initialDistance,
//                                                            double gaussNoiseSigma, double gaussNoiseMean,
//                                                            boolean addPoisson){
//
//        xpositions = new float[frames][nParticles+1];
//        ypositions = new float[frames][nParticles+1];
//        signals    = new float[frames][nParticles+1];
//        float[][] sigmas     = new float[frames][nParticles+1];
//
//        ImageStack ims = new ImageStack(width, height);
//        FloatProcessor ip;
//        double distance = 0;
//        double angle = 0;
//
//        float randx = random.nextFloat();
//        float randy = random.nextFloat();
//
//
//        int counter = 0;
//        for (int n=0; n<frames; n++){
//            distance = (1-((float) n/frames)) * initialDistance;
//
//            ip = new FloatProcessor(width, height);
//            ip.multiply(0);
//
//            xpositions[counter][0] = width/2+(randx-0.5f);
//            ypositions[counter][0] = height/2+(randy-0.5f);
//            sigmas    [counter][0] = (float) sigma;
//            signals   [counter][0] = (float) signal;
//
//            for (int p=0; p<nParticles;p++){
//                angle = random.nextDouble()*Math.toRadians(360);
//                xpositions[counter][p+1] = (float) (xpositions[counter][0]+Math.cos(angle)*distance);
//                ypositions[counter][p+1] = (float) (ypositions[counter][0]+Math.sin(angle)*distance);
//                sigmas    [counter][p+1] = (float) sigma;
//                signals   [counter][p+1] = (float) signal;
//                //System.out.println();
//            }
//
//            new AddParticles().addGaussians(ip, signals[counter], sigmas[counter], xpositions[counter], ypositions[counter]);
//
//            if (addPoisson)
//                Randomizer.addMixedGaussianPoissonNoise(ip, gaussNoiseSigma, gaussNoiseMean);
//            else if (gaussNoiseSigma > 0)
//                Randomizer.addGaussianNoise(ip, gaussNoiseSigma, gaussNoiseMean);
//            else
//                ip.sum(gaussNoiseMean);
//            ims.addSlice(ip);
//
//            counter ++;
//
//        }
//        return new ImagePlus("test", ims);
//
//    }


    public ImagePlus generateStackWithRandomParticles(int width, int height, int depth, int partPerSlice,
                                                      int signal, float sigma){
        return generateStackWithRandomParticles(width, height, depth, partPerSlice, 0, signal, 0, sigma, 0, sigma,
                0, 0, 0, false);
    }

    public ImagePlus generateStackWithRandomParticles(int i3, int i2, int i1, int i, int width, int height, int depth, float particle_sigma, int partPerSlice,
                                                      double gaussNoiseSigma, double gaussNoiseMean, boolean addPoisson){
        return generateStackWithRandomParticles(width, height, depth, partPerSlice,
                0, 1000, 0, 1.5f, 0, 1.5f, 0,
                gaussNoiseSigma, gaussNoiseMean, addPoisson);
    }

    public ImagePlus generateStackWithRandomParticles(int width, int height, int time,
                                                      int partPerSlice, double partPerSliceRandomness,
                                                      int signal, double signalRandomness,
                                                      double sigmaX, double sigmaXRandomness,
                                                      double sigmaY, double sigmaYRandomness,
                                                      double gaussNoiseSigma, double gaussNoiseMean,
                                                      boolean addPoisson){
        xpositions = new float[time][];
        ypositions = new float[time][];
        signals = new float[time][];
        sigmasX = new float[time][];
        sigmasY = new float[time][];

        ImageStack ims = new ImageStack(width, height);
        FloatProcessor ip;

        int npart;
        for (int t=0; t<time; t++)
        {
            ip = new FloatProcessor(width, height);
            ip.multiply(0);

            npart = (int) (partPerSlice * (1 - random.nextDouble()*partPerSliceRandomness));
            xpositions[t] = new float[npart];
            ypositions[t] = new float[npart];
            signals[t] = new float[npart];
            sigmasX[t] = new float[npart];
            sigmasY[t] = new float[npart];

            for (int p=0; p<npart; p++)
            {
                xpositions[t][p] = random.nextFloat()*(width-1);
                ypositions[t][p] = random.nextFloat()*(height-1);
                sigmasX[t][p] = (float) (sigmaX * (1 - sigmaXRandomness * random.nextFloat()));
                sigmasY[t][p] = (float) (sigmaY * (1 - sigmaYRandomness * random.nextFloat()));
                signals[t][p] = (float) (signal * (1 - signalRandomness * random.nextFloat()));
            }
            new SubPixelGaussianRendering().addGaussians(ip, signals[t], sigmasX[t], sigmasY[t], xpositions[t], ypositions[t]);
            if (addPoisson)
                Randomizer.addMixedGaussianPoissonNoise(ip, gaussNoiseSigma, gaussNoiseMean);
            else if (gaussNoiseSigma > 0)
                Randomizer.addGaussianNoise(ip, gaussNoiseSigma, gaussNoiseMean);
            else
                ip.add(gaussNoiseMean);
            ims.addSlice(ip);
        }
        return new ImagePlus("test", ims);
    }

    public ParticlesHolder getParticlesHolder(){

        int nParticles = 0;
        for (int n=0; n<this.xpositions.length; n++)
            nParticles+=this.xpositions[n].length;

        ParticlesHolder peaks = new ParticlesHolder(nParticles);

        String [] entrySequence = {"signal", "x", "y", "t", "sigmaX", "sigmaY"}; // sequence of elements to be given
        float [] entryValues = new float[entrySequence.length];

        for (int t=0; t<this.xpositions.length; t++)
        {
            for (int p=0; p<this.xpositions[t].length; p++) {
                entryValues[0] = this.signals[t][p]; // intensity
                entryValues[1] = this.xpositions[t][p]; // center x
                entryValues[2] = this.ypositions[t][p]; // center y
                entryValues[3] = t;
                entryValues[4] = this.sigmasX[t][p];
                entryValues[5] = this.sigmasY[t][p];

                peaks.addPoint(entrySequence, entryValues);
            }
        }
        return peaks;
    }

}
