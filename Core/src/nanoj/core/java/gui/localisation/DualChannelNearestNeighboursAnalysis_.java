package nanoj.core.java.gui.localisation;

import ij.ImageStack;
import ij.gui.NonBlockingGenericDialog;
import ij.gui.PointRoi;
import ij.measure.ResultsTable;
import ij.plugin.filter.MaximumFinder;
import ij.plugin.frame.RoiManager;
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
    private int reference;

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
        gd.addSlider("Reference channel", 1, 2, 1);
        gd.addCheckbox("Show preview", false);
    }

    @Override
    public boolean loadSettings() {
        tolerance1 = gd.getNextNumber();
        tolerance2 = gd.getNextNumber();
        show1 = gd.getNextBoolean();
        show2 = gd.getNextBoolean();
        reference = (int) gd.getNextNumber();
        showPreview = gd.getNextBoolean();

        setPrefs("tolerance1", tolerance1);
        setPrefs("tolerance2", tolerance2);
        savePrefs();
        return true;
    }

    @Override
    public void execute() throws InterruptedException, IOException {

    }

    public void doPreview() {
        ImageStack ims = imp.getImageStack();
        if (ims.getSize() != 2) log.error("Expecting image with two frames");


        if (showPreview) {
            ImageProcessor ip0, ip1;
            if (reference == 1) {
                ip0 = ims.getProcessor(1).convertToFloatProcessor();
                ip1 = ims.getProcessor(2).convertToFloatProcessor();
            }
            else {
                ip0 = ims.getProcessor(2).convertToFloatProcessor();
                ip1 = ims.getProcessor(1).convertToFloatProcessor();
            }

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

            float[] xps0 = intToFloat(plg0.xpoints);
            float[] yps0 = intToFloat(plg0.ypoints);
            float[] xps1 = intToFloat(plg1.xpoints);
            float[] yps1 = intToFloat(plg1.ypoints);

            float[][] nearest = NN.calculate(xps0, yps0, xps1, yps1);
            float[] nearestDistance = nearest[0];
            float[] nearestPosition = nearest[1];



            // Create  table
            log.status("populating table...");
            Map<String, double[]> data = new LinkedHashMap<String, double[]>();
            data.put("x-position", floatToDouble(xps0));
            data.put("y-position", floatToDouble(yps0));
            data.put("closest neighbour distance", floatToDouble(nearestDistance));
            data.put("closest neighbour", floatToDouble(nearestPosition));
            ResultsTable rt = dataMapToResultsTable(data);
            rt.show("Nearest-Neighbour Table");

//            ImageProcessor ipVoronoi = VI.calculate(ip.getWidth(), ip.getHeight(), xps, yps, nearestDistance);
//
//            if (impPreview != null) impPreview.setProcessor(ipVoronoi);
//            else impPreview = new ImagePlus("Nearest Neighbours Voronoi", ipVoronoi);
//
//            impPreview.show();
        }
    }
}
