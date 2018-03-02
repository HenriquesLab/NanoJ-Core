package nanoj.core2;

import ij.IJ;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.awt.*;

import static nanoj.core.java.image.analysis.CalculateImageStatistics.calculatePPMCC;
import static nanoj.core2.NanoJFHT.*;

public class NanoJCrossCorrelation {
    public static ImageProcessor calculateCrossCorrelationMap(ImageProcessor ip1, ImageProcessor ip2, boolean normalized) {
        int w1 = ip1.getWidth();
        int h1 = ip1.getHeight();
        int w2 = ip2.getWidth();
        int h2 = ip2.getHeight();
        if (w1!=w2 && h1!=h2) {
            IJ.error("Both comparison images don't have same size");
            return null;
        }
        if (!isEvenSquare(ip1)) {
            int size = getClosestEvenSquareSize(ip1);
            ip1.setRoi((w1-size)/2, (h1-size)/2, size, size);
            ip2.setRoi((w1-size)/2, (h1-size)/2, size, size);
            ip1 = ip1.crop();
            ip2 = ip2.crop();
        }

        return _calculateCrossCorrelationMap(ip1.convertToFloatProcessor(), ip2.convertToFloatProcessor(), normalized);
    }

    /**
     * Assumes ip1 and ip2 are already even square
     * @param ip1
     * @param ip2
     */
    private static FloatProcessor _calculateCrossCorrelationMap(FloatProcessor ip1, FloatProcessor ip2, boolean normalized) {
        FloatProcessor h1 = forwardFHT(ip1);
        FloatProcessor h2 = forwardFHT(ip2);
        FloatProcessor ipCCM = conjugateMultiply(h1, h2);
        ipCCM = inverseFHT(ipCCM, false);
        swapQuadrants(ipCCM);
        flip(ipCCM);

        if (normalized) ipCCM = _normalizeCrossCorrelationMap(ip1, ip2, ipCCM);
        ipCCM.setRoi(new Rectangle(0, 0, ipCCM.getWidth()-1, ipCCM.getHeight()-1));
        ipCCM = (FloatProcessor) ipCCM.crop();

        return ipCCM;
    }

    private static FloatProcessor _normalizeCrossCorrelationMap(FloatProcessor ip1, FloatProcessor ip2, FloatProcessor ipCCM) {
        float[] ccmPixels = (float[]) ipCCM.getPixels();

        int w = ipCCM.getWidth();
        int h = ipCCM.getHeight();
        float vMax = -Float.MAX_VALUE;
        float vMin = Float.MAX_VALUE;
        int pMax = 0;
        int pMin = 0;

        for (int n=0; n<ccmPixels.length; n++) {
            float v = ccmPixels[n];
            if (v > vMax) {
                vMax = v;
                pMax = n;
            }
            if (v < vMin) {
                vMin = v;
                pMin = n;
            }
        }

        int shiftXMax = (pMax % w) - w/2;
        int shiftYMax = (pMax / w) - h/2;
        int shiftXMin = (pMin % w) - w/2;
        int shiftYMin = (pMin / w) - h/2;

        float maxPPMCC = calculatePPMCC(ip1, ip2, shiftXMax, shiftYMax);
        float minPPMCC = calculatePPMCC(ip1, ip2, shiftXMin, shiftYMin);

        // calculate max and min Pearson product-moment correlation coefficient
        float deltaV = vMax - vMin;
        float deltaP = maxPPMCC - minPPMCC;
        for (int n=0; n<ccmPixels.length; n++) {
            float v = (ccmPixels[n] - vMin) / deltaV;
            v = (v * (maxPPMCC - minPPMCC)) + minPPMCC;
            ccmPixels[n] = v;
        }

        return new FloatProcessor(w, h, ccmPixels);
    }
}
