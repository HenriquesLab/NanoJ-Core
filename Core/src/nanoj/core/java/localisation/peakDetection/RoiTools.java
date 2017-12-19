package nanoj.core.java.localisation.peakDetection;

import ij.gui.PolygonRoi;
import ij.gui.Roi;

/**
 * Author: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 4/30/13
 * Time: 3:20 PM
 */
public class RoiTools {

    public static Roi[] vector4ParticlesRois(nanoj.core.java.localisation.particlesHandling.ParticlesHolder peaks){

        Roi [] particles = new Roi[peaks.npoints];
        Roi particle  = null;

        float[] xcoord = peaks.getDataArray("x");
        float[] ycoord = peaks.getDataArray("y");

        float[] xlSigma = peaks.getDataArray("xlSigma");
        float[] xrSigma = peaks.getDataArray("xrSigma");
        float[] ylSigma = peaks.getDataArray("ylSigma");
        float[] yrSigma = peaks.getDataArray("yrSigma");

        float [] xpoints = new float [9];
        float [] ypoints = new float [9];

        for (int n=0; n<peaks.npoints; n++){

            xpoints[0] = xcoord[n];
            ypoints[0] = ycoord[n];

            xpoints[1] = xcoord[n]-(xlSigma[n]*2.354f);
            ypoints[1] = ycoord[n];

            xpoints[2] = xcoord[n];
            ypoints[2] = ycoord[n];

            xpoints[3] = xcoord[n]+(xrSigma[n]*2.354f);
            ypoints[3] = ycoord[n];

            xpoints[4] = xcoord[n];
            ypoints[4] = ycoord[n];

            xpoints[5] = xcoord[n];
            ypoints[5] = ycoord[n]-(ylSigma[n]*2.354f);

            xpoints[6] = xcoord[n];
            ypoints[6] = ycoord[n];

            xpoints[7] = xcoord[n];
            ypoints[7] = ycoord[n]+(yrSigma[n]*2.354f);

            xpoints[8] = xcoord[n];
            ypoints[8] = ycoord[n];

            particle = new PolygonRoi(xpoints, ypoints, 8, Roi.POLYLINE);
            particles[n] = particle;
        }
        return particles;
    }

    public static Roi [] vector2ParticlesRois(nanoj.core.java.localisation.particlesHandling.ParticlesHolder peaks){

        Roi [] particles = new Roi[peaks.npoints];
        Roi particle  = null;

        float[] xcoord = peaks.getDataArray("x");
        float[] ycoord = peaks.getDataArray("y");

        float[] xSigma = peaks.getDataArray("xSigma");
        float[] ySigma = peaks.getDataArray("ySigma");

        float [] xpoints = new float [7];
        float [] ypoints = new float [7];

        for (int n=0; n<peaks.npoints; n++){

            xpoints[0] = xcoord[n];
            ypoints[0] = ycoord[n];

            xpoints[1] = xcoord[n]-(xSigma[n]*2.354f);
            ypoints[1] = ycoord[n];

            xpoints[2] = xcoord[n]+(xSigma[n]*2.354f);
            ypoints[2] = ycoord[n];

            xpoints[3] = xcoord[n];
            ypoints[3] = ycoord[n];

            xpoints[4] = xcoord[n];
            ypoints[4] = ycoord[n]-(ySigma[n]*2.354f);

            xpoints[5] = xcoord[n];
            ypoints[5] = ycoord[n]+(ySigma[n]*2.354f);

            xpoints[6] = xcoord[n];
            ypoints[6] = ycoord[n];

            particle = new PolygonRoi(xpoints, ypoints, 7, Roi.POLYLINE);
            particles[n] = particle;
        }
        return particles;
    }


}
