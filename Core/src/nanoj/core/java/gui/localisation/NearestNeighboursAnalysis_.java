package nanoj.core.java.gui.localisation;

import ij.ImagePlus;
import ij.gui.HistogramWindow;
import ij.gui.NonBlockingGenericDialog;
import ij.gui.PointRoi;
import ij.measure.ResultsTable;
import ij.plugin.filter.MaximumFinder;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import nanoj.core.java.gui._BaseDialog_;
import nanoj.kernels.Kernel_NearestNeighbour;
import nanoj.kernels.Kernel_VoronoiImage;

import java.awt.*;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static nanoj.core.java.array.ArrayCasting.floatToDouble;
import static nanoj.core.java.array.ArrayCasting.intToFloat;
import static nanoj.core.java.imagej.ResultsTableTools.dataMapToResultsTable;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 21/02/2016
 * Time: 22:55
 */
public class NearestNeighboursAnalysis_ extends _BaseDialog_ {

    private MaximumFinder MF = new MaximumFinder();
    private Kernel_NearestNeighbour NN = new Kernel_NearestNeighbour();
    private Kernel_VoronoiImage VI = new Kernel_VoronoiImage();
    private double tolerance;
    private int nBins;
    private float pixelSize;

    @Override
    public boolean beforeSetupDialog(String arg) {
        autoOpenImp = true;
        useSettingsObserver = true;
        return true;
    }

    @Override
    public void setupDialog() {
        gd = new NonBlockingGenericDialog("Nearest Neighbour Analysis");
        gd.addNumericField("Tolerance", getPrefs("tolerance", 10), 2);
        gd.addNumericField("Number of bins for histogram", getPrefs("nBins", 100), 0);
        gd.addNumericField("Pixel size (nm)", getPrefs("pixelSize", 100), 1);
        gd.addCheckbox("Preview point selection", false);
    }

    @Override
    public boolean loadSettings() {
        tolerance = gd.getNextNumber();
        nBins = (int) gd.getNextNumber();
        pixelSize = (float) gd.getNextNumber();
        showPreview = gd.getNextBoolean();

        setPrefs("tolerance", tolerance);
        setPrefs("nBins", nBins);
        setPrefs("pixelSize", pixelSize);
        savePrefs();
        return true;
    }

    @Override
    public void execute() throws InterruptedException, IOException {

        imp.deleteRoi();

        ImageProcessor ip = imp.getProcessor();

        Polygon plg = MF.getMaxima(ip, tolerance, true);
        PointRoi roi = new PointRoi(plg);

        imp.setRoi(roi);

        float[] xps = intToFloat(plg.xpoints);
        float[] yps = intToFloat(plg.ypoints);

        float[][] nearest = NN.calculate(xps, yps);
        float[] nearestDistance = nearest[0];

        int nPoints = nearestDistance.length;

        float[] nearestDistanceNm = new float[nPoints];
        for(int i=0; i<nPoints; i++){
            nearestDistanceNm[i] = nearestDistance[i]*pixelSize;
        }

        // Create  table
        log.status("populating table...");
        Map<String, double[]> data = new LinkedHashMap<String, double[]>();
        data.put("x-position", floatToDouble(xps));
        data.put("y-position", floatToDouble(yps));
        data.put("closest neighbour distance (pixels)", floatToDouble(nearestDistance));
        data.put("closest neighbour distance (nm)", floatToDouble(nearestDistanceNm));
        ResultsTable rt = dataMapToResultsTable(data);
        rt.show("Nearest-Neighbour Table");

        // Create histogram
        log.status("creating histogram");
        FloatProcessor fpHist = new FloatProcessor(nPoints, 1, nearestDistanceNm);
        ImagePlus impHist = new ImagePlus("List of neighbour distances", fpHist);
        HistogramWindow histogramWindow = new HistogramWindow("Histogram of nearest-neighbour distances", impHist, nBins);

        // Create Voronoi diagram

        ImageProcessor ipVoronoi = VI.calculateImage(ip.getWidth(), ip.getHeight(), xps, yps, nearestDistanceNm);

        ImagePlus impVoronoi = new ImagePlus("Nearest Neighbours Voronoi (nm)", ipVoronoi);

        impVoronoi.show();

    }

    public void doPreview() {
        if (showPreview) {
            ImageProcessor ip = imp.getProcessor();

            Polygon plg = MF.getMaxima(ip, tolerance, true);
            PointRoi roi = new PointRoi(plg);

            imp.setRoi(roi);
        }
    }
}
