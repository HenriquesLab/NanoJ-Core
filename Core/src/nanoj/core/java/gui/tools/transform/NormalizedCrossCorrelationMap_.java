package nanoj.core.java.gui.tools.transform;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.NonBlockingGenericDialog;
import ij.process.FloatProcessor;
import nanoj.core.java.gui._BaseDialog_;
import nanoj.core.java.image.transform.NormalizedCrossCorrelationMap;

/**
 * Created by paxcalpt on 02/02/15.
 */
public class NormalizedCrossCorrelationMap_ extends _BaseDialog_ {

    int radiusX, radiusY;
    NormalizedCrossCorrelationMap CCM = new NormalizedCrossCorrelationMap();

    @Override
    public boolean beforeSetupDialog(String arg) {
        autoOpenImp = true;
        useSettingsObserver = true;
        return true;
    }

    public void setupDialog() {
        gd = new NonBlockingGenericDialog("Calculate the Normalized Cross-Correlation Map...");
        gd.addNumericField("Max shift in X", getPrefs("radiusX", 100), 0);
        gd.addNumericField("Max shift in Y", getPrefs("radiusY", 100), 0);
        gd.addCheckbox("Show preview", false);
    }

    public boolean loadSettings() {
        // Grab data from dialog
        radiusX = (int) gd.getNextNumber();
        radiusY = (int) gd.getNextNumber();
        showPreview = gd.getNextBoolean();

        // Save dialog values into prefs
        setPrefs("radiusX", radiusX);
        setPrefs("radiusY", radiusY);
        return true;
    }

    public void execute() throws InterruptedException {
        ImageStack ims = imp.getStack();
        ImagePlus impResults = new ImagePlus("Normalized cross-correlation map",
                CCM.calculate(imp.getProcessor().convertToFloatProcessor(), imp.getStack(), radiusX, radiusY));
        impResults.show();
    }

    public void doPreview() {
        if (showPreview) {
            ImageStack ims = imp.getStack();

            FloatProcessor fpReference = ims.getProcessor(1).convertToFloatProcessor();
            FloatProcessor fpComparison = imp.getProcessor().convertToFloatProcessor();
            FloatProcessor map = CCM.calculate(fpReference, fpComparison, radiusX, radiusY);

            if (impPreview != null) impPreview.setProcessor(map);
            else impPreview = new ImagePlus("Normalized Cross-Correlation Map", map);

            impPreview.show();
        }
    }
}