package nanoj.core.java.featureExtraction;

import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import nanoj.core.java.tools.Log;

import static java.lang.Math.*;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 15/02/15
 * Time: 15:55
 */
public class Peaks {

    public static boolean showProgress = false;

    public static float[] getMax(ImageProcessor ip, boolean[][] mask) {

        int width = ip.getWidth();
        int height = ip.getHeight();
        float v, vMax = -1;
        int xMax = 0, yMax = 0;

        for (int j = 1; j < height - 1; j++) {
            for (int i = 1; i < width - 1; i++) {
                if (mask!=null && !mask[i][j]) continue; // see if we are over an already found peak
                v = ip.getf(i, j);
                if (v > vMax) {
                    vMax = v;
                    xMax = i;
                    yMax = j;
                }
            }
        }
        return new float[] {vMax, xMax, yMax};
    }

    /**
     * QuickPALM style peak detection, positions returned are not sub-pixel
     * @param ip processor where to find peaks
     * @param nPeaks maximum number of peaks to find
     * @param radius next peak cannot be at a distance smaller than radiusX from previous peaks
     * @return [peakIdx][intensity, x, y]
     */
    public static float[][] getPeaks(ImageProcessor ip, int nPeaks, int radius) {
        return getPeaks(ip, nPeaks, radius, 0);
    }

    public static float[][] getPeaks(ImageProcessor ip, int nPeaks, int radius, float maxOverlap) {
        Log log = new Log();

        int width = ip.getWidth();
        int height = ip.getHeight();

        float [][] peaks = new float[nPeaks][3]; // [peakIdx][intensity, x, y]
        boolean[][] peakMask = new boolean[width][height];
        float v, vMax;
        int xMax = 0, yMax = 0, nDetectedPeaks = nPeaks;

        // mask out pixels that are not peaks
        for (int j=1; j<height-1; j++){
            for (int i=1; i<width-1; i++){
                if (i<radius || i>width-radius || j<radius || j>height-radius) continue;
                v = ip.getf(i, j);
                if (v < ip.getf(i-1, j-1)) continue; // not a peak
                else if (v < ip.getf(i-1, j  )) continue; // not a peak
                else if (v < ip.getf(i-1, j+1)) continue; // not a peak
                else if (v < ip.getf(i  , j-1)) continue; // not a peak
                else if (v < ip.getf(i  , j+1)) continue; // not a peak
                else if (v < ip.getf(i+1, j-1)) continue; // not a peak
                else if (v < ip.getf(i+1, j  )) continue; // not a peak
                else if (v < ip.getf(i+1, j+1)) continue; // not a peak
                peakMask[i][j] = true;
            }
        }
        // find maximum peaks in image
        for (int n=0; n<nPeaks; n++) {
            if (showProgress) log.progress(n+1, nPeaks);

            float[] maxs = getMax(ip, peakMask);
            vMax = maxs[0];
            xMax = (int) maxs[1];
            yMax = (int) maxs[2];

            // store peak
            if (vMax != -1) {
                peaks[n][0] = vMax;
                peaks[n][1] = xMax;
                peaks[n][2] = yMax;
                // prevent peak overlap
                float maximumDistance = radius*2*(1f-maxOverlap);
                float xStart = (int) max(1, xMax - maximumDistance);
                float yStart = (int) max(1, yMax - maximumDistance);
                float xEnd = min(width - 1, xMax + maximumDistance);
                float yEnd = min(height - 1, yMax + maximumDistance);

                for (int j = (int) yStart; j < yEnd; j++) {
                    for (int i = (int) xStart; i < xEnd; i++) {
                        if (sqrt(pow(i-xMax,2)+pow(j-yMax,2))<=maximumDistance)
                            peakMask[i][j] = false;
                    }
                }
            }
            else {
                nDetectedPeaks = n;
                break;
            }
        }
        if (nDetectedPeaks == nPeaks)
            return peaks;

        float [][] peaksFinal = new float[nDetectedPeaks][];
        System.arraycopy(peaks, 0, peaksFinal, 0, nDetectedPeaks);

        return peaksFinal;
    }

    public static Roi[] getROIs(float[][] peaks, int radius) {
        int nPeaks = peaks.length;
        Roi[] rois = new Roi[nPeaks];
        int[] xStart = new int[nPeaks];
        int[] yStart = new int[nPeaks];
        int[] rWidth = new int[nPeaks];
        int[] rHeight = new int[nPeaks];

        for (int n = 0; n < nPeaks; n++) {
            xStart[n] = (round(peaks[n][1]) - radius);
            yStart[n] = (round(peaks[n][2]) - radius);
            if (xStart[n] < 0) continue;
            if (yStart[n] < 0) continue;
            rWidth[n] = radius * 2 + 1;
            rHeight[n] = radius * 2 + 1;
            rois[n] = new Roi(xStart[n], yStart[n], rWidth[n], rHeight[n]);
        }
        return rois;
    }

    public static void populateRoiManagerWithPeaks(float[][] peaks, int radius, RoiManager rm) {
        Roi[] rois = getROIs(peaks, radius);
        for (Roi r: rois) {
            rm.addRoi(r);
        }
    }
}
