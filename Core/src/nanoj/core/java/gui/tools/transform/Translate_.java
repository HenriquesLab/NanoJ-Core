package nanoj.core.java.gui.tools.transform;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.NonBlockingGenericDialog;
import ij.process.FloatProcessor;
import nanoj.core.java.array.ArrayCasting;
import nanoj.core.java.gui._BaseDialog_;
import nanoj.core.java.image.transform.TranslateOrRotateImage;
import nanoj.core.java.tools.Options;

import static nanoj.core.java.tools.MapTools.getKeyByValue;

/**
 * Created by paxcalpt on 07/02/15.
 */
public class Translate_ extends _BaseDialog_ {
    float shiftX, shiftY;

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
        gd.addNumericField("Shift in X", getPrefs("shiftX", 0), 2);
        gd.addNumericField("Shift in Y", getPrefs("shiftY", 0), 2);
        gd.addChoice("Method", ArrayCasting.mapValue2StringArray(Options.interpolationMethod), Options.interpolationMethod.get(getPrefs("method", Options.BICUBIC)));
        gd.addCheckbox("Show preview", false);
    }

    public boolean loadSettings() {
        // Grab data from dialog
        shiftX = (float) gd.getNextNumber();
        shiftY = (float) gd.getNextNumber();
        method = getKeyByValue(Options.interpolationMethod, gd.getNextChoice());
        showPreview = gd.getNextBoolean();

        TR.setInterpolationMethod(method);

        // Save dialog values into prefs
        setPrefs("shiftX", shiftX);
        setPrefs("shiftY", shiftY);
        setPrefs("method", method);
        return true;
    }

    public void execute() throws InterruptedException {
        ImageStack ims = imp.getStack();

        ImagePlus impResults = new ImagePlus(imp.getTitle()+" - translated", TR.translate(ims, shiftX, shiftY));
        impResults.show();
    }

    public void doPreview() {
        if (showPreview) {

            FloatProcessor fpOriginal = imp.getProcessor().convertToFloatProcessor();
            FloatProcessor fpTranslated = TR.translate(fpOriginal, shiftX, shiftY);

            if (impPreview != null) impPreview.setProcessor(fpTranslated);
            else impPreview = new ImagePlus("Translated", fpTranslated);

            impPreview.show();
        }
    }
}
