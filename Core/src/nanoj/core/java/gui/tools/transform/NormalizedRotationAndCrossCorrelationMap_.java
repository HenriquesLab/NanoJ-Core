package nanoj.core.java.gui.tools.transform;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.NonBlockingGenericDialog;
import ij.process.FloatProcessor;
import nanoj.core.java.image.drift.EstimateShiftAndTilt;
import nanoj.core.java.image.handeling.ImageConcatenator;
import nanoj.core.java.image.transform.NormalizedRotationAndCrossCorrelationMap;

import static java.lang.Math.*;

/**
 * Created by paxcalpt on 02/02/15.
 */
public class NormalizedRotationAndCrossCorrelationMap_ extends nanoj.core.java.gui._BaseDialog_ {

    NormalizedRotationAndCrossCorrelationMap RCCM = new NormalizedRotationAndCrossCorrelationMap();
    float angleStep;
    int radiusX, radiusY;
    boolean showTiltAndShift;

    @Override
    public boolean beforeSetupDialog(String arg) {
        autoOpenImp = true;
        useSettingsObserver = true;
        return true;
    }

    public void setupDialog() {
        gd = new NonBlockingGenericDialog("Calculate the Rotation and Cross-Correlation Map...");
        gd.addNumericField("Max shift in X", getPrefs("radiusX", 100), 0);
        gd.addNumericField("Max shift in Y", getPrefs("radiusY", 100), 0);
        gd.addNumericField("Angle step", getPrefs("angleStep", 1), 0);
        gd.addCheckbox("Show Tilt and Shift", false);
        gd.addCheckbox("Show preview", false);
    }

    public boolean loadSettings() {
        // Grab data from dialog
        radiusX = (int) gd.getNextNumber();
        radiusY = (int) gd.getNextNumber();
        angleStep = (float) max(gd.getNextNumber(), 0.01);
        showTiltAndShift = gd.getNextBoolean();
        showPreview = gd.getNextBoolean();

        // Save dialog values into prefs
        setPrefs("radiusX", radiusX);
        setPrefs("radiusY", radiusY);
        setPrefs("angleStep", angleStep);
        setPrefs("showTiltAndShift", showTiltAndShift);

        angleStep = (float) toRadians(angleStep);
        return true;
    }

    public void execute() throws InterruptedException {
        ImageStack ims = imp.getStack();
        ImageStack[] imsRCCMap = RCCM.calculate(
                imp.getProcessor().convertToFloatProcessor(),
                imp.getStack(), angleStep);

        ImageStack imsResults = imsRCCMap[0];
        for (int s=1; s<imsRCCMap.length; s++) ImageConcatenator.concatenate(imsResults, imsRCCMap[s]);

        ImagePlus impResults = new ImagePlus("Normalized rotation-correlation map", imsResults);
        impResults.show();
    }

    public void doPreview() {
        if (showPreview) {
            ImageStack ims = imp.getStack();

            FloatProcessor fpReference = ims.getProcessor(1).convertToFloatProcessor();
            FloatProcessor fpComparison = imp.getProcessor().convertToFloatProcessor();

            ImageStack imsRCCMap = RCCM.calculate(fpReference, fpComparison, angleStep);

            if (showTiltAndShift) {
                float[][] shiftAndTilt = EstimateShiftAndTilt.getShiftAndTiltFromRotationAndCorrelationPeak(new ImageStack[]{imsRCCMap}, angleStep);
                float shiftX = shiftAndTilt[1][0];
                float shiftY = shiftAndTilt[2][0];
                float theta = shiftAndTilt[3][0];
                log.msg("shift-X="+shiftX+" shift-Y="+shiftY+" tilt="+toDegrees(theta));
            }

            if (impPreview != null) impPreview.setStack(imsRCCMap);
            else impPreview = new ImagePlus("Normalized Cross-Correlation Map", imsRCCMap);

            impPreview.show();
        }
    }
}