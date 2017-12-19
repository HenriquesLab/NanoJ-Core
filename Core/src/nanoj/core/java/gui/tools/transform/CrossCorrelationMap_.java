package nanoj.core.java.gui.tools.transform;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.NonBlockingGenericDialog;
import ij.process.ImageProcessor;
import nanoj.core.java.gui._BaseDialog_;

import static nanoj.core.java.image.transform.CrossCorrelationMap.calculateCrossCorrelationMap;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 21/02/2016
 * Time: 10:34
 */
public class CrossCorrelationMap_  extends _BaseDialog_ {

    private boolean normalized;

    @Override
    public boolean beforeSetupDialog(String arg) {
        autoOpenImp = true;
        useSettingsObserver = true;
        return true;
    }

    public void setupDialog() {
        gd = new NonBlockingGenericDialog("Calculate the Cross-Correlation Map...");
        gd.addCheckbox("Normalized", getPrefs("normalized", false));
        gd.addCheckbox("Show preview", false);
    }

    public boolean loadSettings() {
        normalized = gd.getNextBoolean();
        showPreview = gd.getNextBoolean();

        setPrefs("normalized", normalized);
        savePrefs();
        return true;
    }

    public void execute() throws InterruptedException {
        ImageStack ims = imp.getStack();

        ImageProcessor ipComparison = imp.getProcessor().convertToFloatProcessor();

        ImagePlus impResults =
                new ImagePlus("Cross-Correlation Map", calculateCrossCorrelationMap(ipComparison, ims, normalized));
        impResults.show();
    }

    public void doPreview() {
        if (showPreview) {
            ImageStack ims = imp.getStack();

            ImageProcessor ipReference = ims.getProcessor(1);
            ImageProcessor ipComparison = imp.getProcessor().convertToFloatProcessor();
            ImageProcessor map = calculateCrossCorrelationMap(ipReference, ipComparison, normalized);

            if (impPreview != null) impPreview.setProcessor(map);
            else impPreview = new ImagePlus("Cross-Correlation Map", map);

            impPreview.show();
        }
    }
}
