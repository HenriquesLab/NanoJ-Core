package nanoj.core.java.gui.tools.transform;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.NonBlockingGenericDialog;
import ij.process.FloatProcessor;
import nanoj.core.java.image.transform.ResizeImage;

/**
 * Created by paxcalpt on 07/02/15.
 */
public class IncreaseImageSize_ extends nanoj.core.java.gui._BaseDialog_ {
    float magnification;

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
        gd.addNumericField("Magnification",
                prefs.get("NJ.Transform.Resize.magnification", 5), 2);
        gd.addChoice("Method", methods, prefs.get("NJ.Transform.Resize.method", methods[2]));

        gd.addCheckbox("Show preview", false);
    }

    public boolean loadSettings() {
        // Grab data from dialog
        magnification = (float) gd.getNextNumber();
        showPreview = gd.getNextBoolean();
        method = gd.getNextChoice();

        if (method==methods[0]) RI.setInterpolationMethod(0);
        else if (method==methods[1]) RI.setInterpolationMethod(1);
        else if (method==methods[2]) RI.setInterpolationMethod(2);

        // Save dialog values into prefs
        prefs.set("NJ.Transform.Resize.magnification", magnification);
        prefs.set("NJ.Transform.Resize.method", method);
        return true;
    }

    public void execute() throws InterruptedException {
        ImageStack ims = imp.getStack();
        ImagePlus impResults = new ImagePlus(imp.getTitle()+" - resized", RI.increaseSize(ims, magnification));
        impResults.show();
    }

    public void doPreview() {
        if (showPreview) {

            FloatProcessor fpOriginal = imp.getProcessor().convertToFloatProcessor();
            FloatProcessor fpResized = RI.increaseSize(fpOriginal, magnification);

            if (impPreview != null) impPreview.setProcessor(fpResized);
            else impPreview = new ImagePlus("Resized Image", fpResized);

            impPreview.show();
        }
    }
}
