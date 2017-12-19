package nanoj.core.java.gui.tools.transform;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.NonBlockingGenericDialog;
import ij.measure.ResultsTable;
import ij.process.FloatProcessor;
import nanoj.core.java.gui._BaseDialog_;
import nanoj.kernels.Kernel_LineAndOrientationFilterTransform;

import static java.lang.Math.*;
import static nanoj.core.java.featureExtraction.ExtractSmallRegion.extractSmall2DRegion;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 21/02/2016
 * Time: 10:34
 */
public class FilamentAnalysis_ extends _BaseDialog_ {

    private int radius, nAngles;
    private static Kernel_LineAndOrientationFilterTransform kLAOFT = new Kernel_LineAndOrientationFilterTransform();
    private boolean showOFT, showLFT, showOT, doThreshold;

    @Override
    public boolean beforeSetupDialog(String arg) {
        autoOpenImp = true;
        useSettingsObserver = true;
        return true;
    }

    public void setupDialog() {
        gd = new NonBlockingGenericDialog("Calculate the Line and Orientation Filter Transform...");
        gd.addNumericField("Radius", getPrefs("radius", 5), 0);
        gd.addNumericField("Number of Angles", getPrefs("nAngles", 180), 0);

        gd.addCheckbox("Show Orientation Filter Transform (OFT)", getPrefs("showOFT", true));
        gd.addCheckbox("Show Line Filter Transform (LFT)", getPrefs("showLFT", false));

        gd.addMessage("-=-= Orientation Analysis =-=-\n", headerFont);
        gd.addCheckbox("Show Orientation", getPrefs("showOT", true));
        gd.addCheckbox("Threshold and Analyse Orientation", getPrefs("doThreshold", true));

        gd.addCheckbox("Show preview", false);
    }

    public boolean loadSettings() {

        radius = (int) gd.getNextNumber();
        nAngles = (int) gd.getNextNumber();
        showOFT = gd.getNextBoolean();
        showLFT = gd.getNextBoolean();
        showOT = gd.getNextBoolean();
        doThreshold = gd.getNextBoolean();

        showPreview = gd.getNextBoolean();

        setPrefs("radius", radius);
        setPrefs("nAngles", nAngles);
        setPrefs("showOFT", showOFT);
        setPrefs("showLFT", showLFT);
        setPrefs("showOT", showOT);
        setPrefs("doThreshold", doThreshold);
        savePrefs();
        return true;
    }

    public void execute() throws InterruptedException {
        ImageStack ims = imp.getStack();
        String title = imp.getTitle();

        ImageStack[] imsArray = kLAOFT.calculate(ims, nAngles, radius);

        ImagePlus impOFT = new ImagePlus(title+" - OFT", imsArray[0]);
        ImagePlus impLFT = new ImagePlus(title+" - LFT", imsArray[1]);
        ImagePlus impOrientation = new ImagePlus(title+" - Orientation", imsArray[2]);

        if (showOFT) impOFT.show();
        if (showLFT) impLFT.show();

        if (doThreshold) {
            ImagePlus impOFTSkeleton = impOFT.duplicate();
            IJ.run("Options...", "iterations=1 count=1 black");
            IJ.run(impOFTSkeleton, "Options...", "iterations=1 count=1 black");
            IJ.setAutoThreshold(impOFTSkeleton, "Otsu dark");
            IJ.run(impOFTSkeleton, "Convert to Mask", "");
            //IJ.run(impOFTSkeleton, "Skeletonize", "");

            ImageStack imsOFT = impOFTSkeleton.getImageStack();
            ImageStack imsOrientation = impOrientation.getImageStack();

            ResultsTable rt = new ResultsTable();

            for (int s=1; s<=imsOFT.getSize(); s++) {
                byte[] pixelsOFT = (byte[]) imsOFT.getPixels(s);
                float[] pixelsOrientation = (float[]) imsOrientation.getPixels(s);

                int count = 1;
                double x_component = 0;
                double y_component = 0;

                for (int p=0; p<pixelsOFT.length; p++) {
                    if (pixelsOFT[p]==0) pixelsOrientation[p] = Float.NaN;
                    else {
                        double angle_r = toRadians(pixelsOrientation[p]);
                        x_component += (cos(angle_r)-x_component)/count;
                        y_component += (sin(angle_r)-y_component)/count;
                        count++;
                    }
                }
                double avg_r = atan2(y_component, x_component);
                double avg_d = toDegrees(avg_r);

                double stddev_r = Math.sqrt(-Math.log(y_component*y_component+x_component*x_component));
                double stddev_d = toDegrees(stddev_r);

                rt.incrementCounter();
                rt.addValue("Average Angle (degrees)", avg_d);
                rt.addValue("Standard-Deviation Angle (degrees)", stddev_d);
                rt.addValue("Filaments Density (% of area occupied)", (100f*(count-1f)/pixelsOFT.length));

                //if (x_component > 0 && y_component < 0) avg_d += 360;
                //else if (x_component < 0) avg_d += 180;

            }
            rt.show("Orientation Analysis...");
        }

        if (showOT) {
            IJ.run(impOrientation, "Fire", "");
            impOrientation.show();
        }
    }

    public void doPreview() {
        FloatProcessor ip = extractSmall2DRegion(imp, 128, 128).convertToFloatProcessor();
        int w = ip.getWidth();
        int h = ip.getHeight();

        float[] pixelsOFT = kLAOFT.calculate((float[]) ip.getPixels(), w, h, nAngles, radius);
        FloatProcessor fpOut = new FloatProcessor(w, h, pixelsOFT);

        if (impPreview != null) impPreview.setProcessor(fpOut);
        else impPreview = new ImagePlus("Preview OFT", fpOut);
        impPreview.show();
    }
}
