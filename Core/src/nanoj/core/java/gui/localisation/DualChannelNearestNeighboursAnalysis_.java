package nanoj.core.java.gui.localisation;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.HistogramWindow;
import ij.gui.NonBlockingGenericDialog;
import ij.gui.PointRoi;
import ij.measure.ResultsTable;
import ij.plugin.filter.MaximumFinder;
import ij.plugin.frame.RoiManager;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import nanoj.core.java.featureExtraction.ExtractRois;
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
public class DualChannelNearestNeighboursAnalysis_ extends _BaseDialog_ {

    private MaximumFinder MF = new MaximumFinder();
    private Kernel_NearestNeighbour NN = new Kernel_NearestNeighbour();
    private Kernel_VoronoiImage VI = new Kernel_VoronoiImage();
    private double tolerance1, tolerance2;
    private boolean show1, show2;
    //private int reference;
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
        gd.addNumericField("Tolerance Ch1", getPrefs("tolerance1", 10), 2);
        gd.addNumericField("Tolerance Ch2", getPrefs("tolerance2", 10), 2);
        gd.addCheckbox("Show peaks Ch1", true);
        gd.addCheckbox("Show peaks Ch2", true);

        gd.addNumericField("Number of bins", getPrefs("nBins", 100), 0);
        gd.addNumericField("Pixel size (nm)", getPrefs("pixelSize", 100), 0);
        //gd.addSlider("Reference channel", 1, 2, 1);
        gd.addCheckbox("Preview point selection", false);
    }

    @Override
    public boolean loadSettings() {
        tolerance1 = gd.getNextNumber();
        tolerance2 = gd.getNextNumber();
        show1 = gd.getNextBoolean();
        show2 = gd.getNextBoolean();
        //reference = (int) gd.getNextNumber();
        nBins = (int) gd.getNextNumber();
        pixelSize = (float) gd.getNextNumber();
        showPreview = gd.getNextBoolean();

        setPrefs("tolerance1", tolerance1);
        setPrefs("tolerance2", tolerance2);
        setPrefs("nBins", nBins);
        setPrefs("pixelSize", pixelSize);
        savePrefs();
        return true;
    }

    @Override
    public void execute() throws InterruptedException, IOException {

        ImageStack ims = imp.getImageStack();
        if (ims.getSize() != 2) log.error("Expecting image with two frames");

        //Detect peaks
        ImageProcessor ip0, ip1;
        ip0 = ims.getProcessor(1).convertToFloatProcessor();
        ip1 = ims.getProcessor(2).convertToFloatProcessor();

        Polygon plg0 = MF.getMaxima(ip0, tolerance1, true);
        PointRoi roi0 = new PointRoi(plg0);
        roi0.setStrokeColor(Color.YELLOW);

        Polygon plg1 = MF.getMaxima(ip1, tolerance2, true);
        PointRoi roi1 = new PointRoi(plg1);
        roi1.setStrokeColor(Color.MAGENTA);

        RoiManager rm = ExtractRois.getRoiManager();
        if (show1) rm.addRoi(roi0);
        if (show2) rm.addRoi(roi1);
        //rm.runCommand("Associate", "false");

        // Get peak info
        float[] xps0 = intToFloat(plg0.xpoints);
        float[] yps0 = intToFloat(plg0.ypoints);
        float[] xps1 = intToFloat(plg1.xpoints);
        float[] yps1 = intToFloat(plg1.ypoints);

        // Get nearest neighbour information with channel 1 as reference
        float[][] nearestCh1ToCh2 = NN.calculate(xps0, yps0, xps1, yps1);
        float[] nearestDistanceCh1ToCh2 = nearestCh1ToCh2[0];
        float[] nearestPositionCh1ToCh2 = nearestCh1ToCh2[1];
        int nPointsCh1 = nearestDistanceCh1ToCh2.length;

        // Get nearest neighbour information with channel 2 as reference
        float[][] nearestCh2ToCh1 = NN.calculate(xps1, yps1, xps0, yps0);
        float[] nearestDistanceCh2ToCh1 = nearestCh2ToCh1[0];
        float[] nearestPositionCh2ToCh1 = nearestCh2ToCh1[1];
        int nPointsCh2 = nearestDistanceCh2ToCh1.length;

        // Convert to nm and populate index list
        float[] nearestDistanceCh1ToCh2Nm = new float[nPointsCh1];
        float[] ch1Indices = new float[nPointsCh1];
        for(int i=0; i<nPointsCh1; i++) {
            nearestDistanceCh1ToCh2Nm[i] = nearestDistanceCh1ToCh2[i] * pixelSize;
            ch1Indices[i] = i;
        }
        float[] nearestDistanceCh2ToCh1Nm = new float[nPointsCh2];
        float[] ch2Indices = new float[nPointsCh2];
        for(int i=0; i<nPointsCh2; i++) {
            nearestDistanceCh2ToCh1Nm[i] = nearestDistanceCh2ToCh1[i] * pixelSize;
            ch2Indices[i] = i;
        }

        // Create table with Ch1 as reference
        log.status("populating table...");
        Map<String, double[]> dataCh1 = new LinkedHashMap<String, double[]>();
        dataCh1.put("particle index", floatToDouble(ch1Indices));
        dataCh1.put("x-position", floatToDouble(xps0));
        dataCh1.put("y-position", floatToDouble(yps0));
        dataCh1.put("closest neighbour distance", floatToDouble(nearestDistanceCh1ToCh2));
        dataCh1.put("closest neighbour distance (nm)", floatToDouble(nearestDistanceCh1ToCh2Nm));
        dataCh1.put("closest neighbour in Ch2", floatToDouble(nearestPositionCh1ToCh2));
        ResultsTable rtCh1 = dataMapToResultsTable(dataCh1);
        rtCh1.show("Nearest-Neighbour Table: Channel 1 as reference");


        // Create table with Ch2 as reference
        log.status("populating table...");
        Map<String, double[]> dataCh2 = new LinkedHashMap<String, double[]>();
        dataCh2.put("particle index", floatToDouble(ch2Indices));
        dataCh2.put("x-position", floatToDouble(xps1));
        dataCh2.put("y-position", floatToDouble(yps1));
        dataCh2.put("closest neighbour distance", floatToDouble(nearestDistanceCh2ToCh1));
        dataCh2.put("closest neighbour distance (nm)", floatToDouble(nearestDistanceCh2ToCh1Nm));
        dataCh2.put("closest neighbour in Ch1", floatToDouble(nearestPositionCh2ToCh1));
        ResultsTable rtCh2 = dataMapToResultsTable(dataCh2);
        rtCh2.show("Nearest-Neighbour Table: Channel 2 as reference");

        // Show histograms
        log.status("creating histogram");
        FloatProcessor fpHistCh1 = new FloatProcessor(nPointsCh1, 1, nearestDistanceCh1ToCh2Nm);
        ImagePlus impHistCh1 = new ImagePlus("List of neighbour distances", fpHistCh1);
        HistogramWindow histogramWindowCh1 = new HistogramWindow("Histogram of nearest-neighbour distances (Ch1 -> Ch2)", impHistCh1, nBins);

        FloatProcessor fpHistCh2 = new FloatProcessor(nPointsCh2, 1, nearestDistanceCh2ToCh1Nm);
        ImagePlus impHistCh2 = new ImagePlus("List of neighbour distances", fpHistCh2);
        HistogramWindow histogramWindowCh2 = new HistogramWindow("Histogram of nearest-neighbour distances (Ch2 -> Ch1)", impHistCh2, nBins);

        // Create Voronoi diagrams

        ImageProcessor ipVoronoiCh1ToCh2 = VI.calculateImage(ip0.getWidth(), ip0.getHeight(), xps0, yps0, nearestDistanceCh1ToCh2Nm);
        ImageProcessor ipVoronoiCh2ToCh1 = VI.calculateImage(ip0.getWidth(), ip0.getHeight(), xps1, yps1, nearestDistanceCh2ToCh1Nm);

        ImageStack imsVoronoi = new ImageStack(ip0.getWidth(), ip0.getHeight(), 2);
        imsVoronoi.setProcessor(ipVoronoiCh1ToCh2, 1);
        imsVoronoi.setSliceLabel("Channel 1 to Channel 2", 1);
        imsVoronoi.setProcessor(ipVoronoiCh2ToCh1, 2);
        imsVoronoi.setSliceLabel("Channel 2 to Channel 1", 2);

        ImagePlus impVoronoi = new ImagePlus("Nearest neighbours Voronoi (nm)", imsVoronoi);
        impVoronoi.show();
        IJ.run("Fire");

    }

    public void doPreview() {
        ImageStack ims = imp.getImageStack();
        if (ims.getSize() != 2) log.error("Expecting image with two frames");


        if (showPreview) {
            ImageProcessor ip0, ip1;
            ip0 = ims.getProcessor(1).convertToFloatProcessor();
            ip1 = ims.getProcessor(2).convertToFloatProcessor();

            Polygon plg0 = MF.getMaxima(ip0, tolerance1, true);
            PointRoi roi0 = new PointRoi(plg0);
            roi0.setStrokeColor(Color.YELLOW);

            Polygon plg1 = MF.getMaxima(ip1, tolerance2, true);
            PointRoi roi1 = new PointRoi(plg1);
            roi1.setStrokeColor(Color.MAGENTA);

            RoiManager rm = ExtractRois.getRoiManager();
            if (show1) rm.addRoi(roi0);
            if (show2) rm.addRoi(roi1);
            rm.runCommand("Associate", "false");
        }

    }
}
