package nanoj.core.java.gui.tools.transform;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.NonBlockingGenericDialog;
import ij.process.FloatProcessor;
import nanoj.core.java.array.ArrayCasting;
import nanoj.core.java.image.transform.TranslateOrRotateImage;
import nanoj.core.java.tools.MapTools;
import nanoj.core.java.tools.Options;

/**
 * Created by paxcalpt on 07/02/15.
 */
public class Rotate_ extends nanoj.core.java.gui._BaseDialog_ {
    float angle;

    TranslateOrRotateImage TR = new TranslateOrRotateImage();
    int method;

    @Override
    public boolean beforeSetupDialog(String arg) {
        autoOpenImp = true;
        useSettingsObserver = true;
        return true;
    }

    public void setupDialog() {
        gd = new NonBlockingGenericDialog("Translate...");
        gd.addNumericField("Angle", getPrefs("angle", 0), 2);
        gd.addChoice("Method", ArrayCasting.mapValue2StringArray(Options.interpolationMethod), Options.interpolationMethod.get(getPrefs("method", Options.BICUBIC)));

        gd.addCheckbox("Show preview", false);
    }

    public boolean loadSettings() {
        // Grab data from dialog
        angle = (float) Math.toRadians(gd.getNextNumber());
        method = MapTools.getKeyByValue(Options.interpolationMethod, gd.getNextChoice());
        showPreview = gd.getNextBoolean();

        TR.setInterpolationMethod(method);

        // Save dialog values into prefs
        setPrefs("angle", angle);
        setPrefs("method", method);
        return true;
    }

    public void execute() throws InterruptedException {
        ImageStack ims = imp.getStack();

        ImagePlus impResults = new ImagePlus(imp.getTitle()+" - rotated", TR.rotate(ims, angle));
        impResults.show();
    }

    public void doPreview() {
        if (showPreview) {

            FloatProcessor fpOriginal = imp.getProcessor().convertToFloatProcessor();
            FloatProcessor fpRotated = TR.rotate(fpOriginal, angle);

            if (impPreview != null) impPreview.setProcessor(fpRotated);
            else impPreview = new ImagePlus("Rotated", fpRotated);

            impPreview.show();
        }
    }
}
