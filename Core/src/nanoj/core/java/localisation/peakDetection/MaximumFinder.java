package nanoj.core.java.localisation.peakDetection;

import ij.ImageStack;
import ij.process.ImageProcessor;
import nanoj.core.java.array.ImageStackToFromArray;
import nanoj.core.java.image.analysis.CalculateNoiseAndBackground;
import nanoj.core.java.localisation.particlesHandling.ParticlesHolder;

import java.awt.*;

/**
 * Author: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 5/17/13
 * Time: 3:13 PM
 */
public class MaximumFinder {

    private ij.plugin.filter.MaximumFinder mf = new ij.plugin.filter.MaximumFinder();
    private CalculateNoiseAndBackground cnb = new CalculateNoiseAndBackground();

    public ParticlesHolder findPeaks(ImageStack ims, float snr){

        // TODO: eventually use Octane implementation instead of standard ImageJ's

        ImageProcessor ip;
        Polygon plg;
        ParticlesHolder peaks = new ParticlesHolder();

        float [] pixels = ImageStackToFromArray.ImageStackToFloatArray(ims);
        float [] bckgrdSigmaAndMean = cnb.calculateSigmaAndMeanInPeriphery(pixels, ims.getWidth(), ims.getHeight(), false);

        for (int t=0; t<ims.getSize();t++)
        {
            ip = ims.getProcessor(t+1);
            float bck_sigma = bckgrdSigmaAndMean[2*t];
            plg = mf.getMaxima(ip, snr*bck_sigma, true);
            peaks.appendPolygon(plg, t, 0.5f, 0.5f);
        }

        return peaks;
    }
}
