package nanoj.core.java.gui.tools.transform;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.NonBlockingGenericDialog;
import ij.process.FloatProcessor;
import nanoj.core.java.image.transform.ResizeImage;

/**
 * Created by paxcalpt on 07/02/15.
 */
public class DecreaseImageSize_ extends nanoj.core.java.gui._BaseDialog_ {
    float spatialSizeReduction, temporalSizeReduction;

    ResizeImage RI = new ResizeImage();
    String[] methods = {"None", "BiLinear", "BiCubic"};
    String method;

    @Override
    public boolean beforeSetupDialog(String arg) {
        useSettingsObserver = true;
        autoOpenImp = true;
        return true;
    }

    public void setupDialog() {
        gd = new NonBlockingGenericDialog("Resize...");
        gd.addNumericField("Spatial size reduction",
                prefs.get("NJ.Transform.Resize.spatialSizeReduction", 5), 2);
        gd.addNumericField("Temporal size reduction",
                prefs.get("NJ.Transform.Resize.temporalSizeReduction", 5), 2);

        gd.addCheckbox("Show preview", false);
    }

    public boolean loadSettings() {
        // Grab data from dialog
        spatialSizeReduction = (float) gd.getNextNumber();
        temporalSizeReduction = (float) gd.getNextNumber();
        showPreview = gd.getNextBoolean();

        if (spatialSizeReduction<1) {
            IJ.error("Spatial size reduction needs to be >=1.");
            return false;
        }
        if (temporalSizeReduction<1) {
            IJ.error("Temporal size reduction needs to be >=1.");
            return false;
        }

        // Save dialog values into prefs
        prefs.set("NJ.Transform.Resize.spatialSizeReduction", spatialSizeReduction);
        prefs.set("NJ.Transform.Resize.temporalSizeReduction", temporalSizeReduction);
        return true;
    }

    public void execute() throws InterruptedException {
        ImageStack ims = imp.getStack();
        ImagePlus impResults = new ImagePlus(imp.getTitle()+" - resized",
                RI.decreaseSize(ims, spatialSizeReduction, temporalSizeReduction));
        impResults.show();
    }

    public void doPreview() {
        if (showPreview) {

            FloatProcessor fpOriginal = imp.getProcessor().convertToFloatProcessor();
            FloatProcessor fpResized = RI.decreaseSize(fpOriginal, spatialSizeReduction, temporalSizeReduction);

            if (impPreview != null) impPreview.setProcessor(fpResized);
            else impPreview = new ImagePlus("Resized Image", fpResized);

            impPreview.show();
        }
    }
}
